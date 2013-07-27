package org.smlt.tools.wgrep.config

import java.util.Map;

import groovy.util.logging.Slf4j;
import groovy.xml.dom.DOMCategory;
import org.smlt.tools.wgrep.filters.enums.Qualifier;

/**
 * A helper class to provide automatic filter, log entry pattern identification. Currently supported identification by filename, or by specifying tag explicitly via option in config.xml
 *
 * @author Alexander Semelit 
 */

@Slf4j
class PredictingParamsHolderFactory extends BasicParamsHolderFactory {

	List ATMTN_SEQ = []
	List FIRE_ONCE_METHODS = []
	String currentConfigPtrn
	String currentConfigId
	boolean isAmmended = false
	String lv_tag

	/**
	 * Constructor. Accepts WgrepConfig to initialize needed params. <br>
	 * Parses automation method sequence from the config.xml depending on level tag as specified in <automation> section of the config.xml.
	 */

	PredictingParamsHolderFactory(ConfigHolder config) {
		super(config)
	}

	@Override
	protected void optionNotFoundHook(Map<Param, ?> params, String opt) {
		if (isAutomationEnabled() && applySequenceByTag(params, opt)) {
			//trying to apply sequence first
			log.info("Applied sequence for: {}", opt)
			if (!ParamsHolder.checkParamIsEmpty(params, Param.FILTER_PATTERN)) {
				varParsers.remove(filterParser)
			}
		}
		if (checkIfConfigExsits(opt)) { //checking if  there exists a config with such id and applying it if so
			params[Param.PREDEF_TAG] = opt
			setPredefinedConfig(params, opt)
		}
		else if (ParamsHolder.checkParamIsEmpty(params, Param.FILTER_PATTERN) && checkIfFilterExsits(opt)) { //checking filter wasn't supplied explicitly and there exists a filter with such id and applying it if so
			params[Param.PREDEF_TAG] = opt
			setPredefinedFilter(params, opt)
		}
		else if (checkIfExecuteThreadExsits(opt)) { //checking if there exists a thread preserving patterns with such id and applying it if so
			setThreadPreserving(params, opt)
		}
		else if (checkIfPostProcessExsits(opt)) { //checking if there exists a post_processing config with such id and applying it if so
			setPostProcessing(params, opt)
		}
		else {
			super.optionNotFoundHook(params, opt)
		}
	}

	/**
	 * Method for refreshing config params by a filename. Requires {@link PatternAutomationHelper} paHelper to be initialized. <br>
	 * Calls {@link PatternAutomationHelper.applySequenceByFileName}
	 * @param fileName String representing name of a file to be checked. Could be an absolute path as well.
	 */

	@Override
	boolean refreshParamsByFile(ParamsHolder paramsHolder, String fileName)
	{
		return isAutomationEnabled() ? applySequenceByFileName(paramsHolder.getModifiableParams(), fileName) : false
	}

	@Override
	protected void setUserLEPattern(Map<Param, ?> params, def val)
	{
		super.setUserLEPattern(params, val)
		disableSequence()
	}

	/**
	 * Enables <code>LOG_ENTRY_PATTERN</code>, <code>FILTER_PATTERN</code>, <code>PRESERVE_THREAD</code> auto-identification based on supplied <code>level</code>. Initializes {@link PatternAutomationHelper}.
	 * @param field Field to be set
	 * @param val <code>String</code> value to be set. Valid config preset tag from <code>automation</code> section is expected here.
	 */

	protected void setAutomation(Map<Param, ?> params, def val)
	{
		enableSequence(val) //refreshing PatternAutomation instance
	}

	/**
	 * Sets <code>FILTER_PATTERN</code> according to on supplied <code>tag</code> from <code>filters</code> section of config.xml. If pattern automation.
	 * @param field Field to be set
	 * @param val <code>String</code> value to be set. Valid config preset tag from <code>automation</code> section is expected here.
	 */
	protected void setPredefinedConfig(Map<Param, ?> params, def val)
	{
		isAutomationEnabled() ? applySequenceByTag(params, val) : log.warn("Attempt to predefine config with disabled automation")
		//disableSequence() //not disabling, so it can be reconfigured if supplied files have heterogenous log entry patterns
	}


	/**
	 * Sets <code>FILTER_PATTERN</code> according to on supplied <code>tag</code> from <code>filters</code> section of config.xml. Requires pattern automation to operate. <br>
	 * Calls {@link PatternAutomation.parseFilterConfig}
	 * @param field Field to be set
	 * @param val <code>String</code> value to be set. Valid config preset tag from <code>automation</code> section is expected here.
	 */
	protected void setPredefinedFilter(Map<Param, ?> params, def val)
	{
		varParsers.remove(filterParser)
		parseFilterConfig(params, val)
	}


	/**
	 * Sets <code>FILTER_PATTERN</code> according to on supplied <code>tag</code> from <code>filters</code> section of config.xml. Requires pattern automation to operate. <br>
	 * Calls {@link PatternAutomation.parseFilterConfig}
	 * @param field Field to be set
	 * @param val <code>String</code> value to be set. Valid config preset tag from <code>automation</code> section is expected here.
	 */
	protected void setThreadPreserving(Map<Param, ?> params, def val)
	{
		parseExecuteThreadConfig(params, val)
	}


	/**
	 * Enables post processing.
	 * @param field Field to be set
	 * @param val <code>String</code> value to be set. Valid config preset tag from <code>pp_splitters</code> section is expected here.
	 */

	protected void setPostProcessing(Map<Param, ?> params, def val)
	{
		parsePostFilterConfig(params, val)
	}


	private void enableSequence(String level_tag) {
		lv_tag = level_tag
		log.info("Enabling level /{}/ sequence", lv_tag)
		config.withRoot { root ->
			def levels = root.automation.level.findAll { checkInTags(it.'@tags', lv_tag)}.sort {it.'@order'}
			ATMTN_SEQ.addAll(levels.collect { it.'@handler' })
			FIRE_ONCE_METHODS.addAll(levels.findAll { it.'@fireonce' == "true" }.collect{ it.'@handler'})
		}
	}

	void disableSequence() {
		lv_tag = null
		ATMTN_SEQ = []
	}

	boolean isAutomationEnabled() {
		return  lv_tag != null
	}

	void applyAutomationSequence(Map<Param, ?> params, String tag) {
		log.trace("Current sequence: {}", ATMTN_SEQ)
		ATMTN_SEQ.each { handler ->
			applyMethod(params, tag, handler)
		}

		log.trace("Fired methods: {}", FIRE_ONCE_METHODS)
		ATMTN_SEQ.removeAll(FIRE_ONCE_METHODS)
		log.trace("Config {} ammended", isAmmended ? "was": "wasn't")
	}


	/**
	 * Applies automation sequence to the filename and tries to find <config> section which has matching pattern. <br>
	 * 
	 * 
	 * @param filename String representing file name
	 * @return true if config params were changed.
	 */

	boolean applySequenceByFileName(Map<Param, ?> params, String filename)
	{
		isAmmended = false
		if (currentConfigPtrn != null)
		{
			if (filename =~ currentConfigPtrn) return isAmmended
			else currentConfigPtrn = null //since it's a different file
		}

		currentConfigId = findConfigIdByData(filename)
		applySequenceByTag(params, currentConfigId)

		return isAmmended
	}

	/**
	 * Applies automation sequence to the tag and tries to find <config> with id which matches the tag. <br>
	 * 
	 * @param tag String representing config tag as in config.xml
	 * @return true if config params were changed.
	 */

	boolean applySequenceByTag(Map<Param, ?> params, String tag)
	{
		isAmmended = false
		applyAutomationSequence(params, tag)
		return isAmmended
	}

	/**
	 * Applies method of PatternAutomationHelper to a via reflection. <br>
	 * 
	 * @param tag String representing tag to apply method for
	 * @param method String which should be name of a declared method in PatterAutomationHelper accepting tag
	 */

	void applyMethod(Map<Param, ?> params, String tag, String method)
	{
		log.trace("Applying method={} for tag={}", method, tag)
		if (method == null)
		{
			throw new IllegalArgumentException("Method shouldn't be null")
		}
		this."$method"(params, tag)
	}

	/**
	 * Parses <config> section with id equal to supplied tag. Depending on which elements are supplied it fills: <br>
	 * LOG_ENTRY_PATTERN <br>
	 * LOG_DATE_PATTERN <br>
	 * LOG_DATE_FORMAT <br>
	 * LOG_FILE_THRESHOLD <br>
	 * If custom config was found and params were filled, toggles isAmmended flag.
	 * 
	 * @param tag <config> section's id
	 */

	void parseCustomConfig(Map<Param, ?> params, String tag)
	{
		config.withRoot { root ->
			def customCfg = root.custom.config.find { it.'@id' ==~ tag }
			if (customCfg != null)
			{
				log.trace("Parsing entry config for {}", tag)

				def starter = customCfg.starter[0]
				if (starter != null) starter = starter.text()
				
				def date = customCfg.date[0]
				if (date != null) date = date.text()
								
				if (starter != null || date != null)
				{
					params[Param.LOG_ENTRY_PATTERN] = ((starter != null) ? starter : "") + ((date != null) ? date : "" )
				}
				else
				{
					log.warn("Either <starter> or <date> should be filled for config: {}", tag)
				}
				params[Param.LOG_DATE_PATTERN] = customCfg.date.text()
				params[Param.LOG_DATE_FORMAT] = customCfg.date_format.text()
				def thrshld = customCfg.log_threshold.text()
				if (thrshld != null && thrshld != "") params[Param.LOG_FILE_THRESHOLD] = Integer.valueOf(thrshld)
				isAmmended = true
			}
			else
			{
				log.trace("Entry config is undefined")
			}
		}
	}

	boolean checkIfConfigExsits(String tag) {
		config.withRoot{ root ->
			def customCfg = root.custom.config.find { it.'@id' ==~ tag }
			return customCfg != null
		}
	}

	/**
	 * Looks for <filter> element with "tags" parameter containing supplied tag. Method fills: <br>
	 * FILTER_PATTERN
	 * 
	 * @param tag One of "tags", which could be found in <filter> element.
	 */

	void parseFilterConfig(Map<Param, ?> params, String tag)
	{
		config.withRoot{ root ->
			log.trace("Parsing filter config for {}", tag)
			def customFilter = root.custom.filters.filter.find { checkInTags(it.'@tags', tag)}
			if (customFilter != null)
			{
				params[Param.FILTER_PATTERN] = customFilter.text()
				isAmmended = true
			}
			else
			{
				log.trace("Filter is undefined")
			}
		}
	}

	boolean checkIfFilterExsits(String tag) {
		config.withRoot{ root ->
			def customFilter = root.custom.filters.filter.find { checkInTags(it.'@tags', tag)}
			return customFilter != null
		}
	}

	/**
	 * Simply sets POST_PROCESSING to a supplied tag value
	 * 
	 * @param tag One of a <splitter> element tags
	 */

	void parsePostFilterConfig(Map<Param, ?> params, String tag)
	{
		params[Param.POST_PROCESSING] = tag
		params[Param.POST_PROCESS_PARAMS] = parsePostFilterParams(params, tag)
	}


	boolean checkIfPostProcessExsits(String tag) {
		config.withRoot { root ->
			def postPatterns = root.custom.pp_splitters.splitter.find{ checkInTags(it.'@tags', tag) }
			return postPatterns != null
		}
	}
	/**
	 * Parses PostFilter configuration from config.xml 
	 * 
	 * @param pp_tag "tag" attribute associated with post processing config
	 * @param config WgrepConfig instance
	 * @return Mapping of params desired by PostFilter
	 */

	Map parsePostFilterParams(Map<Param, ?> params, String pp_tag){
		def POST_PROCESS_SEP = null
		def POST_PROCESS_DICT = new LinkedHashMap()
		def POST_GROUPS_METHODS = []
		def POST_PROCESS_HEADER = null
		def PATTERN = new StringBuilder()
		config.withRoot{ root ->
			log.trace("Looking for splitters of type={}", pp_tag)
			def pttrns = root.custom.pp_splitters.splitter.findAll { checkInTags(it.'@tags', pp_tag) }
			log.trace("Patterns found={}", pttrns)
			if (pttrns != null) {
				pttrns.sort { it.'@order' }
				pttrns.each { ptrn_node ->
					String pttrn = ptrn_node.text()

					def sep_tag = ptrn_node.'@sep'

					if (sep_tag != null && POST_PROCESS_SEP == null) {

						if (sep_tag == '') {
							sep_tag = root.pp_config.'@default_sep'[0]
						}
						log.trace("Looking for separator={}", sep_tag)

						def sep = root.pp_config.pp_separators.separator.find { it.'@id' ==~ sep_tag}
						if (sep != null) {
							POST_PROCESS_SEP = sep.text()
							if (sep.'@spool' != null) params[Param.SPOOLING_EXT] = sep.'@spool'
						}
					}

					PATTERN = PATTERN.size() == 0 ? PATTERN.append("(?ms)").append(pttrn) : PATTERN.append(Qualifier.and.getPattern()).append(pttrn)
					def splitter_type = root.pp_config.pp_splitter_types.splitter_type.find { sp_type -> sp_type.'@id' ==~ ptrn_node.'@type' }
					def handler = splitter_type.'@handler'
					POST_PROCESS_DICT[pttrn] = handler
					if (splitter_type.'@handler_type' ==~ "group_method") {
						POST_GROUPS_METHODS.add(handler)
					}
					POST_PROCESS_HEADER = (POST_PROCESS_HEADER != null) ? POST_PROCESS_HEADER + POST_PROCESS_SEP + ptrn_node.'@col_name' : ptrn_node.'@col_name'
				}
				POST_PROCESS_HEADER += "\n"
				isAmmended = true
			}
			else {
				log.trace('POST_PROCESSING is not defined')
			}
		}
		return ["POST_PROCESS_SEP":POST_PROCESS_SEP,
			"POST_PROCESS_DICT":POST_PROCESS_DICT,
			"POST_GROUPS_METHODS":POST_GROUPS_METHODS,
			"POST_PROCESS_HEADER":POST_PROCESS_HEADER,
			"PATTERN":PATTERN.toString()]
	}

	/**
	 * Method simply sets PRESERVE_THREAD value to supplied tag
	 * 
	 * @param tag One of "tags", which could be found in <extractor> or <pattern> element
	 */

	void parseExecuteThreadConfig(Map<Param, ?> params, String tag)
	{
		params[Param.PRESERVE_THREAD] = tag
		params[Param.PRESERVE_THREAD_PARAMS] = parseComplexFilterParams(tag)
	}

	boolean checkIfExecuteThreadExsits(String tag) {
		config.withRoot { root ->
			def startExtractors = root.custom.thread_configs.extractors.pattern.find{ checkInTags(it.'@tags', tag)}
			return startExtractors != null
		}
	}

	/**
	 * 
	 * Parses appropriate ComplexFilter params from config.xml
	 * 
	 * @param preserveTag "tag" attribute associated with thread preserving patterns in config.xml
	 * @param config Initialized WgrepConfig
	 * @return Mapping of ComplexFilter params
	 */
	Map parseComplexFilterParams(String preserveTag) {
		def pt_tag = preserveTag
		def cfParams = [:]
		config.withRoot{ root ->
			if (pt_tag != null) {
				cfParams['THRD_START_EXTRCTRS'] = root.custom.thread_configs.extractors.pattern.findAll { checkInTags(it.'@tags', pt_tag) }.collect{it.text()}
				cfParams['THRD_SKIP_END_PTTRNS'] = root.custom.thread_configs.skipends.pattern.findAll { checkInTags(it.'@tags', pt_tag) }.collect{it.text()}
				cfParams['THRD_END_PTTRNS'] = root.custom.thread_configs.ends.pattern.findAll { checkInTags(it.'@tags', pt_tag) }.collect{it.text()}
				isAmmended = true
			}
			else {
				log.trace('Thread preserving is undefined')
			}
		}
		return cfParams
	}
	/**
	 * Finds config id by specified String. Method looks up for <config> element containing matching <pattern> with "alevel" parameter equal to level.
	 * 
	 * @param data String which would be matched to <pattern> element values which have corresponding to level "alevel" parameter.
	 * @return
	 */

	String findConfigIdByData(String data)
	{
		log.trace("findConfigByData started")

		if (data == null)
		{
			throw new IllegalArgumentException("Data shouldn't be null")
		}

		String id = null
		config.withRoot{ root ->
			def configs = root.custom.config.findAll { it.pattern[0] }
			def config = configs.find { config ->
				currentConfigPtrn = config.pattern[0].text()
				log.trace("ptrn=/{}/ data='{}'", currentConfigPtrn, data)
				data =~ currentConfigPtrn
			}
			if (config != null) id = config.'@id'
		}
		return id
	}

	private boolean checkInTags(String tagsToCheckIn, String tagToLookFor) {
		log.trace("Looking in /{}/ for tag /{}/",tagsToCheckIn, tagToLookFor)
		return tagsToCheckIn != null && tagsToCheckIn =~ /(?<!\w)$tagToLookFor/
	}

}
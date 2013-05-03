package org.smlt.tools.wgrep.config

import groovy.util.logging.Slf4j;
import groovy.xml.dom.DOMCategory;
import org.smlt.tools.wgrep.filters.enums.Qualifier;

/**
 * A helper class to provide automatic filter, log entry pattern identification. Currently supported identification by filename, or by specifying tag explicitly via option in config.xml
 *
 * @author Alexander Semelit 
 */

@Slf4j
class PatternAutomationHelper extends ModuleBase
{

    List ATMTN_SEQ = []
    List FIRE_ONCE_METHODS = []
    String currentConfigPtrn = null
    String currentConfigId = null
    boolean isAmmended = false
	String lv_tag

    /**
    * Constructor. Accepts WgrepConfig to initialize needed params. <br>
    * Parses automation method sequence from the config.xml depending on level tag as specified in <automation> section of the config.xml.
    */

    PatternAutomationHelper(WgrepConfig config)
    {
		super(config)
    }        
	
    void enableSequence(String level_tag) {
        lv_tag = level_tag
		log.trace("Enabling level /$lv_tag/ sequence")
        use(DOMCategory)
        {
            def levels = getRoot().automation.level.findAll { it.'@tags' =~ lv_tag}.sort {it.'@order'}
            ATMTN_SEQ.addAll(levels.collect { it.'@handler' })
            FIRE_ONCE_METHODS.addAll(levels.findAll { it.'@fireonce' == "true" }.collect{ it.'@handler'})
        }
    }

    void disableSequence() {
        lv_tag = null
        ATMTN_SEQ = []
    }

    boolean isAutomationEnabled() {
        return lv_tag != null
    }

    void applyAutomationSequence(String tag) {
		log.trace("Current sequence: " + ATMTN_SEQ)
		ATMTN_SEQ.each { handler ->
            applyMethod(tag, handler)
        }
        log.trace("Fired methods: " + FIRE_ONCE_METHODS)
        ATMTN_SEQ.removeAll(FIRE_ONCE_METHODS)
        log.trace("Config ${isAmmended ? "was": "wasn't"} ammended")
    }


	/**
	 * Applies automation sequence to the filename and tries to find <config> section which has matching pattern. <br>
	 * 
	 * 
	 * @param filename String representing file name
	 * @return true if config params were changed.
	 */
	
    boolean applySequenceByFileName(String filename)
    {
        isAmmended = false
        if (currentConfigPtrn != null)
        {
            if (filename =~ currentConfigPtrn) return isAmmended
            else currentConfigPtrn = null //since it's a different file
        }
		
        currentConfigId = findConfigIdByData(filename)
        applySequenceByTag(currentConfigId)

        return isAmmended
    }

	/**
	 * Applies automation sequence to the tag and tries to find <config> with id which matches the tag. <br>
	 * 
	 * @param tag String representing config tag as in config.xml
	 * @return true if config params were changed.
	 */
	
    boolean applySequenceByTag(String tag)
    {
        isAmmended = false
        applyAutomationSequence(tag)
        return isAmmended
    }

	/**
	 * Applies method of PatternAutomationHelper to a via reflection. <br>
	 * 
	 * @param tag String representing tag to apply method for
	 * @param method String which should be name of a declared method in PatterAutomationHelper accepting tag
	 */
	
    void applyMethod(String tag, String method)
    {
        log.trace('Applying method=' + method + ' for tag=' + tag)
		if (method == null)
		{
			throw new IllegalArgumentException("Method shouldn't be null")
		}
        this."$method"(tag)
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
	
    void parseCustomConfig(String tag)
    {
        use(DOMCategory)
        {
            def customCfg = getRoot().custom.config.find { it.'@id' ==~ tag }
            if (customCfg != null)
            {
                log.trace('Parsing entry config for ' + tag)
                
                String starter = getCDATA(customCfg.starter[0])
                String date = getCDATA(customCfg.date[0])
                if (starter != null || date != null)
                {
                    setLogEntryPattern( ((starter != null) ? starter : "") + ((date != null) ? date : "" ) )   
                }
                else
                {
                    log.warn("Either <starter> or <date> should be filled for config: " + tag)
                }
                setParam('LOG_DATE_PATTERN', customCfg.date.text())
                setParam('LOG_DATE_FORMAT', customCfg.date_format.text())
                def thrshld = customCfg.log_threshold.text()
                if (thrshld != null && thrshld != "") setParam('LOG_FILE_THRESHOLD', Integer.valueOf(thrshld))
                isAmmended = true
            }
            else
            {
                log.trace("Entry config is undefined")
            }
        }
    }
	
	boolean checkIfConfigExsits(String tag) {
		use(DOMCategory)
		{
			def customCfg = getRoot().custom.config.find { it.'@id' ==~ tag }
			return customCfg != null
		}
	}
	
	/**
	 * Looks for <filter> element with "tags" parameter containing supplied tag. Method fills: <br>
	 * FILTER_PATTERN
	 * 
	 * @param tag One of "tags", which could be found in <filter> element.
	 */

    void parseFilterConfig(String tag)
    {
        use(DOMCategory)
        {
            log.trace('Parsing filter config for ' + tag)
            def customFilter = getRoot().custom.filters.filter.find { it.'@tags' =~ tag}
            if (customFilter != null)
            {
                setFilterPattern(getCDATA(customFilter))
                isAmmended = true
            }
            else
            {
                log.trace("Filter is undefined")
            }
        }
    }
	
	boolean checkIfFilterExsits(String tag) {
		use(DOMCategory)
		{
			def customFilter = getRoot().custom.filters.filter.find { it.'@tags' =~ tag}
			return customFilter != null
		}
	}
	
	/**
	 * Simply sets POST_PROCESSING to a supplied tag value
	 * 
	 * @param tag One of a <splitter> element tags
	 */
	
    void parsePostFilterConfig(String tag)
    {
        setParam('POST_PROCESSING', tag)
        setParam('POST_PROCESS_PARAMS', parsePostFilterParams(tag))
    }
	

    boolean checkIfPostProcessExsits(String tag) {
        use(DOMCategory)
        {
            def postPatterns = getRoot().custom.pp_splitters.splitter.findAll { it.'@tags' =~ tag}
            return postPatterns != null && !postPatterns.isEmpty()
        }
    }
    /**
     * Parses PostFilter configuration from config.xml 
     * 
     * @param pp_tag "tag" attribute associated with post processing config
     * @param config WgrepConfig instance
     * @return Mapping of params desired by PostFilter
     */
    
    Map parsePostFilterParams(String pp_tag){
        def root = getRoot()
        def POST_PROCESS_SEP = null
        def POST_PROCESS_DICT = new LinkedHashMap()
        def POST_GROUPS_METHODS = []
        def POST_PROCESS_HEADER = null
        def PATTERN = new StringBuilder()
        use(DOMCategory) {
            if (log.isTraceEnabled()) log.trace("Looking for splitters of type=" + pp_tag)
            def pttrns = root.custom.pp_splitters.splitter.findAll { it.'@tags' =~ pp_tag}
            if (log.isTraceEnabled()) log.trace("Patterns found=" + pttrns)
            if (pttrns != null) {
                pttrns.sort { it.'@order' }
                pttrns.each { ptrn_node ->
                    String pttrn = getCDATA(ptrn_node)

                    def sep_tag = ptrn_node.'@sep'

                    if (sep_tag != null && POST_PROCESS_SEP == null) {

                        if (sep_tag == '') {
                            sep_tag = root.pp_config.'@default_sep'[0]
                        }
                        if (log.isTraceEnabled()) log.trace("Looking for separator=" + sep_tag)

                        def sep = root.pp_config.pp_separators.separator.find { it.'@id' ==~ sep_tag}
                        if (sep != null) {
                            POST_PROCESS_SEP = sep.text()
                            if (sep.'@spool' != null) setParam('SPOOLING_EXT', (sep.'@spool'))
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
	
    void parseExecuteThreadConfig(String tag)
    {
        setParam('PRESERVE_THREAD', tag)
        setParam('PRESERVE_THREAD_PARAMS', parseComplexFilterParams(tag))
    }
	
	boolean checkIfExecuteThreadExsits(String tag) {
		use(DOMCategory)
		{
			def startExtractors = getRoot().custom.thread_configs.extractors.pattern.findAll { it.'@tags' =~ tag}
			return startExtractors != null && !startExtractors.isEmpty()
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
        def root = getRoot()
        def pt_tag = preserveTag
        def cfParams = [:]
        use(DOMCategory) {
            if (pt_tag != null) {
                cfParams['THRD_START_EXTRCTRS'] = root.custom.thread_configs.extractors.pattern.findAll { it.'@tags' =~ pt_tag }.collect{it.text()}
                cfParams['THRD_SKIP_END_PTTRNS'] = root.custom.thread_configs.skipends.pattern.findAll { it.'@tags' =~ pt_tag }.collect{it.text()}
                cfParams['THRD_END_PTTRNS'] = root.custom.thread_configs.ends.pattern.findAll { it.'@tags' =~ pt_tag }.collect{it.text()}
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
        use(DOMCategory)
        {
            def configs = getRoot().custom.config.findAll { it.pattern[0] }
            def config = configs.find { config ->
                currentConfigPtrn = getCDATA(config.pattern[0])
                log.trace("ptrn=/" + currentConfigPtrn + "/ data='" + data + "'")
                data =~ currentConfigPtrn
            }
            if (config != null) id = config.'@id'
        }
        return id
    }



}
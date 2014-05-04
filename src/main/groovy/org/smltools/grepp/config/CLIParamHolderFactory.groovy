package org.smltools.grepp.config

import groovy.util.logging.Slf4j
import groovy.xml.DOMBuilder
import groovy.xml.dom.DOMCategory
import org.smltools.grepp.config.varparsers.*
import org.smltools.grepp.util.GreppUtil
import org.smltools.grepp.filters.enums.*
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * Class represents wgrep config, which will be used to parse incoming arguments, config.xml and would be a source for processing, filtering etc. 
 *
 * @author Alexander Semelit 
 *
 */
@Slf4j
public class CLIParamHolderFactory implements ParamHolderFactory<List<String>> {
	
	protected ConfigHolder config;

	//OPTIONS
	protected FilterParser filterParser =  null
	protected FileNameParser fileNameParser =  null
	protected Deque<ParamParser> varParsers = new ArrayDeque<ParamParser>();
	
	String currentConfigPtrn
	String currentConfigId
	boolean isAmmended = false

	protected File curWorkDir //allows to restrict access to a supplied working dir only

	public CLIParamHolderFactory(ConfigHolder pConfig) {
		config = pConfig
	}
	
	/**
	 *  Method loads defaults and spooling extension as configured in config.xml's <code>global</code> section.
	 *  Loads some values set via System properties as well.
	 */
	protected void loadDefaults(Map<Param, ?> params)
	{
		params[Param.CONFIG] = config
		
		params[Param.SPOOLING_EXT] = config.defaults.spoolingFileExtension
		params[Param.RESULTS_DIR] = config.defaults.resultsDir

		if (curWorkDir != null) {
			params.set(Param.CWD, curWorkDir)
		}
		
		def systemSep = System.getProperty("file.separator")
		params[Param.HOME_DIR] = System.getProperty("grepp.home") + systemSep
		if ("\\".equals(systemSep)) {
			systemSep += "\\"
		}
		params[Param.FOLDER_SEPARATOR] = systemSep
	}

	// INITIALIZATION

	public void setWorkingDir(File cwd) {
		log.trace("Directory limited to {}", cwd.getAbsolutePath())
		curWorkDir = cwd
	}
	
	public File getWorkingDir() {
		return curWorkDir
	}


	/**
	 * Main method for the command-line arguments processing.
	 * <p>
	 * It processes arguments in the following way:
	 *   <li>1. Flags starting with - and options starting with --</li>
	 *   <li>2. All other arguments</li>
	 * <p>
	 * All other arguments are parsed via subscribed {@link varParsers}. <br>
	 * I.e. if option, or flag requires some arguments to be parsed immediately after it was specified, a valid subclass of {@link ParserBase} should be instantiated and subscribed in the option/flag handler. <br>
	 * {@link varParsers} are iterated in a LIFO manner. Only the last one recieves an argument for parsing. As soon as parser recieves all the required arguments, it should unsubscribe, so further arguments are passed to the next parser. <br>
	 * By default the following parser are instantiated:
	 *   <li>1. {@link FilterParser}</li>
	 *   <li>2. {@link FileNameParser}</li>
	 *
	 * @param args Array of strings containing arguments for parsing.
	 */

	public ParamHolder getParamHolder(List<String> args)
	{
		if (args == null || args.size() == 0) throw new IllegalArgumentException("Invalid arguments")
		
		def cli = new CliBuilder(usage:"grepp [options] filter_regex [filename [filename]]"
            , width: 100
            , header:"Options:"
            , footer: """
===========================
Parameters:
filter_regex     - a string to find in the input. Could be replaced with pre-configured filter_regex, for this just put '--filter_regex_id'
filename         - filename for analysis. Could be multiple, or with wildcard *. In case of piping (i.e. cat bla.txt | grepp blabla) filename should be omitted.
===========================
Examples:
Using in Windows
grepp -s \"Something_I#Need ToFind\" \"D:\\myfolder\\LOGS\\myapp\\node*.log*\"
grepp -s \"SomethingINeed To Find\" D:\\myfolder\\LOGS\\myapp\\node*.log
grepp -s SomethingINeedToFind D:\\myfolder\\LOGS\\myapp\\node*.log
grepp -L \"RecordStart\" \"SomethingINeedToFind\" D:\\myfolder\\LOGS\\myapp\\node*.log*
---------------------------
Using on NIX 
grepp --my_predefined_config --dtime 2011-11-11T11:10;2011-11-11T11:11 myapp.log 
grepp --my_predefined_config --dtime 2011-11-11T11:10;-10 myapp.log 
grepp --my_predefined_regex_id myapp.log 
grepp 'SomethingINeedToFind' myanotherapp.log 
grepp -s 'RecordShouldContainThis%and%ShouldContainThisAsWell' --dtime 2012-12-12T12;2012-12-12T12:12 thirdapp.log 
grepp 'RecordShouldContainThis%and%ShouldContainThisAsWell%or%ItCouldContainThis%and%This' --dtime 2009-09-09T09:00 + thirdapp.log 
grepp -s 'SimplyContainsThis' onemoreapp.log1 onemoreapp.log2 onemoreapp.log3 
cat blabla.txt | grepp -L Chapter 'Once upon a time' > myfavoritechapter.txt
""")
        cli.v(Param.VERBOSE.getDescription())
        cli.t(Param.TRACE.getDescription())
        cli.s(Param.SPOOLING.getDescription())
        cli.m(Param.FILE_MERGING.getDescription())
        cli.h("Print this message")
        cli.L(args:1, argName:"entry_regex", "Tells grepp to split the input in blocks, treating <entry_regex> as a start of the next block (so it's a block end at the same time).\n<entry_regex> - a string which will be used to \"split\" the input. Is optinal, as by default it will be looked up by the filename in config. Anyway, if not found input would be processed by line.")
        cli.p(longOpt:"parse", Param.PARSE_PROPERTIES.getDescription())
        cli.e(Param.PRESERVE_THREAD.getDescription())
        cli.d(longOpt:"dtime", args:2, valueSeparator:";", argName:"from;to", """Tells grepp to include files/log entries within the supplied timeframe.
            <from to> - string representing date constraints for current search. 
                        Default format is yyyy-mm-ddTHH:MM:ss (could be reduced till yyyy). If <from> or <to> is not known (or is indefinite) '+' can be passed as argument.
                        Date's could be constructed by an offset from NOW or from supplied date. I.e. --dtime -10;+ will mean 'searching period is last 10 minutes'.
                        E.g. --dtime 2013-05-01T12:00;-20, --dtime 2013-05-01T12:00;+20
                        If <from> is after <to> they will be swapped automatically.
                        Usage requires valid date pattern to be configured for such a file in config. Otherwise it won't be applied
""")

        def options = cli.parse(args)
        if (options.h) {
        	cli.usage()
        	return null
        }
        
        if (options.v) {
        	enforceInfo()
        }
        else if (options.t) {
        	enforceTrace()
        }
		
		ParamHolder params = new ParamHolder(this);
		filterParser = new FilterParser()
		fileNameParser = new FileNameParser()
		varParsers.addAll([filterParser, fileNameParser])

		if (options.s) {
			params.set(Param.SPOOLING, true)
		}
		if (options.m) {
			params.set(Param.FILE_MERGING, true)
		}
		if (options.L) {
			params.set(Param.USER_ENTRY_PATTERN, true)
			params.set(Param.LOG_ENTRY_PATTERN, options.L)
		}
		if (options.p) {
			params.set(Param.PARSE_PROPERTIES, true)
			varParsers.remove(filterParser)
		}
		if (options.e) {
			params.set(Param.PRESERVE_THREAD, true)
		}
		if (options.d) {
			params.set(Param.DATE_TIME_FILTER, true)
			def dtimeParser = new DateTimeParser()
			def map = params.getModifiableParams()
			log.trace('Got date options: {}', options.ds)
			options.ds.each {
				dtimeParser.parseVar(config, map, it)
			}
		}

		params.withModifiableParams { map ->
			loadDefaults(map)
			for (arg in options.arguments())
			{
				log.debug("next arg: {}", arg);
				if (!this.processOptions(map, arg)) {
					ParamParser<?> paramParser = varParsers.pop() 
					if (!paramParser.parseVar(config, map, arg)) { //pushing back since this parser has more to parse
						varParsers.push(paramParser)
					}
				} 
			}
		}
		
		return params
	}

	/**
	 * Method for flags and options parsing. It identifies if passed help flag, simple flag or option, and calls appropriate method. Also it removes special symbols - and -- before passing argument further.
	 *
	 * @param arg An argument to be parsed
	 * @return <code>1</code> if <code>arg</code> was processed(i.e. it was a valid arg) <code>0</code> otherwise.
	 */

	protected boolean processOptions(Map<Param,?> params, String arg)
	{
		if (arg =~/^-(?![-0-9])/) //option should be a char and not a number
		{
			this.processSimpleArg(params, arg.substring(1)) //skipping '-' itself
			return true
		}
		else if (arg =~ /^--/)
		{
			this.processComlpexArg(params, arg.substring(2)) //skipping '--' itself
			return true
		}
		return false
	}

	/**
	 * Method for simple flags. It tokenizes passed string by each symbol.
	 * I.e. for each character it will be looking for corresponding optiong in config.xml
	 * Such behaviour was introduced to support multiple flags at once, like '-abcd'.
	 * For each tokenized character it calls {@link processOption} method.
	 *
	 * @param arg An argument to be parsed
	 */
	protected void processSimpleArg(Map<Param,?> params, String arg)
	{
		throw new IllegalArgumentException("Invalid flag: " + arg)
	}

	/**
	 * Method for complex flags/options. There is no complex logic at the moment.
	 * It fetches every character from string and passes the result to {@link processOption} method.
	 *
	 * @param arg An argument to be parsed
	 */

	protected void processComlpexArg(Map<Param,?> params, String arg)
	{
		(arg =~ /.*/).each{opt -> if(opt) this.processOption(params, opt)}
	}

	/**
	 * Method which performs actual option lookup in the config.xml.
	 * <p>
	 * It fetches handler function (from <code>handler</code> attribute), and calls it from {@link WgrepFacade} class.
	 * <p> It passes to the handler function <code>field</code> attribute and value of matching option from config.xml.
	 *
	 * @param opt An argument to be looked up
	 * @throws IllegalArgumentException If the supplied <code>arg</code> is not configured, i.e. cannot be found in the config.xml.
	 */

	protected void processOption(Map<Param,?> params, String opt)
	{
		loadParamsById(params, opt)
	}

	protected void loadParamsById(Map<Param, ?> params, String id) {
		boolean matchedAny = false
		if (checkIfConfigExsits(id)) { //checking if  there exists a config with such id and applying it if so
			matchedAny = matchedAny || true
			params[Param.PREDEF_TAG] = id
			setPredefinedConfig(params, id)
		}
		if (checkIfFilterExsits(id)) { //checking filter wasn't supplied explicitly and there exists a filter with such id and applying it if so
			matchedAny = matchedAny || true
			params[Param.PREDEF_TAG] = id
			setPredefinedFilter(params, id)
			varParsers.remove(filterParser)			
		}
		if (checkIfExecuteThreadExsits(id)) { //checking if there exists a thread preserving patterns with such id and applying it if so
			matchedAny = matchedAny || true
			setThreadPreserving(params, id)
		}
		if (checkIfPostProcessExsits(id)) { //checking if there exists a post_processing config with such id and applying it if so
			matchedAny = matchedAny || true
			setPostProcessing(params, id)
		}
		if (!matchedAny) {
			throw new IllegalArgumentException("Invalid id, doesn't match any pre-configured: " + id)
		}
	}
	

    /**
    * Method for refreshing config params by a filename. Requires {@link PatternAutomationHelper} paHelper to be initialized. <br>
    * Calls {@link PatternAutomationHelper.applySequenceByFileName}
    * @param fileName String representing name of a file to be checked. Could be an absolute path as well.
    */

	boolean refreshParams(ParamHolder params, Object file)
	{
		if (file instanceof File) {
			return refreshParamsByFile(params, file as File)
		}
		else if (file instanceof String) {
			return refreshParamsByFile(params, file as String)
		}
		else {
			return false
		}
	}

	
	boolean refreshParamsByFile(ParamHolder params, File file)
    {
        return refreshParamsByFile(params, file.getName())
    }
    
	/**
	 * Method for refreshing config params by a filename. Requires {@link PatternAutomationHelper} paHelper to be initialized. <br>
	 * Calls {@link PatternAutomationHelper.applySequenceByFileName}
	 * @param fileName String representing name of a file to be checked. Could be an absolute path as well.
	 */

	boolean refreshParamsByFile(ParamHolder paramsHolder, String fileName)
	{
		return loadParamsByFileName(paramsHolder.getModifiableParams(), fileName)
	}

	/**
	*
	* Method enforces TRACE level of logging by resetting logback config and redirects it to STDOUT.
	*/

	protected void enforceTrace()
	{
		log.debug("Enabling trace")
		String traceConfig ="""\
<configuration>

  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <!-- encoders are assigned the type
         ch.qos.logback.classic.encoder.PatternLayoutEncoder by default -->
    <encoder>
      <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>

  <root level="trace">
    <appender-ref ref="STDOUT" />
  </root>
</configuration>
"""
		GreppUtil.resetLogging(traceConfig)
	}

	/**
	*
	* Method enforces INFO level of logging by resetting logback config and redirects it to STDOUT.
	*/

	protected void enforceInfo()
	{
		log.debug("Redirecting info to STDOUT")
		String infoConfig ="""\
<configuration>

  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <!-- encoders are assigned the type
         ch.qos.logback.classic.encoder.PatternLayoutEncoder by default -->
    <encoder>
      <pattern>%msg%n</pattern>
    </encoder>
  </appender>

  <root level="info">
    <appender-ref ref="STDOUT" />
  </root>
</configuration>
"""
		GreppUtil.resetLogging(infoConfig)
	}


	/**
	 * Sets <code>FILTER_PATTERN</code> according to on supplied <code>tag</code> from <code>filters</code> section of config.xml. If pattern automation.
	 * @param field Field to be set
	 * @param val <code>String</code> value to be set. Valid config preset tag from <code>automation</code> section is expected here.
	 */
	protected void setPredefinedConfig(Map<Param, ?> params, def val)
	{
		parseCustomConfig(params, val)
	}


	/**
	 * Sets <code>FILTER_PATTERN</code> according to on supplied <code>tag</code> from <code>filters</code> section of config.xml. Requires pattern automation to operate. <br>
	 * Calls {@link PatternAutomation.parseFilterConfig}
	 * @param field Field to be set
	 * @param val <code>String</code> value to be set. Valid config preset tag from <code>automation</code> section is expected here.
	 */
	protected void setPredefinedFilter(Map<Param, ?> params, def val)
	{
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


	boolean loadParamsByFileName(Map<Param, ?> params, String filename)
	{
		isAmmended = false
		if (currentConfigPtrn != null)
		{
			if (filename =~ currentConfigPtrn) return isAmmended
			else currentConfigPtrn = null //since it's a different file
		}

		currentConfigId = findConfigIdByData(filename)
		loadParamsById(params, currentConfigId)

		return isAmmended
	}

	/**
	 * Gets savedConfig section with id equal to supplied tag. Depending on which elements are supplied it fills: <br>
	 * LOG_ENTRY_PATTERN <br>
	 * LOG_DATE_PATTERN <br>
	 * LOG_DATE_FORMAT <br>
	 * LOG_FILE_THRESHOLD <br>
	 * If custom config was found and params were filled, toggles isAmmended flag.
	 * 
	 * @param savedConfig's id
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
		if (params[Param.PRESERVE_THREAD]) {
			params[Param.PRESERVE_THREAD_PARAMS] = parseComplexFilterParams(tag)
		}
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

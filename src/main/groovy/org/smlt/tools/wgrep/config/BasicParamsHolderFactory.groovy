package org.smlt.tools.wgrep.config

import groovy.util.logging.Slf4j
import groovy.xml.DOMBuilder
import groovy.xml.dom.DOMCategory
import org.smlt.tools.wgrep.config.varparsers.*
import org.smlt.tools.wgrep.util.WgrepUtil;
import org.w3c.dom.Document
import org.w3c.dom.Element

import com.sun.org.apache.xalan.internal.xsltc.compiler.FilterParentPath;

/**
 * Class represents wgrep config, which will be used to parse incoming arguments, config.xml and would be a source for processing, filtering etc. 
 *
 * @author Alexander Semelit 
 *
 */
@Slf4j
public class BasicParamsHolderFactory implements ParamsHolderFactory<Object> {
	
	protected ConfigHolder config;

	//OPTIONS
	protected FilterParser filterParser =  null
	protected FileNameParser fileNameParser =  null
	protected Deque<ParamParser> varParsers = new ArrayDeque<ParamParser>();
	

	public BasicParamsHolderFactory(ConfigHolder pConfig) {
		config = pConfig
	}
	
	/**
	 *  Method loads defaults and spooling extension as configured in config.xml's <code>global</code> section.
	 *  Loads some values set via System properties as well.
	 */
	protected void loadDefaults(Map<Param, ?> params)
	{
		params[Param.CONFIG_FILE_PATH] = config.getConfigFilePath()
		
		config.withRoot{ root ->
			params[Param.SPOOLING_EXT] = root.global.spooling[0].text()
			params[Param.RESULTS_DIR] = root.global.results_dir[0].text()
		}
				
		def systemSep = System.getProperty("file.separator")
		params[Param.HOME_DIR] = System.getProperty("wgrep.home") + systemSep
		if (systemSep == "\\") {
			systemSep += "\\"
		}
		params[Param.FOLDER_SEPARATOR] = systemSep

		def defaults = config.withRoot{ root ->
				def defaultOptions = root.global.default_options[0]
				if (defaultOptions != null)
				{
					return defaultOptions.text();
				}
				else {
					return defaultOptions;
				}
		}
		
		if (defaults != null) {
			log.info("Processing defaults {}", defaults)
			defaults.split(" ").each { opt -> processOptions(params, opt) } //processing default options
		}
		else {
			log.info("Defaults not specified")
		}
	}

	// Getters

	def getDataForProcessing() {
		def data = null
		if (check(['FILES'], null)) {
			log.trace("Files were specified")
			data = getParam('FILES')
		}
		else if (System.in.available() > 0) {
			data = System.in
		}
		return data
	}

	// INITIALIZATION

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

	public ParamsHolder getParamsHolder(Object args)
	{
		if (args == null || args.size() == 0) throw new IllegalArgumentException("Invalid arguments")
		
		filterParser = new FilterParser()
		fileNameParser = new FileNameParser()
		varParsers.addAll([filterParser, fileNameParser])
		ParamsHolder params = new ParamsHolder(this);

		params.withModifiableParams { map ->
			loadDefaults(map)
			for (arg in args)
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
		(arg =~ /./).each{opt -> this.processOption(params, opt)}
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
		config.withRoot{ root ->
			def optElem = root.options.opt.find {it.text() == opt}
			if (optElem == null) optElem = root.custom.options.opt.find {it.text() == opt}
			if (optElem != null)
			{
				def handler = optElem['@handler']
				def value = optElem.text()
				params[Param.valueOf(optElem['@field'])] = value   
				log.trace("Calling: {} for {}", handler, value)
				if (handler != null && handler != "") {
					this."$handler"(params, value)
				}
			}
			else {
				optionNotFoundHook(params, opt)
			}
		}
	}

	protected void optionNotFoundHook(Map<Param, ?> params, String opt) {
		throw new IllegalArgumentException("Invalid option, doesn't match any <opt>: " + opt)
	}
	

    /**
    * Method for refreshing config params by a filename. Requires {@link PatternAutomationHelper} paHelper to be initialized. <br>
    * Calls {@link PatternAutomationHelper.applySequenceByFileName}
    * @param fileName String representing name of a file to be checked. Could be an absolute path as well.
    */

	boolean refreshParams(ParamsHolder params, Object file)
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

	
	boolean refreshParamsByFile(ParamsHolder params, File file)
    {
        return refreshParamsByFile(params, file.getName())
    }
    
    boolean refreshParamsByFile(ParamsHolder params, String fileName)
    {
        return false
    }

    /**
	 * Enables post processing. Initializes {@link DateTimeParser}.
	 * @param field Field to be set
	 * @param val <code>String</code> value to be set. Valid config preset tag from <code>date_time_config</code> section is expected here.
	 */

	protected void setDateTimeFilter(Map<Param, ?> params, def val)
	{
		varParsers.push(new DateTimeParser())
	}

	/**
	 * Disables pattern autoidentification and enables user-supplied log entry pattern. Initializes {@link LogEntryParser}.
	 * @param field Field to be set
	 * @param val <code>String</code> value to be set. Valid config preset tag from <code>date_time_config</code> section is expected here.
	 */

	protected void setUserLEPattern(Map<Param, ?> params, def val)
	{
		varParsers.push(new LogEntryParser())
	}

	protected void setPropertiesParsing(Map<Param, ?> params, def val) 
	{
		varParsers.remove(filterParser)
	}
	
	/**
	 * Method prints out some help
	 * <p>
	 * Actually it has the same date as in groovydoc.
	 */
	protected void printHelp(Map<Param, ?> params, def val)
	{
		varParsers.clear(); //unsubscribing all
		def help = """\
CLI program to analyze text files in a regex manner. Adding a feature of a log record splitting, thread-coupling and reporting.

Usage via supplied .bat or .sh file: 
wgrep [-[:option:]] [--:filter_option:] [-L LOG_ENTRY_PATTERN] [FILTER_PATTERN] [--dtime FROM_DATE TO_DATE] [FILENAME [FILENAME]]

option 				- single character represeting a configured in config.xml <opt> element

filter_option 		- a word representing configured <opt> element.
						Or one <config> @id attribute; or one of @tag attributes from patterns in config.xml

LOG_ENTRY_PATTERN 	- a string which will be used to \"split\" the input. 
						Is optinal, as by default it will be looked up by filename in config.xml mapping

FILTER_PATTERN 		- a string which will is needed to be found in the input.  
						Is optional, as it can be identified by '--filter_option' or by '-option'

FROM_DATE/TO_DATE 	- string representing date constraints for current search. 
						Default format is yyyy-mm-ddThh-MM-ss (could be reduced till yyyy). If FROM_DATE or TO_DATE is not known (or is indefinite) '+' can be passed as argument.
						Date's could be constructed by an offset from NOW or from supplied date. I.e. --dtime -10 + will mean 'searching period is last 10 minutes'.
						E.g. --dtime 2013-05-01T12:00 -20, --dtime 2013-05-01T12:00 +20
						If TO_DATE is after FROM_DATE they will be swapped automatically.
						Usage requires valid date pattern to be configured for such a file in config.xml

FILENAME 			- filename for analysis. Could be multiple, or with wildcard *. To use input stream contents for analysis do not specify any file names

Examples:

Using in Windows
wgrep -s \"Something_I#Need ToFind\" \"D:\\myfolder\\LOGS\\myapp\\node*.log*\"
wgrep -s \"SomethingINeed To Find\" D:\\myfolder\\LOGS\\myapp\\node*.log
wgrep -s SomethingINeedToFind D:\\myfolder\\LOGS\\myapp\\node*.log
wgrep -L \"RecordStart\" \"SomethingINeedToFind\" D:\\myfolder\\LOGS\\myapp\\node*.log*

Using on NIX 
wgrep --my_predefined_config --dtime 2011-11-11T11:10 2011-11-11T11:11 myapp.log 
wgrep --my_predefined_config --dtime 2011-11-11T11:10 -10 myapp.log 
wgrep --my_predefined_config myapp.log 
wgrep 'SomethingINeedToFind' myanotherapp.log 
wgrep -s 'RecordShouldContainThis%and%ShouldContainThisAsWell' --dtime 2012-12-12T12 2012-12-12T12:12 thirdapp.log 
wgrep 'RecordShouldContainThis%and%ShouldContainThisAsWell%or%ItCouldContainThis%and%This' --dtime 2009-09-09T09:00 + thirdapp.log 
wgrep -s 'SimplyContainsThis' onemoreapp.log1 onemoreapp.log2 onemoreapp.log3 
cat blabla.txt | wgrep -L Chapter 'Once upon a time' > myfavoritechapter.txt
"""
		println help
	}

	/**
	*
	* Method enforces TRACE level of logging by resetting logback config and redirects it to STDOUT.
	*/

	protected void enforceTrace(Map<Param, ?> params, def val)
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
		WgrepUtil.resetLogging(traceConfig)
	}

	/**
	*
	* Method enforces INFO level of logging by resetting logback config and redirects it to STDOUT.
	*/

	protected void enforceInfo(Map<Param, ?> params, def val)
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
		WgrepUtil.resetLogging(infoConfig)
	}


}

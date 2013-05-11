package org.smlt.tools.wgrep.config

import groovy.util.logging.Slf4j
import groovy.xml.DOMBuilder
import groovy.xml.dom.DOMCategory
import org.smlt.tools.wgrep.config.varparsers.*
import org.smlt.tools.wgrep.util.WgrepUtil;
import org.w3c.dom.Document
import org.w3c.dom.Element
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

/**
 * Class represents wgrep config, which will be used to parse incoming arguments, config.xml and would be a source for processing, filtering etc. 
 *
 * @author Alexander Semelit 
 *
 */
@Slf4j
class WgrepConfig {

	//internal
	protected def configValidator
	protected Document cfgDoc = null
	protected Element root = null
	//GLOBAL
	protected String FOLDER_SEPARATOR = null
	protected String CWD = null
	protected String HOME_DIR = null
	protected String RESULTS_DIR = 'results'
	protected String SPOOLING_EXT = 'log'


	//GENERAL
	protected List<File> FILES = []

	//OPTIONS
	protected FilterParser filterParser =  null
	protected FileNameParser fileNameParser =  null
	protected List<ParserBase> varParsers = [] //organized as LIFO
	protected Map params = [:] //all params as a Map

	/**
	 * Constructor <br>
	 * Initializes the instance. Parses config.xml and loads defaults from there.
	 *
	 * @param configFilePath String which can be recognized by a <code>FileReader</code> and is a valid path to an config.xml file
	 */
	WgrepConfig(String configFilePath)
	{
		this(configFilePath, null)	
	}
	
	
	WgrepConfig(String configFilePath, String configXSDpath)
	{
		loadConfigInternal(configFilePath, configXSDpath)
	}

	public void loadConfig(String configFilePath) {
		if (configValidator == null || validateConfigFile(configFilePath)) {
			initConfig(configFilePath)
		}
		else {
			throw new RuntimeException("config.xml is invalid")
		}
	}
	
	protected void loadConfigInternal(String configFilePath, String configXSDpath) {
		if (configXSDpath == null || validateConfigFile(configFilePath, configXSDpath)) {
			initConfig(configFilePath)
		}
		else {
			throw new RuntimeException("config.xml is invalid")
		}
	}

	protected boolean validateConfigFile(String configFilePath) {
		log.trace("Validating the config")
		configValidator.validate(new StreamSource(new FileReader(configFilePath)))
		return true
	}
	
	protected boolean validateConfigFile(String configFilePath, String configXSDpath) {
		log.trace("Loading validator")
		def factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
		def schema = factory.newSchema(new StreamSource(new FileReader(configXSDpath)))
		this.configValidator = schema.newValidator()
		return validateConfigFile(configFilePath)
	}
	
	protected void initConfig(String configFilePath) {
		this.cfgDoc = DOMBuilder.parse(new FileReader(configFilePath))
		this.root = cfgDoc.documentElement
		loadDefaults()
	}

	/**
	 *  Method loads defaults and spooling extension as configured in config.xml's <code>global</code> section.
	 *  Loads some values set via System properties as well.
	 */
	protected void loadDefaults()
	{
		this.FOLDER_SEPARATOR = System.getProperty("file.separator")
		this.HOME_DIR = System.getProperty("wgrep.home") + FOLDER_SEPARATOR
		if (FOLDER_SEPARATOR == "\\") {
			this.FOLDER_SEPARATOR += "\\"
		}
		use(DOMCategory)
		{
			this.SPOOLING_EXT = root.global.spooling[0].text()
			def defaultOptions = root.global.default_options[0]
			if (defaultOptions != null)
			{
				defaultOptions.text().split(" ").each { opt -> processOptions(opt) } //processing default options
			}
		}
		
	}

	// Getters


	/**
	 * Getter to extract CDATA element value from a node which is expected to be text.
	 * @return <code>node.text()</code> if the node has text. Value of CDATA element i.e. <code>node.getFirstChild().getNodeValue()</code> otherwise.
	 */

	String getCDATA(Element node)
	{
		return WgrepUtil.getCDATA(node)
	}

	/**
	 * Gets value of the {@link this.params} by key.
	 * @param field Key for <code>params</code> which is needed to be get.
	 * @return Value set to the key <code>field</code>
	 */

	def getParam(String field)
	{
		if (field == null) return
		return hasField(field) ? this."$field" : this.params[field]
	}

	/**
	 * Sets value of any field which exists in <code>WgrepFacade</code> via reflection. Is used to propagate value directly from config.xml
	 * @param field Name of field of <code>WgrepFacade</code> which is needed to be set.
	 * @param val Value to be set
	 */

	void setParam(String field, def val)
	{
		if (field == 'FILES')
		{
			this.FILES.add(val)
		}
		else if (hasField(field))
		{
			this."$field" = val
		}
		else
		{
			this.params[field] = val
		}
	}

	/**
	 * Method checks if mandatory and optional parameters are filled.
	 * @return <code>true</code> if check is passed. <code>false</code> otherwise.
	 */

	boolean check(List<String> mandatory, List<String> optional)
	{
		boolean checkResult = true
		
		def emptyMandatory = mandatory.findAll{ paramName -> checkParamIsEmpty(paramName)}
			.each{ paramName ->
					log.error("Mandatory param {} is empty", paramName)
			}
		
		if (emptyMandatory.size() > 0) return false

		optional.findAll{ paramName -> checkParamIsEmpty(paramName)}
			.each{ paramName ->
				log.warn("Optional param {} is empty", paramName)
			}

		return checkResult
	}

	/**
	 * Method checks if param is empty.
	 * @return <code>true</code> if it is empty. <code>false</code> otherwise.
	 */

	boolean checkParamIsEmpty(String paramName) {
		def param = getParam(paramName)
		if (param != null) {
			if (param instanceof Collection) {
				return param.size() == 0
			}
			if (param instanceof Map) {
				return param.size() == 0
			}
			return false
		}
		else {
			return true
		}
	}

	/**
	* Checks if specified field is declared or not. 
	* Used to determine whether the field should be looked up in the <code>params</code> Map or not.
	*
	*/

	protected boolean hasField(String field)
	{
		WgrepUtil.hasField(this.getClass(), field)
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

	void processInVars(def args)
	{
		if (args == null || args.size() == 0) throw new IllegalArgumentException("Invalid arguments")

		filterParser = new FilterParser(this)
		fileNameParser = new FileNameParser(this)
		subscribeVarParsers([fileNameParser, filterParser])

		for (arg in args)
		{

			log.debug("next arg: {}", arg);

			switch (processOptions(arg))
			{
				case 1:
					break
				default:
					parseVar(arg)
			}
		}

		unsubscribeVarParsers(varParsers)
	}

	/**
	 * Method for flags and options parsing. It identifies if passed help flag, simple flag or option, and calls appropriate method. Also it removes special symbols - and -- before passing argument further.
	 *
	 * @param arg An argument to be parsed
	 * @return <code>1</code> if <code>arg</code> was processed(i.e. it was a valid arg) <code>0</code> otherwise.
	 */

	protected int processOptions(String arg)
	{
		if (arg =~/^-(?![-0-9])/) //option should be a char and not a number
		{
			processSimpleArg(arg.substring(1)) //skipping '-' itself
			return 1
		}
		else if (arg =~ /^--/)
		{
			processComlpexArg(arg.substring(2)) //skipping '--' itself
			return 1
		}
		return 0
	}

	/**
	 * Method for simple flags. It tokenizes passed string by each symbol.
	 * I.e. for each character it will be looking for corresponding optiong in config.xml
	 * Such behaviour was introduced to support multiple flags at once, like '-abcd'.
	 * For each tokenized character it calls {@link processOption} method.
	 *
	 * @param arg An argument to be parsed
	 */
	protected void processSimpleArg(String arg)
	{
		(arg =~ /./).each{opt -> processOption(opt)}
	}

	/**
	 * Method for complex flags/options. There is no complex logic at the moment.
	 * It fetches every character from string and passes the result to {@link processOption} method.
	 *
	 * @param arg An argument to be parsed
	 */

	protected void processComlpexArg(String arg)
	{
		(arg =~ /.*/).each{opt -> if(opt) processOption(opt)}
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

	protected void processOption(String opt)
	{
		use(DOMCategory)
		{
			def optElem = root.options.opt.find {it.text() == opt}
			if (optElem == null) optElem = root.custom.options.opt.find {it.text() == opt}
			if (optElem != null)
			{
				def handler = optElem['@handler']
				this."$handler"(optElem['@field'], optElem.text())
			}
			else {

				 throw new IllegalArgumentException("Invalid option, doesn't match any <opt>: " + opt)
			}
		}
	}

	/**
	 * Method for parsing arguments which are not options or flags. I.e. they are variable values needed for correct processing.
	 * <p>
	 * It gets last parser from {@link varParsers} array and calls it <code>parseVar</code> function with supplied <code>arg</code>
	 *
	 * @param arg An argument to be parsed
	 */

	protected void parseVar(String arg)
	{
		if (varParsers == null) {
			return
		}

		int nextParserIdx = varParsers.size() - 1
		if (nextParserIdx >= 0)
		{
			log.debug("passing to parser")
			varParsers[nextParserIdx].parseVar(arg)
		}
	}

	/**
	 * Method for subscribing var parsers.
	 *
	 * @param parsers List of {@link ParserBase} objects.
	 */

	void subscribeVarParsers(List<ParserBase> parsers)
	{
		this.varParsers.addAll(parsers)
	}

	/**
	 * Method for unsubscribing var parsers.
	 *
	 * @param parsers List of {@link ParserBase} objects.
	 */

	void unsubscribeVarParsers(List<ParserBase> parsers)
	{
		this.varParsers.removeAll(parsers)
	}


    /**
    * Method for refreshing config params by a filename. Requires {@link PatternAutomationHelper} paHelper to be initialized. <br>
    * Calls {@link PatternAutomationHelper.applySequenceByFileName}
    * @param fileName String representing name of a file to be checked. Could be an absolute path as well.
    */

    boolean refreshConfigByFile(File file)
    {
        return refreshConfigByFile(file.getName())
    }
    
    boolean refreshConfigByFile(String fileName)
    {
        return false
    }

    /**
	 * Enables post processing. Initializes {@link DateTimeParser}.
	 * @param field Field to be set
	 * @param val <code>String</code> value to be set. Valid config preset tag from <code>date_time_config</code> section is expected here.
	 */

	protected void setDateTimeFilter(String field, def val)
	{
		setParam(field, val)
		new DateTimeParser(this).subscribe()
	}

	/**
	 * Disables pattern autoidentification and enables user-supplied log entry pattern. Initializes {@link LogEntryParser}.
	 * @param field Field to be set
	 * @param val <code>String</code> value to be set. Valid config preset tag from <code>date_time_config</code> section is expected here.
	 */

	protected void setUserLEPattern(String field, def val)
	{
		setParam(field, val)
		new LogEntryParser(this).subscribe()
	}


	/**
	 * Method prints out some help
	 * <p>
	 * Actually it has the same date as in groovydoc.
	 */
	protected void printHelp(String field, def arg)
	{
		unsubscribeVarParsers(varParsers); //unsubscribing all
		def help = """\
CLI program to analyze text files in a regex manner. Adding a feature of a log record splitting, thread-coupling and reporting.

Usage via supplied .bat or .sh file: 
wgrep [CONFIG_FILE] [-[:option:]] [--:filter_option:] [-L LOG_ENTRY_PATTERN] [FILTER_PATTERN] [--dtime FROM_DATE TO_DATE] FILENAME [FILENAME]

CONFIG_FILE 		- path to config.xml for wgrep to use. If none is specified, it will be looked up in wgrep classpath

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

FILENAME 			- filename for analysis. Could be multiple, or with wildcard *

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
"""
		println help
	}

	/**
	*
	* Method enforces TRACE level of logging by resetting logback config and redirects it to STDOUT.
	*/

	protected void enforceTrace(String field, def val)
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

	protected void enforceInfo(String field, def val)
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

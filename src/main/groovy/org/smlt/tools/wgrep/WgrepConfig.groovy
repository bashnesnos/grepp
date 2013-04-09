package org.smlt.tools.wgrep

import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.joran.spi.JoranException
import ch.qos.logback.core.util.StatusPrinter
import groovy.util.logging.Slf4j
import groovy.xml.DOMBuilder
import groovy.xml.dom.DOMCategory
import org.smlt.tools.wgrep.varparsers.*
import org.w3c.dom.Document
import org.w3c.dom.Element

@Slf4j
class WgrepConfig {
	//internal
	private Document cfgDoc = null
	private Element root = null
	//GLOBAL
	private String FOLDER_SEPARATOR = null
	private String CWD = null
	private String HOME_DIR = null
	private String RESULTS_DIR = 'results'
	private String SPOOLING_EXT = 'log'


	//GENERAL
	private List FILES = []

	//OPTIONS
	private FilterParser filterParser =  null
	private FileNameParser fileNameParser =  null
	private PatternAutomationHelper paHelper = null

	private List<ParserBase> varParsers = [] //organized as LIFO
	private Map params = [:] //all params as a Map

	/**
	 * Constructor
	 * <p>
	 * Initializes the instance. Parses config.xml and loads defaults from there.
	 *
	 * @param args Params needed for the facade initialization. Currently only path to the config.xml is expected.
	 */
	WgrepConfig(String configFile)
	{
		cfgDoc = DOMBuilder.parse(new FileReader(configFile))
		root = cfgDoc.documentElement
		CWD = System.getProperty("user.dir")
		loadDefaults()
	}

	/**
	 *  Method loads default mode and spooling extension as configured in config.xml
	 */
	private void loadDefaults()
	{
		FOLDER_SEPARATOR = System.getProperty("file.separator")
		HOME_DIR = System.getProperty("wgrep.home") + FOLDER_SEPARATOR
		if (FOLDER_SEPARATOR == "\\") FOLDER_SEPARATOR += "\\"
		use(DOMCategory)
		{
			SPOOLING_EXT = root.global.spooling[0].text()
			setParam('ATMTN_LEVEL', root.global.automation_level[0].text()) //setting default automation level
		}
	}

	// Getters


	/**
	 * Getter to extract CDATA element value from a node which is expected to be text.
	 * @return <code>node.text()</code> if the node has text. Value of CDATA element i.e. <code>node.getFirstChild().getNodeValue()</code> otherwise.
	 */

	String getCDATA(Element node)
	{
		if (node == null) return
		use(DOMCategory)
		{
			def txt = node.text()
			return (txt)?txt:node.getFirstChild().getNodeValue()
		}
	}

	/**
	 * Gets value of the {@link this.params} by key.
	 * @param field Key for <code>params</code> which is needed to be get.
	 * @return Value set to the key <code>field</code>
	 */

	def getParam(String field)
	{
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
			FILES.add(val)
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

	private boolean hasField(String field)
	{
		try {
			this.getClass().getDeclaredField(field)
		}
		catch (NoSuchFieldException e) {
			return false
		}
		return true
	}
	// INITIALIZATION

	/**
	 * Main method for the command-line arguments processing.
	 * <p>
	 * It processes arguments in the following way:
	 *   <li>1. Flags starting with - and options starting with --</li>
	 *   <li>2. {@link this.LOG_ENTRY_PATTERN} if not overrided by any flag/option</li>
	 *   <li>3. {@link this.FILTER_PATTERN} if not overrided by any flag/option</li>
	 *   <li>4. Additional modules, like {@link DateTimeChecker} variables</li>
	 *   <li>5. All other are treated as consequential filenames to be checked</li>
	 * <p>
	 * As soon as they are processed, it starts module initialization.
	 * @param args Array of strings containing arguments for parsing.
	 */

	void processInVars(def args)
	{
		if (args == null || args.size() == 0) throw new IllegalArgumentException("Invalid arguments")

		filterParser = new FilterParser(this)
		fileNameParser = new FileNameParser(this)
		subscribeVarParsers([fileNameParser, filterParser])

		paHelper = new PatternAutomationHelper(this)

		for (arg in args)
		{

			log.debug("next arg: ${arg}");

			switch (processOptions(arg))
			{
				case 1:
					break
				default:
					parseVar(arg)
			}
		}
	}

	/**
	 * Method for flags and options parsing. It identifies if passed help flag, simple flag or option, and calls appropriate method. Also it removes special symbols - and -- before passing argument further.
	 *
	 * @param arg An argument to be parsed
	 * @return <code>1</code> if <code>arg</code> was processed(i.e. it was a valid arg) <code>null</code> otherwise.
	 */

	private int processOptions(String arg)
	{
		if (arg =~/^-(?!-)/)
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
	private void processSimpleArg(String arg)
	{
		(arg =~ /./).each{opt -> processOption(opt)}
	}

	/**
	 * Method for complex flags/options. There is no complex logic at the moment.
	 * It fetches every character from string and passes the result to {@link processOption} method.
	 *
	 * @param arg An argument to be parsed
	 */

	private void processComlpexArg(String arg)
	{
		(arg =~ /.*/).each{opt -> if(opt) processOption(opt)}
	}

	/**
	 * Method which performs actual option lookup in the config.xml.
	 * <p>
	 * It fetches handler function (from <code>handler</code> attribute), and calls it from {@link WgrepFacade} class.
	 * <p> It passes to the handler function <code>field</code> attribute and value of matching option from config.xml.
	 *
	 * @param arg An argument to be looked up
	 * @throws IllegalArgumentException If the supplied <code>arg</code> is not configured, i.e. cannot be found in the config.xml.
	 */

	private void processOption(String opt)
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
			else throw new IllegalArgumentException("Invalid option=" + opt)
		}
	}

	/**
	 * Method for parsing variables.
	 * <p>
	 * It gets first parser from {@link varParsers} array and calls it <code>parseVar</code> function with supplied <code>arg</code>
	 *
	 * @param arg An argument to be parsed
	 * @return <code>1</code> if <code>arg</code> was processed(i.e. if there was any {@link AdditionalVarParser} subscribed) <code>null</code> otherwise.
	 */

	private void parseVar(String arg)
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
	 * Method for subscribing additional parsers.
	 *
	 * @param parsers Collection of {@link AdditionalVarParser} objects.
	 */

	void subscribeVarParsers(List<ParserBase> parsers)
	{
		varParsers.addAll(parsers)
	}

	/**
	 * Method for unsubscribing additional parsers.
	 *
	 * @param parsers Collection of {@link AdditionalVarParser} objects.
	 */

	void unsubscribeVarParsers(List<ParserBase> parsers)
	{
		varParsers.removeAll(parsers)
	}

	/**
	 * Enables extended pattern processing.
	 * @param field Field to be set. Either <code>EXTNDD_PATTERN</code> for just enabling or <code>PRESERVE_THREAD</code> if it should enable thread parsing as well.
	 * @param val <code>String</code> value to be set. Either <code>e</code> if it just enabling, and a valid config preset tag from <code>thread_configs</code> section otherwise.
	 */

	private void setExtendedPattern(String field, def val)
	{
		setParam(field, val)
	}

	/**
	 * Enables post processing.
	 * @param field Field to be set
	 * @param val <code>String</code> value to be set. Valid config preset tag from <code>pp_splitters</code> section is expected here.
	 */

	private void setPostProcessing(String field, def val)
	{
		setParam(field, val)
	}

	/**
	 * Enables post processing. Initializes {@link DateTimeVarParser}.
	 * @param field Field to be set
	 * @param val <code>String</code> value to be set. Valid config preset tag from <code>date_time_config</code> section is expected here.
	 */

	private void setDateTimeFilter(String field, def val)
	{
		setParam(field, val)
		new DateTimeParser(this).subscribe()
	}

	private void setUserLEPattern(String field, def val)
	{
		setParam(field, val)
		new LogEntryParser(this).subscribe()
		disableAutomation()
	}

	/**
	 * Enables <code>LOG_ENTRY_PATTERN</code>, <code>FILTER_PATTERN</code>, <code>PRESERVE_THREAD</code> auto-identification based on supplied <code>level</code>. Initializes {@link PatternAutomationHelper}.
	 * @param field Field to be set
	 * @param val <code>String</code> value to be set. Valid config preset tag from <code>automation</code> section is expected here.
	 */

	private void setAutomation(String field, def val)
	{
		setParam(field, val)
		paHelper = new PatternAutomationHelper(this) //refreshing PatternAutomation instance
	}

	/**
	 * Sets <code>FILTER_PATTERN</code> according to on supplied <code>tag</code> from <code>filters</code> section of config.xml. If pattern automation.
	 * @param field Field to be set
	 * @param val <code>String</code> value to be set. Valid config preset tag from <code>automation</code> section is expected here.
	 */
	private void setPredefinedFilter(String field, def val)
	{
		filterParser.unsubscribe()
		setParam(field, val)
		paHelper.parseFilterConfig(val)
	}

	/**
	 * Sets <code>FILTER_PATTERN</code> according to on supplied <code>tag</code> from <code>filters</code> section of config.xml. If pattern automation.
	 * @param field Field to be set
	 * @param val <code>String</code> value to be set. Valid config preset tag from <code>automation</code> section is expected here.
	 */
	private void setPredefinedBulkFilter(String field, def val)
	{
		filterParser.unsubscribe()
		setParam(field, val)
		paHelper.parseBulkFilterConfig(val)
	}

	/**
	 * Sets <code>FILTER_PATTERN</code> according to on supplied <code>tag</code> from <code>filters</code> section of config.xml. If pattern automation.
	 * @param field Field to be set
	 * @param val <code>String</code> value to be set. Valid config preset tag from <code>automation</code> section is expected here.
	 */
	private void setPredefinedConfig(String field, def val)
	{
		setParam(field, val)
		paHelper.applySequenceByTag(val)
		disableAutomation()
	}

	private void disableAutomation()
	{
		paHelper = null
	}


	/**
	 * Method prints out some help
	 * <p>
	 * Actually it has the same date as in groovydoc.
	 */
	private void printHelp(String field, def arg)
	{
		unsubscribeVarParsers(varParsers); //unsubscribing all
		def help = """\
CLI program to analyze text files in a regex manner. Adding a feature of a log record splitting, thread-coupling and reporting.

Usage: 
java -cp wgrep.jar org.smlt.tools.wgrep.WGrep CONFIG_FILE [-[:option:]] [--:filter_option:] [-L LOG_ENTRY_PATTERN] [FILTER_PATTERN] [--dtime FROM_DATE TO_DATE] FILENAME [FILENAME]
Usage via supplied .bat or .sh file: 
wgrep [-[:option:]] [--:filter_option:] [-L LOG_ENTRY_PATTERN] [FILTER_PATTERN] [--dtime FROM_DATE TO_DATE] FILENAME [FILENAME]

Examples:

Using in Windows
wgrep -s \"SomethingINeedToFind\" \"D:\\myfolder\\LOGS\\myapp\\node*.log*\"
wrep -s \"SomethingINeedToFind\" D:\\myfolder\\LOGS\\myapp\\node*.log

Using on NIX 
wgrep --my_predefined_config --dtime 2011-11-11T11:10 2011-11-11T11:11 myapp.log 
wgrep --my_predefined_config myapp.log 
wgrep 'SomethingINeedToFind' myanotherapp.log 
wgrep -s 'RecordShouldContainThis%and%ShouldContainThisAsWell' --dtime 2012-12-12T12 2012-12-12T12:12 thirdapp.log 
wgrep 'RecordShouldContainThis%and%ShouldContainThisAsWell%or%ItCouldContainThis%and%This' --dtime 2009-09-09T09:00 + thirdapp.log 
wgrep -s 'SimplyContainsThis' onemoreapp.log1 onemoreapp.log2 onemoreapp.log3 
"""
		println help
	}

	void enforceTrace(String field, def val)
	{
		log.debug("Enabling trace")
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
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
		try {
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(context);
			// Call context.reset() to clear any previous configuration, e.g. default
			// configuration. For multi-step configuration, omit calling context.reset().
			context.reset();
			configurator.doConfigure(new ByteArrayInputStream(traceConfig.getBytes()));
		} catch (JoranException je) {
			// StatusPrinter will handle this
		}
		StatusPrinter.printInCaseOfErrorsOrWarnings(context);
	}

	void enforceInfo(String field, def val)
	{
		log.debug("Redirecting info to STDOUT")
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		String traceConfig ="""\
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
		try {
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(context);
			// Call context.reset() to clear any previous configuration, e.g. default
			// configuration. For multi-step configuration, omit calling context.reset().
			context.reset();
			configurator.doConfigure(new ByteArrayInputStream(traceConfig.getBytes()));
		} catch (JoranException je) {
			// StatusPrinter will handle this
		}
		StatusPrinter.printInCaseOfErrorsOrWarnings(context);
	}

	boolean refreshConfigByFileName(String fileName)
	{
		if ( paHelper != null)
		{
			return paHelper.applySequenceByFileName(fileName)
		}
		return false
	}

}

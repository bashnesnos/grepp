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
public class CLIFacade {
	
	protected ConfigHolder config;

	//OPTIONS
	protected FilterParser filterParser =  null
	protected FileNameParser fileNameParser =  null
	protected Deque<ParamParser> varParsers = new ArrayDeque<ParamParser>();
	
	String currentConfigPtrn
	String currentConfigId
	boolean isAmmended = false

	protected File curWorkDir //allows to restrict access to a supplied working dir only

	public CLIFacade(ConfigHolder config) {
		this.config = config
	}
	
	/**
	 *  Method loads defaults and spooling extension as configured in config.xml's <code>global</code> section.
	 *  Loads some values set via System properties as well.
	 */
	protected void loadDefaults()
	{
		if (curWorkDir != null) {
			config.runtime.cwd = curWorkDir
		}
		
		def systemSep = System.getProperty("file.separator")
		config.runtime.home = System.getProperty("grepp.home") + systemSep
		if ("\\".equals(systemSep)) {
			systemSep += "\\"
		}
		config.runtime.folderSeparator = systemSep
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

	public void processCliArgs(List<String> args)
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
grepp -s --dtime 2012-12-12T12;2012-12-12T12:12 'RecordShouldContainThis%and%ShouldContainThisAsWell' thirdapp.log 
grepp --dtime 2009-09-09T09:00;+ 'RecordShouldContainThis%and%ShouldContainThisAsWell%or%ItCouldContainThis%and%This' thirdapp.log 
grepp -s 'SimplyContainsThis' onemoreapp.log1 onemoreapp.log2 onemoreapp.log3 
cat blabla.txt | grepp -L Chapter 'Once upon a time' > myfavoritechapter.txt
""")
        cli.v("Enforce info to stdout")
        cli.t("Enforce trace to stdout")
        cli.s("Toggles spooling to configured results dir and with configured spooling extension")
        cli.m("Toggles non-stop file traversing")
        cli.h("Print this message")
        cli.L(args:1, argName:"entry_regex", "Tells grepp to split the input in blocks, treating <entry_regex> as a start of the next block (so it's a block end at the same time).\n<entry_regex> - a string which will be used to \"split\" the input. Is optinal, as by default it will be looked up by the filename in config. Anyway, if not found input would be processed by line.")
        cli.p(longOpt:"parse", "Toggles logging .properties file to grepp config parsing")
        cli.e("Toggles thread ID preserving, i.e. all the records for a thread will be fetched")
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
		
        FilterChain<List<File>> fileFilterChain = new FilterChain<List<File>>()
        fileFilterChain.add(new FileSortFilter())
        fileFilterChain.disableFilter(FileSortFilter.class)
        fileFilterChain.disableFilter(LogEntryFilter.class)
        fileFilterChain.disableFilter(PropertiesFilter.class)
        fileFilterChain.disableFilter(ThreadFilter.class)
        fileFilterChain.disableFilter(SimpleFilter.class)
        fileFilterChain.disableFilter(EntryDateFilter.class)
        fileFilterChain.disableFilter(PostFilter.class)

        FilterChain<String> entryFilterChain = new FilterChain<String>()
        entryFilterChain.disableFilter(FileSortFilter.class)
        entryFilterChain.disableFilter(FileDateFilter.class)

		filterParser = new FilterParser()
		fileNameParser = new FileNameParser()
		varParsers.addAll([filterParser, fileNameParser])

		if (options.L) {
			//params.set(Param.USER_ENTRY_PATTERN, true)
			//params.set(Param.LOG_ENTRY_PATTERN, options.L)
			def logEntryFilter = new LogEntryFilter(options.L)
			logEntryFilter.lock()
			entryFilterChain.add(logEntryFilter)
		}

		if (options.p) {
			varParsers.remove(filterParser)
			entryFilterChain.disableFilter(ThreadFilter.class)
			entryFilterChain.disableFilter(SimpleFilter.class)
			entryFilterChain.disableFilter(EntryDateFilter.class)
			entryFilterChain.disableFilter(PostFilter.class)
		}
		else {
			entryFilterChain.disableFilter(PropetiesFilter.class)
		}

		if (options.e) {
			entryFilterChain.disableFilter(SimpleFilter.class)
		}
		else {
			entryFilterChain.disableFilter(ThreadFilter.class)
		}

		if (options.d) {
			//params.set(Param.DATE_TIME_FILTER, true)
			def dtimeParser = new DateTimeParser()
			log.trace('Got date options: {}', options.ds)
			options.ds.each {
				dtimeParser.parseVar(config, it)
			}

			entryFilterChain.add(new EntryDateFilter(null, null, config.runtime.dateFilter.from, config.runtime.dateFilter.to)) //postpone file-specific filter creation
			fileFilterChain.add(new FileDateFilter(config.runtime.dateFilter.from, config.runtime.dateFilter.to, null)) //postpone file-specific filter creation
		}

		loadDefaults()

		for (arg in options.arguments()) {
			log.debug("next arg: {}", arg);
			if (!this.processOptions(arg)) {
				ParamParser<?> paramParser = varParsers.pop() 
				if (!paramParser.parseVar(config, arg)) { //pushing back since this parser has more to parse
					varParsers.push(paramParser)
				}
			} 
		}

		if (config.runtime.containsKey('filterPattern')) {
			if (options.e) {
				entryFilterChain.add(new SimpleFilter(config.runtime.filterPattern))
			}
			else {
				entryFilterChain.add(new ThreadFilter(config.runtime.filterPattern, null, null, null))	
			}
		}

		PrintWriter printer = null
		GreppOutput output = null
		if (options.p) {
			log.info("Creating config output")
			output = new ConfigOutput(configHolder, entryFilterChain)
		}
		else if (options.s) {
			log.info("Creating file output")
			printer = getFilePrinter(configHolder)
			output new SimpleOutput<String>(configHolder, entryFilterChain, printer)
		}
		else {
			log.info("Creating console output")
			printer = getConsolePrinter()
			output new SimpleOutput<String>(configHolder, entryFilterChain, printer)
		}

		DataProcessor processor = null
		if (config.runtime.data.containsKey('files')) {
			processor = new TextFileProcessor(output, fileFilterChain, options.m != null)
			processor.process(config.runtime.data.files)
		}
		else {
			processor = new InputStreamProcessor(output)
			processor.process(System.in)
		}
	}

	/**
	 * Method for flags and options parsing. It identifies if passed help flag, simple flag or option, and calls appropriate method. Also it removes special symbols - and -- before passing argument further.
	 *
	 * @param arg An argument to be parsed
	 * @return <code>1</code> if <code>arg</code> was processed(i.e. it was a valid arg) <code>0</code> otherwise.
	 */

	protected boolean processOptions(String arg)
	{
		if (arg =~/^-(?![-0-9])/) //option should be a char and not a number
		{
			this.processSimpleArg(arg.substring(1)) //skipping '-' itself
			return true
		}
		else if (arg =~ /^--/)
		{
			this.processComlpexArg(arg.substring(2)) //skipping '--' itself
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
	protected void processSimpleArg(String arg)
	{
		throw new IllegalArgumentException("Invalid flag: " + arg)
	}

	/**
	 * Method for complex flags/options. There is no complex logic at the moment.
	 * It fetches every character from string and passes the result to {@link processOption} method.
	 *
	 * @param arg An argument to be parsed
	 */

	protected void processComlpexArg(String arg)
	{
		(arg =~ /.*/).each{opt -> if(opt) this.processOption(opt)}
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
		loadParamsById(opt)
	}

	protected void loadParamsById(String id) {
		boolean matchedAny = false
		//applying all that matches, i.e. greedy

		if (checkIfConfigExsits(id)) { //checking if  there exists a config with such id and applying it if so
			log.debug("Applying savedConfig for {}", id)
			matchedAny = true
			setPredefinedConfig(id)
		}

		if (checkIfFilterExsits(id)) { //checking filter wasn't supplied explicitly and there exists a filter with such id and applying it if so
			log.debug("Applying filterAlias for {}", id)
			matchedAny = true
			setPredefinedFilter(id)
			varParsers.remove(filterParser)			
		}

		if (checkIfExecuteThreadExsits(id)) { //checking if there exists a thread preserving patterns with such id and applying it if so
			log.debug("Applying processThread for {}", id)
			matchedAny = true
			setThreadPreserving(id)
		}

		if (checkIfPostProcessExsits(id)) { //checking if there exists a post_processing config with such id and applying it if so
			log.debug("Applying postProcessColumns for {}", id)
			matchedAny = true
			setPostProcessing(id)
		}

		if (id != null && !matchedAny) {
			throw new IllegalArgumentException("Invalid id, doesn't match any pre-configured: " + id)
		}
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
	protected void setPredefinedConfig(def val)
	{

	}


	/**
	 * Sets <code>FILTER_PATTERN</code> according to on supplied <code>tag</code> from <code>filters</code> section of config.xml. Requires pattern automation to operate. <br>
	 * Calls {@link PatternAutomation.parseFilterConfig}
	 * @param field Field to be set
	 * @param val <code>String</code> value to be set. Valid config preset tag from <code>automation</code> section is expected here.
	 */
	protected void setPredefinedFilter(def val)
	{

	}


	/**
	 * Sets <code>FILTER_PATTERN</code> according to on supplied <code>tag</code> from <code>filters</code> section of config.xml. Requires pattern automation to operate. <br>
	 * Calls {@link PatternAutomation.parseFilterConfig}
	 * @param field Field to be set
	 * @param val <code>String</code> value to be set. Valid config preset tag from <code>automation</code> section is expected here.
	 */
	protected void setThreadPreserving(def val)
	{

	}


	/**
	 * Enables post processing.
	 * @param field Field to be set
	 * @param val <code>String</code> value to be set. Valid config preset tag from <code>pp_splitters</code> section is expected here.
	 */

	protected void setPostProcessing(def val)
	{

	}


	public static PrintWriter getConsolePrinter() {
		def console = System.console()
		if (console != null) {
			return console.writer()
		}
		else {
			log.debug("There is no associated console to use with this output! Defaulting to System.out.");
			return new PrintWriter(System.out, true)
		}
	}
	
	public static PrintWriter getFilePrinter(ConfigHolder config) {
		def outputDir = new File(new File(config.runtime.home), config.defaults.resultsDir)
		if (!outputDir.exists()) outputDir.mkdir()
		def out_file = new File(outputDir, paramsHolder.getSpoolFileName())
		log.trace("Creating new file: {}", out_file.getCanonicalPath())
		out_file.createNewFile()
		return new PrintWriter(new FileWriter(out_file), true) //autoflushing PrintWriter
	}

}

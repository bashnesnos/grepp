package org.smltools.grepp.config

import groovy.util.logging.Slf4j
import groovy.util.ConfigObject
import groovy.util.OptionAccessor
import org.smltools.grepp.config.varparsers.*
import org.smltools.grepp.filters.FilterChain
import org.smltools.grepp.filters.entry.EntryDateFilter
import org.smltools.grepp.filters.entry.LogEntryFilter
import org.smltools.grepp.filters.entry.SimpleFilter
import org.smltools.grepp.filters.entry.ThreadFilter
import org.smltools.grepp.filters.entry.PropertiesFilter
import org.smltools.grepp.filters.entry.PostFilter
import org.smltools.grepp.util.GreppUtil
import org.smltools.grepp.filters.enums.*
import org.smltools.grepp.filters.logfile.FileDateFilter
import org.smltools.grepp.filters.logfile.FileSortFilter
import org.smltools.grepp.output.ConfigOutput
import org.smltools.grepp.output.GreppOutput
import org.smltools.grepp.output.SimpleOutput
import org.smltools.grepp.processors.DataProcessor
import org.smltools.grepp.processors.InputStreamProcessor
import org.smltools.grepp.processors.TextFileProcessor

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
	protected File curWorkDir //allows to restrict access to a supplied working dir only
        
	public CLIFacade(ConfigHolder config) {
		this.config = config
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

	public OptionAccessor parseOptions(String[] args)
	{
		if (args == null || args.length == 0) throw new IllegalArgumentException("Invalid arguments")
		
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
grepp -l \"RecordStart\" \"SomethingINeedToFind\" D:\\myfolder\\LOGS\\myapp\\node*.log*
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
        cli.l(args:1, argName:"entry_regex", "Tells grepp to split the input in blocks, treating <entry_regex> as a start of the next block (so it's a block end at the same time).\n<entry_regex> - a string which will be used to \"split\" the input. Is optinal, as by default it will be looked up by the filename in config. Anyway, if not found input would be processed by line.")
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
        	println "Press any key to exit"
        	System.in.read()
        	System.exit(0)
        }
        
        if (options.v) {
        	enforceInfo()
        }
        else if (options.t) {
        	enforceTrace()
        }

        return options
    }

	/**
	 *  Method loads defaults and spooling extension as configured in config.xml's <code>global</code> section.
	 *  Loads some values set via System properties as well.
	 */
	public ConfigObject makeRuntimeConfig() {

        ConfigObject runtimeConfig = new ConfigObject()
        runtimeConfig.putAll(config)

        runtimeConfig.runtime.spoolFileExtension = config.defaults.spoolFileExtension
        runtimeConfig.runtime.resultsDir = config.defaults.resultsDir
                
		if (curWorkDir != null) {
			runtimeConfig.runtime.cwd = curWorkDir
		}
		
		def systemSep = System.getProperty("file.separator")
		runtimeConfig.runtime.home = System.getProperty("grepp.home") + systemSep
		if ("\\".equals(systemSep)) {
			systemSep += "\\"
		}
		runtimeConfig.runtime.folderSeparator = systemSep

		return runtimeConfig
	}


	public FilterChain makeFileFilterChain(ConfigObject runtimeConfig, OptionAccessor options) {
        FilterChain<List<File>> fileFilterChain = new FilterChain<List<File>>()
        fileFilterChain.add(new FileSortFilter())

		if (options.d) {
			if (runtimeConfig.runtime.containskey('dateFilter')) {
				fileFilterChain.add(new FileDateFilter(runtimeConfig.runtime.dateFilter.from, runtimeConfig.runtime.dateFilter.to, null)) //postpone file-specific filter creation
			}
			else {
				log.debug("Runtime config: {}; options: {}", runtimeConfig.flatten(), options)
				throw new IllegalStateException("fileFilterChain init attempt before the entryFilterChain was initialized; or dateFilter wasn't proper")
			}
		}


		return fileFilterChain
	}

    public FilterChain makeEntryFilterChain(ConfigObject runtimeConfig, OptionAccessor options) {
        FilterChain<String> entryFilterChain = new FilterChain<String>()
        entryFilterChain.enableFilter(LogEntryFilter.class)

		Deque<ParamParser> varParsers = new ArrayDeque<ParamParser>();

		FilterParser filterParser = new FilterParser()
		FileNameParser fileNameParser = new FileNameParser()
		varParsers.addAll([filterParser, fileNameParser])

		if (options.l) {
			def logEntryFilter = new LogEntryFilter(options.l)
			logEntryFilter.lock()
			entryFilterChain.add(logEntryFilter)
		}

		if (options.p) {
			varParsers.remove(filterParser)
			entryFilterChain.enableFilter(PropetiesFilter.class)
		}
		else {
			entryFilterChain.enableFilter(EntryDateFilter.class)
			entryFilterChain.enableFilter(PostFilter.class)
			entryFilterChain.enableFilter(SimpleFilter.class)
		}

		if (options.e) {
			entryFilterChain.enableFilter(ThreadFilter.class)			
		}

		if (options.d) {
			def dtimeParser = new DateTimeParser()
			log.trace('Got date options: {}', options.ds)
			options.ds.each {
				dtimeParser.parseVar(runtimeConfig, it)
			}

			entryFilterChain.add(new EntryDateFilter(null, null, runtimeConfig.runtime.dateFilter.from, runtimeConfig.runtime.dateFilter.to)) //postpone file-specific filter creation
		}

		for (arg in options.arguments()) {
			log.debug("next arg: {}", arg);
			if (!this.processOptions(entryFilterChain, arg)) {
				ParamParser<?> paramParser = varParsers.pop() 
				if (!paramParser.parseVar(runtimeConfig, arg)) { //pushing back since this parser has more to parse
					varParsers.push(paramParser)
				}
			} 
		}

		if (runtimeConfig.runtime.containsKey('filterPattern')) {
			if (options.e) {
				entryFilterChain.add(new SimpleFilter(runtimeConfig.runtime.filterPattern))
			}
			else {
				entryFilterChain.add(new ThreadFilter(runtimeConfig.runtime.filterPattern, null, null, null))	
			}
		}

		return entryFilterChain
	}

	public GreppOutput makeOutput(ConfigObject runtimeConfig, FilterChain entryFilterChain) {
		PrintWriter printer = null
		GreppOutput output = null
		if (options.p) {
			log.info("Creating config output")
			output = new ConfigOutput(config, entryFilterChain)
		}
		else if (options.s) {
			log.info("Creating file output")
			printer = getFilePrinter(runtimeConfig)
			output = new SimpleOutput<String>(config, entryFilterChain, printer)
		}
		else {
			log.info("Creating console output")
			printer = getConsolePrinter()
			output = new SimpleOutput<String>(config, entryFilterChain, printer)
		}
		return output
	}

	public DataProcessor makeProcessor(ConfigObject runtimeConfig, FilterChain fileFilterChain) {
		DataProcessor processor = null
		if (runtimeConfig.runtime.data.containsKey('files')) {
			processor = new TextFileProcessor(output, fileFilterChain, options.m != null)
			runtimeConfig.runtime.data = runtimeConfig.runtime.data.files
			
		}
		else {
			processor = new InputStreamProcessor(output)
			runtimeConfig.runtime.data = System.in
		}		
		return processor
	}

	public void process(String[] args) {
		def options = parseOptions(args)
		def runtimeConfig = makeRuntimeConfig()
		def entryFilterChain = makeEntryFilterChain(runtimeConfig, options)
		def fileFilterChain = makeFileFilterChain(runtimeConfig, options)
		def output = makeOutput(runtimeConfig, entryFilterChain)
		def processor = makeProcessor(runtimeConfig, fileFilterChain)
		processor.process(runtimeConfig.runtime.data)
	}

	/**
	 * Method for flags and options parsing. It identifies if passed help flag, simple flag or option, and calls appropriate method. Also it removes special symbols - and -- before passing argument further.
	 *
	 * @param arg An argument to be parsed
	 * @return <code>1</code> if <code>arg</code> was processed(i.e. it was a valid arg) <code>0</code> otherwise.
	 */

	protected boolean processOptions(FilterChain filterChain, String arg)
	{
		if (arg =~/^-(?![-0-9])/) //option should be a char and not a number
		{
			this.processSimpleArg(filterChain, arg.substring(1)) //skipping '-' itself
			return true
		}
		else if (arg =~ /^--/)
		{
			this.processComlpexArg(filterChain, arg.substring(2)) //skipping '--' itself
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
	protected void processSimpleArg(FilterChain filterChain, String arg)
	{
		throw new IllegalArgumentException("Invalid flag: " + arg)
	}

	/**
	 * Method for complex flags/options. There is no complex logic at the moment.
	 * It fetches every character from string and passes the result to {@link processOption} method.
	 *
	 * @param arg An argument to be parsed
	 */

	protected void processComlpexArg(FilterChain filterChain, String arg)
	{
		(arg =~ /.*/).each{opt -> if(opt) this.loadParamsById(filterChain, opt)}
	}

	/**
	 * Method which performs actual option lookup in the config
	 * <p>
	 * It fetches handler function (from <code>handler</code> attribute), and calls it from {@link WgrepFacade} class.
	 * <p> It passes to the handler function <code>field</code> attribute and value of matching option from config.xml.
	 *
	 * @param opt An argument to be looked up
	 * @throws IllegalArgumentException If the supplied <code>arg</code> is not configured, i.e. cannot be found in the config.xml.
	 */
	protected void loadParamsById(FilterChain filterChain, String id) {
                if (id != null && !filterChain.addByConfigAndConfigId(config, id)) {
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
	
	public static PrintWriter getFilePrinter(ConfigObject runtimeConfig) {
		def outputDir = new File(new File(runtimeConfig.runtime.home), runtimeConfig.runtime.resultsDir)
		if (!outputDir.exists()) outputDir.mkdir()
		def out_file = new File(outputDir, runtimeConfig.runtime.spoolExtension)
		log.trace("Creating new file: {}", out_file.getCanonicalPath())
		out_file.createNewFile()
		return new PrintWriter(new FileWriter(out_file), true) //autoflushing PrintWriter
	}

}

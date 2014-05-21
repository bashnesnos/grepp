package org.smltools.grepp.cli

import groovy.util.logging.Slf4j
import groovy.util.ConfigObject
import groovy.util.OptionAccessor
import org.smltools.grepp.cli.varparsers.*
import org.smltools.grepp.config.ConfigHolder
import org.smltools.grepp.filters.FilterChain
import org.smltools.grepp.filters.StringAggregator
import org.smltools.grepp.filters.entry.EntryDateFilter
import org.smltools.grepp.filters.entry.LogEntryFilter
import org.smltools.grepp.filters.entry.SimpleFilter
import org.smltools.grepp.filters.entry.ThreadLogEntryFilter
import org.smltools.grepp.filters.entry.PropertiesFilter
import org.smltools.grepp.filters.entry.ReportFilter
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
import static org.smltools.grepp.Constants.*

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


	public CliBuilder getCliBuilder() {
		def cli = new CliBuilder(usage:"grepp [options] [extra_options|filter_regex] [filename [filename]]"
            , width: 100
            , header:"Options:"
            , footer: """
===========================
Extra options:
${FilterChain.getConfigIdToFilterClassMap(config).collect { configId, classes -> 
	String.format("--%-20s\t\t\tmay configure: %s", configId, classes.collect { it.simpleName }.join(','))
}.join('\n')}            
===========================
Parameters:
filter_regex     - a string to find in the input. Is optional if any of the supplied <extra_options> configures it
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
grepp --my_predefined_config -d 2011-11-11T11:10;2011-11-11T11:11 myapp.log 
grepp --my_predefined_config -d 2011-11-11T11:10;-10 myapp.log 
grepp --my_predefined_regex_id myapp.log 
grepp 'SomethingINeedToFind' myanotherapp.log 
grepp -s -d 2012-12-12T12;2012-12-12T12:12 'RecordShouldContainThis%and%ShouldContainThisAsWell' thirdapp.log 
grepp -d 2009-09-09T09:00;+ 'RecordShouldContainThis%and%ShouldContainThisAsWell%or%ItCouldContainThis%and%This' thirdapp.log 
grepp -s 'SimplyContainsThis' onemoreapp.log1 onemoreapp.log2 onemoreapp.log3 
cat blabla.txt | grepp -l Chapter 'Once upon a time' > myfavoritechapter.txt
""")
        cli.v("Enforce info to stdout")
        cli.t("Enforce trace to stdout")
        cli.s("Toggles spooling to configured results dir and with configured spooling extension")
        cli.m("Toggles non-stop file traversing")
        cli.h("Print this message")
        cli.l(args:1, argName:"entry_regex", "Tells grepp to split the input in blocks, treating <entry_regex> as a start of the next block (so it's a block end at the same time).\n<entry_regex> - a string which will be used to \"split\" the input. Is optinal, as by default it will be looked up by the filename in config. Anyway, if not found input would be processed by line.")
        cli.p(longOpt:"parse", "Toggles logging .properties file to grepp config parsing")
        cli.e("Toggles thread ID preserving, i.e. all the records for a thread will be fetched")
        cli.d(args:2, valueSeparator:";", argName:"from;to", """Tells grepp to include files/log entries within the supplied timeframe.
            <from to> - string representing date constraints for current search. 
                        Default format is yyyy-mm-ddTHH:MM:ss (could be reduced till yyyy). If <from> or <to> is not known (or is indefinite) '+' can be passed as argument.
                        Date's could be constructed by an offset from NOW or from supplied date. I.e. -d -10;+ will mean 'searching period is last 10 minutes'.
                        E.g. -d 2013-05-01T12:00;-20, -d 2013-05-01T12:00;+20
                        If <from> is after <to> they will be swapped automatically.
                        Usage requires valid date pattern to be configured for such a file in config. Otherwise it won't be applied
""")
        cli.add(args:1, argName:"configId", "Instructs to save given configuraion as a config. <configId> should be unique")
        cli.dateProp(args:2, valueSeparator:";", argName:"format;regex", "Loads date entry filter with <format> (SimpleDateFormat compliant) and <regex> to extract the date from entries")
        cli.threadProp(args:3, valueSeparator:";", argName:"start;skipend;end", "Loads thread filter with <start>, <skipend> (leave as blank if not needed) and <end> regexes")
        cli.repProp(args:1, argName:"type(regex,colName);...", "Loads report filter with <type(regex,colName)> in the given order. Type should be equal to one of the post filter methods. Separate with ';' if multiple columns. You need to escape ',' and ';' with \\ in the <regex> part for correct processing")
        cli.lock("Locks the filter chains after full initialization. I.e. it means if any file processed won't update filter params even if such are configured for it")
        cli.noff("No File Filtering - i.e. turns off file filtering based on date etc.")		
        cli.norx("No RegeX - i.e. treats regex special symbols as usual chars. Extended %or%, %and% etc. are still available though")
        cli.nohd("No HeaDer - i.e. forces header ommitting for a report filter")

        return cli
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
		
		def cli = getCliBuilder()

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
        runtimeConfig.spoolFileExtension = config.defaults.spoolFileExtension
        runtimeConfig.resultsDir = config.defaults.resultsDir
		runtimeConfig.spoolFileName = String.format("result_%tY%<tm%<td_%<tH%<tM%<tS", new Date())
		
		if (curWorkDir != null) {
			runtimeConfig.cwd = curWorkDir
		}
		
		def systemSep = System.getProperty("file.separator")
		runtimeConfig.home = System.getProperty(GREPP_HOME_SYSTEM_OPTION) + systemSep
		if ("\\".equals(systemSep)) {
			systemSep += "\\"
		}
		runtimeConfig.folderSeparator = systemSep

		return runtimeConfig
	}


    public ConfigObject makeFilterChains(ConfigObject runtimeConfig, OptionAccessor options) {
        FilterChain<String> entryFilterChain = new FilterChain<String>(config, new StringAggregator(), String.class)
		Deque<ParamParser> varParsers = new ArrayDeque<ParamParser>();

        FilterChain<List<File>> fileFilterChain = new FilterChain<List<File>>(config, new StringAggregator(), new ArrayList<File>().class)
        fileFilterChain.add(fileFilterChain.getInstance(FileSortFilter.class))

		FilterParser filterParser = new FilterParser()
		FileNameParser fileNameParser = new FileNameParser()
		varParsers.addAll([filterParser, fileNameParser])
		def logEntryFilter

		if (options.e || options.threadProp) {
			entryFilterChain.enableFilter(ThreadLogEntryFilter.class)
			if (options.threadProp) {
				logEntryFilter = entryFilterChain.getInstance(LogEntryFilter.class)
				logEntryFilter.setThreadExtractorList(options.threadProps[0].size() > 0 ? [options.threadProps[0]] : null)
				logEntryFilter.setThreadSkipEndPatternList(options.threadProps[1].size() > 0 ? [options.threadProps[1]] : null)
				logEntryFilter.setThreadEndPatternList(options.threadProps[2].size() > 0 ? [options.threadProps[2]] : null)
				entryFilterChain.add(logEntryFilter)
			}
		}
		else {
			entryFilterChain.disableFilter(ThreadLogEntryFilter.class)
			entryFilterChain.enableFilter(LogEntryFilter.class)
		}

		if (options.l) {
			if (logEntryFilter == null) {
				logEntryFilter = entryFilterChain.getInstance(LogEntryFilter.class)
				entryFilterChain.add(logEntryFilter)
			}
			def starterNTerminator = options.l.split(';')
			logEntryFilter.setStarter(starterNTerminator[0])
			if (starterNTerminator.length > 1) {
				logEntryFilter.setLogEntryTerminatorPattern(starterNTerminator[1])
			}
		}

		if (options.p) {
			varParsers.remove(filterParser)
			fileFilterChain.disableFilter(FileDateFilter.class)
			entryFilterChain.add(entryFilterChain.getInstance(PropertiesFilter.class))
			entryFilterChain.disableFilter(ReportFilter.class)
			entryFilterChain.disableFilter(SimpleFilter.class)
			entryFilterChain.disableFilter(ThreadLogEntryFilter.class) //force disabling this
			entryFilterChain.disableFilter(EntryDateFilter.class)

			entryFilterChain.enableFilter(LogEntryFilter.class) //may be disable this if properties are going to be something else but Filter
		}
		else {
			entryFilterChain.disableFilter(PropertiesFilter.class)
		}

		if (options.repProp) {
			def reportFilter = entryFilterChain.getInstance(ReportFilter.class)
			// default aggregator would be set by default			
			reportFilter.setAggregatorById(config.defaults.report.aggregator)

			options.repProp.split(/(?<!\\);/).each { prop ->
				def mtchr = prop =~ /(\w+?)\((.*)\)/
				if (mtchr.matches()) {
				    def type = mtchr.group(1)
				    def regexAndColName = mtchr.group(2).split(/(?<!\\),/)
				    reportFilter.addReportMethodByType(type, regexAndColName[0], (regexAndColName.length > 1) ? regexAndColName[1] : null)
				}
				else {
					mtchr = prop =~ /agg=(.*)/
					if (mtchr.matches()) {
						def agg = mtchr.group(1)
						reportFilter.setAggregatorById(agg)
					}
					else {
						LOGGER.warn("repProp $prop matched neither method type declaration nor aggregator type declaration")
					}
				}
			}			

			reportFilter.setPrintHeader(config.defaults.report.printHeader)

			reportFilter.lock()
			entryFilterChain.add(reportFilter)
		}

		if (options.dateProp && !options.d) {
			println "dateProp option is ignored if the d option is not supplied"
		}

		if (options.d) {
			def dtimeParser = new DateTimeParser()
			log.trace('Got date options: {}', options.ds)
			dtimeParser.parseVar(runtimeConfig, options.ds[0])
			dtimeParser.parseVar(runtimeConfig, options.ds[1])

			if (!options.noff) {
				def fileDateFilter = fileFilterChain.getInstance(FileDateFilter.class)
				fileDateFilter.setFrom(runtimeConfig.dateFilter.from)
				fileDateFilter.setTo(runtimeConfig.dateFilter.to)
				fileFilterChain.add(fileDateFilter)
			}

			def entryDateFilter = entryFilterChain.getInstance(EntryDateFilter.class)
			entryDateFilter.setFrom(runtimeConfig.dateFilter.from)
			entryDateFilter.setTo(runtimeConfig.dateFilter.to)

			if (options.dateProp) {
				entryDateFilter.setLogDateFormat(options.dateProps[0])
				entryDateFilter.setLogDatePattern(options.dateProps[1])
				entryDateFilter.lock()
				if (logEntryFilter == null) { //enabling if null; otherwise it's useless
					logEntryFilter = entryFilterChain.getInstance(LogEntryFilter.class)
					logEntryFilter.lock()
					entryFilterChain.add(logEntryFilter)
				}

				logEntryFilter.setDateRegex(options.dateProps[1])
			}

			entryFilterChain.add(entryDateFilter) //postpone file-specific filter creation
		}
		else {
			entryFilterChain.disableFilter(EntryDateFilter.class)
			fileFilterChain.disableFilter(FileDateFilter.class)
		}

		for (arg in options.arguments()) {
			log.debug("next arg: {}", arg);

			if (arg =~/^-(?![-0-9])/) //such flags should be processed by CliBuilder in parseOptions()
			{
				throw new IllegalArgumentException("Invalid flag: " + arg)
			}

			if (!processConfigId([entryFilterChain, fileFilterChain], arg)) {
				ParamParser<?> paramParser = varParsers.pop()
				if (paramParser instanceof FilterParser) {
					if (entryFilterChain.has(SimpleFilter.class) || !entryFilterChain.isEnabled(SimpleFilter.class)) {
						paramParser = varParsers.pop() //i.e. skipping filterParser
					}
				}
				if (!paramParser.parseVar(runtimeConfig, arg)) { //pushing back since this parser has more to parse
					varParsers.push(paramParser)
				}
			} 
		}

		if (entryFilterChain.has(EntryDateFilter.class) && !entryFilterChain.has(LogEntryFilter.class)) { //anyway we're lookin for dates here
			def dateRegex = entryFilterChain.get(EntryDateFilter.class).getLogDatePattern()
			if (dateRegex != null) { //if it's not yet initialized then we're not adding entryFilter
				logEntryFilter = entryFilterChain.getInstance(LogEntryFilter.class)
				logEntryFilter.setDateRegex(dateRegex)
				entryFilterChain.add(logEntryFilter)
			}
		}

		if (runtimeConfig.containsKey('filterPattern')) {
			def mainFilter = entryFilterChain.getInstance(SimpleFilter.class)
			mainFilter.setFilterPattern(runtimeConfig.filterPattern, options.norx)
			entryFilterChain.add(mainFilter)
		}

		if (options.lock) {
			log.trace("Locking filter chains")
			entryFilterChain.lock()
			fileFilterChain.lock()	
		}

		if (entryFilterChain.has(ReportFilter.class)) {
			def reportFilter = entryFilterChain.get(ReportFilter.class)
			runtimeConfig.spoolFileExtension = reportFilter.getSpoolFileExtension()

			if (options.nohd) {
				reportFilter.setPrintHeader(false)
			}
		}

		runtimeConfig.entryFilterChain = entryFilterChain
		runtimeConfig.fileFilterChain = fileFilterChain

		return runtimeConfig
	}

	public GreppOutput makeOutput(ConfigObject runtimeConfig, FilterChain entryFilterChain, OptionAccessor options) {
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

	public DataProcessor makeProcessor(ConfigObject runtimeConfig, GreppOutput output, OptionAccessor options) {
		DataProcessor processor = null
		if (runtimeConfig.data.containsKey('files')) {
			processor = new TextFileProcessor(output, options.m)
			runtimeConfig.data = runtimeConfig.data.files
			
		}
		else {
			processor = new InputStreamProcessor(output)
			runtimeConfig.data = System.in
		}		
		return processor
	}

	public void process(String[] args) {
		def options = parseOptions(args)
		def runtimeConfig = makeRuntimeConfig()
		makeFilterChains(runtimeConfig, options)
		def entryFilterChain = runtimeConfig.entryFilterChain
		def fileFilterChain = runtimeConfig.fileFilterChain

		if (runtimeConfig.data.containsKey('files')) {
        	List<File> filteredData = fileFilterChain.filter(runtimeConfig.data.files)
			if (filteredData != null) {
				runtimeConfig.data.files = filteredData
			}
			else {
				return //nothing to process
			}
		}		

		if (options.add) {
			if (entryFilterChain.configIdExists(options.add) || fileFilterChain.configIdExists(options.add)) {
				println "ConfigId $options.add already exists for a given filter chain; try different one or remove the old one"
				return
			}
		}

		if (options.add) {
			log.info("Saving config to {}", options.add)
			config.merge(entryFilterChain.getAsConfig(options.add))
			config.merge(fileFilterChain.getAsConfig(options.add))
			config.save()
		}

		def output = makeOutput(runtimeConfig, entryFilterChain, options)
		def processor = makeProcessor(runtimeConfig, output, options)
		processor.process(runtimeConfig.data)
	}

	/**
	 * Method for flags and options parsing. It identifies if passed help flag, simple flag or option, and calls appropriate method. Also it removes special symbols - and -- before passing argument further.
	 *
	 * @param arg An argument to be parsed
	 * @return <code>1</code> if <code>arg</code> was processed(i.e. it was a valid arg) <code>0</code> otherwise.
	 */

	protected boolean processConfigId(List<FilterChain> filterChainList, String arg)	{
		if (arg =~ /^--/)
		{
			String wannaBeConfigId = arg.substring(2)
			if (wannaBeConfigId == null) throw new IllegalArgumentException("Invalid option: " + arg)			
			
			boolean isAConfigId = false
			filterChainList.each { filterChain ->
				isAConfigId |= filterChain.refreshByConfigId(wannaBeConfigId)
			}
			
			if (!isAConfigId) throw new IllegalArgumentException("Invalid configId, doesn't match any pre-configured: " + arg)
			return isAConfigId
		}
		else {
			return false //otherwise it's something else and we're letting somebody else to process it
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
		def outputDir = new File(runtimeConfig.home, runtimeConfig.resultsDir)
		if (!outputDir.exists()) outputDir.mkdir()
		def out_file = new File(outputDir, runtimeConfig.spoolFileName + "." + runtimeConfig.spoolExtension)
		log.trace("Creating new file: {}", out_file.getCanonicalPath())
		out_file.createNewFile()
		return new PrintWriter(new FileWriter(out_file), true) //autoflushing PrintWriter
	}

}

import org.smltools.grepp.filters.entry.*
import org.smltools.grepp.filters.logfile.*
import org.smltools.grepp.cli.Grepp
import org.smltools.grepp.cli.CLIFacade
import org.smltools.grepp.config.ConfigHolder
import org.smltools.grepp.util.GreppUtil
import java.net.URL
import groovy.xml.DOMBuilder
import groovy.xml.dom.DOMCategory
import groovy.util.GroovyTestCase
import java.text.SimpleDateFormat

class GreppTest extends GroovyTestCase {

	ConfigHolder config
	CLIFacade facade
	def HOME = System.getProperty('grepp.home')
	def GREPP_CONFIG = System.getProperty("grepp.config")

	void setUp() {
		config = new ConfigHolder(new URL('file', '/', GREPP_CONFIG))
		facade = new CLIFacade(config);
	}

	public static String getOutput(Closure operation) {
		def oldStdout = System.out
		def pipeOut = new PipedOutputStream()
		def pipeIn = new PipedInputStream(pipeOut)
		System.setOut(new PrintStream(pipeOut))

		try {
			operation.call()
		}
		catch (Exception e) {
			pipeOut.close()
			System.setOut(oldStdout)
			throw e
		}
		finally {
			System.setOut(oldStdout)
			pipeOut.close()
		}

		def outputReader = new BufferedReader(new InputStreamReader(pipeIn))

		StringBuffer actualResult = new StringBuffer()
		if (outputReader.ready()) {
			def line = outputReader.readLine()
			while (line != null) {
				actualResult.size() > 0 ? actualResult.append('\n').append(line) : actualResult.append(line)
				line = outputReader.readLine()
			}
		}
		return actualResult.toString()
	}

	public static void assertGreppOutput(String expectedResult, Closure operation) {
		println "ER: ####\n$expectedResult\n#### :ER"
		String actualResult = getOutput(operation)
		println "AR: ####\n$actualResult\n#### :AR"
		assertTrue("Output not matched", expectedResult.replace('\r\n', '\n') == actualResult)
	}

	public static def makeFilterChains(def facade, String arguments) {
		def options = facade.parseOptions(arguments.split())
		def runtimeConfig = facade.makeRuntimeConfig()
		return facade.makeFilterChains(runtimeConfig, options)
	}

//	void testGetOptions(){
//		config.getOptions()
//	}
	
	void testMainVarsProcessing() {
		def options = facade.parseOptions("-l test test $HOME\\fpTest*".split())
		assertTrue("User entry pattern option not recognized: " + options.l, "test".equals(options.l))
		def runtimeConfig = facade.makeRuntimeConfig()
		def entryFilterChain = facade.makeFilterChains(runtimeConfig, options).entryFilterChain
		def newConfig = entryFilterChain.getAsConfig("main")
		assertTrue("Filter pattern not recognized", "test".equals(newConfig.savedConfigs.main.starter))
		assertTrue("Files not recognized", runtimeConfig.data.files == [
			new File(HOME+"\\fpTest_test.log")]
		)
		assertTrue("Folder separator not initialized", runtimeConfig.folderSeparator != null )
	}
	
	void testConfigsProcessing() {
		def entryFilterChain = makeFilterChains(facade, "--to_test --predef $HOME\\fpTest*").entryFilterChain
		def newConfig = entryFilterChain.getAsConfig(null)
		assertTrue("Filter pattern not recognized", config.filterAliases.predef.equals(newConfig.filterAliases.predef))
		assertTrue("Should have LogEntryFilter", entryFilterChain.has(LogEntryFilter.class))
		assertTrue("Should have SimpleFilter", entryFilterChain.has(SimpleFilter.class))
	}

	void testExtendedPatternProcessing() {
		def entryFilterChain = makeFilterChains(facade, "-l test test%and%tets $HOME\\test*").entryFilterChain
		assertTrue("Should have LogEntryFilter", entryFilterChain.has(LogEntryFilter.class))
		assertTrue("Should have SimpleFilter", entryFilterChain.has(SimpleFilter.class))
	}

	void testComplexVarsProcessing() {
		def runtimeConfig = makeFilterChains(facade, "-l test -d 2013-01-25T12:00:00;+ test $HOME\\test*")
		def entryFilterChain = runtimeConfig.entryFilterChain
		assertTrue("Should have EntryDateFilter", entryFilterChain.has(EntryDateFilter.class))
		assertTrue("Should have LogEntryFilter", entryFilterChain.has(LogEntryFilter.class))
		assertTrue("Should have SimpleFilter", entryFilterChain.has(SimpleFilter.class))
		def fileFilterChain = runtimeConfig.fileFilterChain
		assertTrue("Should have FileDateFilter", fileFilterChain.has(FileDateFilter.class))
	}

	void testAutomationProcessing() {
		def runtimeConfig = makeFilterChains(facade, "test $HOME\\fpTest_*")
		def entryFilterChain = runtimeConfig.entryFilterChain
		entryFilterChain.refreshByConfigId(ConfigHolder.findConfigIdByFileName(config, runtimeConfig.data.files[0].name))
		assertTrue("Should have LogEntryFilter", entryFilterChain.has(LogEntryFilter.class))
		assertTrue("Should have SimpleFilter", entryFilterChain.has(SimpleFilter.class))
	}

	void testMoreComplexVarsProcessing() {
		def runtimeConfig = makeFilterChains(facade, "-s -l stCommand --count_ops cmd_only_1.log")
		def entryFilterChain = runtimeConfig.entryFilterChain
		assertTrue("Should have LogEntryFilter", entryFilterChain.has(LogEntryFilter.class))
		assertTrue("Should have SimpleFilter", entryFilterChain.has(SimpleFilter.class))
		assertTrue("Files not recognized", runtimeConfig.data.files.containsAll([new File("cmd_only_1.log")]))
		assertTrue("Separator wasn't identified", "\\\\".equals(runtimeConfig.folderSeparator))
	}

	void testPluginForReportFilterMethod() {
		def expectedResult = """\
some_cmd,count_of_operands
Foo,test
Koo,test
Foo,test"""

		assertGreppOutput(expectedResult) {
			Grepp.main("--test_ops oo $HOME\\processing_report_test.log".split(" "))
		}
	}

	void testPluginForFilter() {
		def expectedResult = """\
doodki!
doodki!
doodki!
doodki!"""		

		assertGreppOutput(expectedResult) {
			Grepp.main("--doodki oo $HOME\\processing_report_test.log".split(" "))
		}
	}

	void testOnTheFlyLockedConfig() {
		def expectedResult = """\
2000-01-01 10:05:56,951 [ACTIVE] ThreadStart: '15' 
Too early for main time tests

2000-01-01 10:05:56,952 [ACTIVE] ThreadStart: '10' 
Still too early for main time tests

2000-01-01 10:05:56,953 [ACTIVE] ThreadStart: '15'
The end

2000-01-01 10:05:56,955 [ACTIVE] ThreadStart: '10'
A bit more stuff
The end
"""
		assertGreppOutput(expectedResult) {
			Grepp.main((String[]) ["--lock", "--noff", "--add", "myconfig", "--threadProp", "ThreadStart: '\\d{1,2}';;The end"
				, "--dateProp", "yyyy-MM-dd;(\\d{4}-\\d{2}-\\d{2})", "-e", "-d", "+;2001"
				, "oo", "$HOME\\processing_time_test.log"])
		}

		assertGreppOutput(expectedResult) {
			Grepp.main("--lock --noff -e -d +;2001 --myconfig $HOME\\processing_time_test.log".split(" "))
		}
	}

	void testOnTheFlyReport() {
		def expectedResult = """\
some_cmd,count_of_operands
Foo,3
Koo,1
Foo,1"""

		assertGreppOutput(expectedResult) {
			Grepp.main("--add new_report --repProp filter(name=\"?(.*?)\",some_cmd);counter((operand),count_of_operands) oo $HOME\\processing_report_test.log".split(" "))
		}

		assertGreppOutput(expectedResult) {
			Grepp.main("--new_report $HOME\\processing_report_test.log".split(" "))
		}
	}

	void testFileMTimeFiltering() {
		def fileTime = new Date(new File(HOME+"\\processing_time_test.log").lastModified())
		def dateFormat = new SimpleDateFormat("yyyy-MM-dd")
		def testTimeStringFrom = dateFormat.format(new Date(fileTime.getTime() + 24*60*60*1000))

		def expectedResult = ""
		assertGreppOutput(expectedResult) {
			Grepp.main("-d $testTimeStringFrom;+ Foo $HOME\\processing_time_test.log".split(" "))
		}

	}

	void testComplexFiltering() {

		def expectedResult = """\
2012-09-20 05:05:56,951 [ACTIVE] ThreadStart: '22' 
Foo Koo

2013-09-20 05:05:57,951 [ACTIVE] ThreadStart: '22' SkipPattern
Too
2014-09-20 05:05:57,951 [ACTIVE] ThreadStart: '22' ThreadEnd1
Goo
2012-10-20 05:05:56,951 [ACTIVE] ThreadStart: '1' 
Foo Man Chu
#basic
2012-10-20 05:05:57,952 [ACTIVE] ThreadStart: '1' SkipPattern
Loo
2012-10-20 05:05:57,953 [ACTIVE] ThreadStart: '1' ThreadEnd2
Voo
#complex"""

		assertGreppOutput(expectedResult) {
			Grepp.main("-e Foo $HOME\\processing_test.log".split(" "))
		}
	}

	void testComplexUserPatternFiltering() {

		def expectedResult = """\
2012-10-20 05:05:56,951 [ACTIVE] ThreadStart: '1' 
Foo Man Chu
#basic"""
		assertGreppOutput(expectedResult) {
			Grepp.main((String[]) ["Foo%and%Man Chu%or%#basic" //don't need to split here
				, "$HOME\\processing_test.log"])
		}
	}

	void testBasicFiltering() {

		def expectedResult = """\
2012-09-20 05:05:56,951 [ACTIVE] ThreadStart: '22' 
Foo Koo

2012-10-20 05:05:56,951 [ACTIVE] ThreadStart: '1' 
Foo Man Chu
#basic"""

		assertGreppOutput(expectedResult) {
			Grepp.main("Foo $HOME\\processing_test.log".split(" "))
		}
	}

	void testTimeFiltering() {

		def fileTime = new Date(new File(HOME+"\\processing_time_test.log").lastModified())
		def dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH")
		def logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH")
		def testTimeStringFrom = dateFormat.format(fileTime)

		def expectedResult = """\
${logDateFormat.format(fileTime)}:05:56,951 [ACTIVE] ThreadStart: '22' 
Foo Koo
"""
		assertGreppOutput(expectedResult) {
			Grepp.main("-d $testTimeStringFrom;+60 Foo $HOME\\processing_time_test.log".split(" "))
		}
	}

	void testLogDateConfigFiltering() {

		def fileTime = new Date(new File(HOME+"\\processing_time_test.log").lastModified())
		def dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH")
		def logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH")
		def testTimeStringFrom = dateFormat.format(fileTime)

		def expectedResult = """\
${logDateFormat.format(fileTime)}:05:56,951 [ACTIVE] ThreadStart: '22' 
Foo Koo
"""
		assertGreppOutput(expectedResult) {
			Grepp.main("--lock -d $testTimeStringFrom;+60 --iso Foo $HOME\\processing_time_test.log".split(" "))
		}
	}

	void testTimeLeftBoundOnlyFiltering() {

		def fileTime = new Date(new File(HOME+"\\processing_time_test.log").lastModified())
		def dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH")
		def logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH")
		def testTimeStringFrom = dateFormat.format(fileTime)

		def expectedResult = """\
${logDateFormat.format(fileTime)}:05:56,951 [ACTIVE] ThreadStart: '22' 
Foo Koo

${logDateFormat.format(new Date(fileTime.getTime() + 3*60*60*1000))}:05:56,951 [ACTIVE] ThreadStart: '1' 
Foo Man Chu
#basic"""
		assertGreppOutput(expectedResult) {
			Grepp.main("-d $testTimeStringFrom;+ Foo $HOME\\processing_time_test.log".split(" "))
		}
	}

	void testTimeRightBoundOnlyFiltering() {

		def fileTime = new Date(new File(HOME+"\\processing_time_test.log").lastModified())
		def dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH")
		def logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH")
		def testTimeStringTo = dateFormat.format(new Date(fileTime.getTime() + 60*60*1000))

		def expectedResult = """\
${logDateFormat.format(fileTime)}:05:56,951 [ACTIVE] ThreadStart: '22' 
Foo Koo
"""
		assertGreppOutput(expectedResult) {
			Grepp.main("-d +;$testTimeStringTo --foo $HOME\\processing_time_test.log".split(" "))
		}
	}

	void testReportFiltering() {

		def expectedResult = """\
some_cmd,count_of_operands
Foo,3
Koo,1
Foo,1"""

		assertGreppOutput(expectedResult) {
			Grepp.main("--count_ops $HOME\\processing_report_test.log".split(" "))
		}
	}

	void testReportDefaultGroupFiltering() {

		def expectedResult = """\
some_cmd,count_of_operands
Foo,4
Koo,1
"""

		assertGreppOutput(expectedResult) {
			Grepp.main("--group_ops $HOME\\processing_report_test.log".split(" "))
		}
	}

	void testReportDefaultStringGroupFiltering() {

		def expectedResult = """\
some_cmd,operands
Foo,alpha;bravo;delta;gamma
Koo,this
"""

		assertGreppOutput(expectedResult) {
			Grepp.main("--group_op_values $HOME\\processing_report_test.log".split(" "))
		}
	}


	void testReportAverageFiltering() {

		def expectedResult = """\
some_cmd,avg_processing
Foo,150
Koo,200
"""
		assertGreppOutput(expectedResult) {
			Grepp.main("--avg_timings $HOME\\processing_report_test.log".split(" "))
		}
	}

	void testHeteroFilesGreppMain() {

		def expectedResult = """\
2012-09-20 05:05:56,951 [ACTIVE] ThreadStart: '22' 
Foo Koo

2012-10-20 05:05:56,951 [ACTIVE] ThreadStart: '1' 
Foo Man Chu
#basic"""

		assertGreppOutput(expectedResult) {
			Grepp.main("Foo $HOME\\processing_test.log $HOME\\fpTest_test.log".split(" "))
		}
	}

	void testPropertiesFilter() {
		def configString = """\
log4j.logger.com.netcracker.solutions.tnz.cwms=DEBUG, CWMSGlobal
log4j.appender.CWMSGlobal=org.apache.log4j.RollingFileAppender
log4j.appender.CWMSGlobal.File=logs/cwms_debug_\${weblogic.Name}.log
log4j.appender.CWMSGlobal.MaxFileSize=50MB
log4j.appender.CWMSGlobal.MaxBackupIndex=20
log4j.appender.CWMSGlobal.layout=org.apache.log4j.PatternLayout
log4j.appender.CWMSGlobal.layout.ConversionPattern=\\#\\#\\#\\#[%-5p] %d{ISO8601} %t %c - %n%m%n
"""
		def expectedResult = """\
savedConfigs {
	cwms_debug_ {
		dateFormat {
			value='yyyy-MM-dd HH:mm:ss,SSS'
			regex='(\\\\d{4}-\\\\d{2}-\\\\d{2} \\\\d{2}:\\\\d{2}:\\\\d{2},\\\\d{3})'
		}
		starter='\\\\#\\\\#\\\\#\\\\#\\\\[[TRACEDBUGINFLOWSV]* *\\\\].*'
		pattern='cwms_debug_.*\\\\.log'
	}
}
logDateFormats {
	cwms_debug_ {
		value='yyyy-MM-dd HH:mm:ss,SSS'
		regex='(\\\\d{4}-\\\\d{2}-\\\\d{2} \\\\d{2}:\\\\d{2}:\\\\d{2},\\\\d{3})'
	}
}
"""	
	
		def propFilter = new PropertiesFilter()
		assertTrue(propFilter.filter(configString).replace("\r\n", "\n") == expectedResult)
	}

	void testPropertiesProcessing() {

		Grepp.main("--parse $HOME\\test.properties".split(" "))

		def changedConfig = new ConfigHolder(new URL('file', '/', GREPP_CONFIG))
		assertTrue(changedConfig.savedConfigs.containsKey('cwms_debug_'))
		assertTrue(changedConfig.logDateFormats.containsKey('cwms_debug_'))
		assertTrue(changedConfig.savedConfigs.cwms_debug_.starter == "\\#\\#\\#\\#\\[[TRACEDBUGINFLOWSV]* *\\].*")
		assertTrue(changedConfig.savedConfigs.cwms_debug_.pattern == "cwms_debug_.*\\.log")
	}

	void testInputStreamProcessing() {
		def tPipeOut = new PipedOutputStream()
		def tPipeIn = new PipedInputStream(tPipeOut)
		def passToIn = new PrintStream(tPipeOut)
		def text = """\
#asda
asdas
#asdas
#sadas
fdsfd
"""
		passToIn.print(text)
		passToIn.close()
		def oldIn = System.in
		System.setIn(tPipeIn)
		def expectedResult = """#asda
asdas
#asdas"""

		try {
			assertGreppOutput(expectedResult) {
				Grepp.main("-l # asd".split(" "))
			}
		}
		catch (Exception e) {
			tPipeIn.close()
			System.setIn(oldIn)
			throw e
		}
		finally {
			tPipeIn.close()
			System.setIn(oldIn)
		}
	}
	
}
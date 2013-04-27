import org.smlt.tools.wgrep.*
import org.smlt.tools.wgrep.config.WgrepConfig

import groovy.util.GroovyTestCase
import java.text.SimpleDateFormat
class WGrepTest extends GroovyTestCase {
	WgrepFacade facade = null
	WgrepConfig config = null
	def BASE_HOME = System.getProperty("wgrep.home")
	def HOME = BASE_HOME + "\\build\\resources\\test"
	def WGREP_CONFIG = BASE_HOME + "\\build\\resources\\test\\config.xml"
	def WGREP_CONFIG_XSD = BASE_HOME + "\\build\\resources\\main\\config.xsd"
	def defalutOut = System.out

	void setUp() {
		config = new WgrepConfig(WGREP_CONFIG, WGREP_CONFIG_XSD)
		facade = new WgrepFacade(config)
	}

	void tearDown() {
		System.setOut(defalutOut)
	}

	public <V> String getOutput(Closure<V> operation) {
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

	public <V> void assertWgrepOutput(String expectedResult, Closure<V> operation) {
		assertTrue(expectedResult == getOutput(operation))
	}

	void testMainVarsProcessing() {
		config.processInVars([
			"-L",
			"test",
			"test",
			HOME+"\\fpTest*"
		])
		assertTrue( config.getParam('LOG_ENTRY_PATTERN') == "test" )
		assertTrue( config.getParam('FILTER_PATTERN') == "test" )
		assertTrue( config.getParam('FILES') == [
			new File(HOME+"\\fpTest_test.log")]
		)
		assertTrue( config.getParam('FOLDER_SEPARATOR') != null )
		assertTrue( config.getParam('HOME_DIR') != null )
	}
	
	void testConfigsProcessing() {
		config.processInVars([
			"--to_test",
			"--predef",
			HOME+"\\fpTest*"
		])
		assertTrue( config.getParam('LOG_ENTRY_PATTERN') == "####\\[\\D{1,}\\].*(\\d{4}-\\d{1,2}-\\d{1,2} \\d{2}:\\d{2}:\\d{2})" )
		assertTrue( config.getParam('FILTER_PATTERN') == "Something::" )
		assertTrue( config.getParam('FILES') == [
			new File(HOME+"\\fpTest_test.log")]
		)
		assertTrue( config.getParam('FOLDER_SEPARATOR') != null )
		assertTrue( config.getParam('HOME_DIR') != null )
	}

	void testFailAutomationProcessing() {
		facade.doProcessing([
			"-i",
			"test",
			HOME+"\\config*"
		])
		assertTrue( true )
	}

	void testExtendedPatternProcessing() {

		config.processInVars([
			"-L",
			"test",
			"test%and%tets",
			HOME+"\\test*"
		])
		assertTrue( config.getParam('FILTER_PATTERN') == "test%and%tets" )
	}

	void testComplexVarsProcessing() {

		config.processInVars([
			"-L",
			"test",
			"test",
			"--dtime",
			"2013-01-25T12:00:00",
			"+",
			HOME+"\\test*"
		])
		assertTrue( config.getParam('DATE_TIME_FILTER') == "dtime" )
	}

	void testAutomationProcessing() {
		config.processInVars([
			"-i",
			"test",
			HOME+"\\fpTest_*"
		])
		config.refreshConfigByFile(config.getParam('FILES')[0])
		assertTrue( config.getParam('LOG_ENTRY_PATTERN') == /####\[\D{1,}\].*(\d{4}-\d{1,2}-\d{1,2} \d{2}:\d{2}:\d{2})/)
		assertTrue( config.getParam('LOG_DATE_FORMAT') == "yyyy-MM-dd HH:mm:ss" )
	}

	void testMoreComplexVarsProcessing() {

		config.processInVars([
			"-sL",
			"stCommand",
			"queryTime",
			"--some_timings",
			"cmd_only_1.log"
		])
		assertTrue( config.getParam('LOG_ENTRY_PATTERN') == "stCommand" )
		assertTrue( config.getParam('FILTER_PATTERN') == "queryTime" )
		assertTrue( config.getParam('FILES') == [new File("cmd_only_1.log")])
		assertTrue( config.getParam('FOLDER_SEPARATOR') == "\\\\" )
		assertTrue( config.getParam('HOME_DIR') != null )
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
		
		assertWgrepOutput(expectedResult) {
			facade.doProcessing([
				"-i",
				"Foo",
				HOME+"\\processing_test.log"
			])
		}
	}

	void testComplexUserPatternFiltering() {

		def expectedResult = """\
2012-10-20 05:05:56,951 [ACTIVE] ThreadStart: '1' 
Foo Man Chu
#basic"""
		assertWgrepOutput(expectedResult) {
			facade.doProcessing([
				"Foo%and%Man Chu%or%#basic",
				HOME+"\\processing_test.log"
			])
		}
	}

	void testBasicFiltering() {

		def expectedResult = """\
2012-09-20 05:05:56,951 [ACTIVE] ThreadStart: '22' 
Foo Koo

2012-10-20 05:05:56,951 [ACTIVE] ThreadStart: '1' 
Foo Man Chu
#basic"""
		
		assertWgrepOutput(expectedResult) {
			facade.doProcessing([
				"Foo",
				HOME+"\\processing_test.log"
			])
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
		assertWgrepOutput(expectedResult) {
			facade.doProcessing([
				"Foo",
				"--dtime",
				testTimeStringFrom,
				"+60",
				HOME+"\\processing_time_test.log"
			])
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
		assertWgrepOutput(expectedResult) {
			facade.doProcessing([
				"Foo",
				"--dtime",
				testTimeStringFrom,
				"+",
				HOME+"\\processing_time_test.log"
			])
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
		assertWgrepOutput(expectedResult) {
			facade.doProcessing([
				"Foo",
				"--dtime",
				"+",
				testTimeStringTo,
				HOME+"\\processing_time_test.log"
			])
		}
	}

	void testPostFiltering() {

		def expectedResult = """\
some_cmd,count_of_operands
Foo,3
Koo,1"""

		assertWgrepOutput(expectedResult) {
			facade.doProcessing([
				"oo",
				"--some_timings",
				HOME+"\\processing_report_test.log"
			])
		}
	}

	void testPostAverageFiltering() {

		def expectedResult = """\
some_cmd,avg_processing
Foo,150
Koo,200
"""
		assertWgrepOutput(expectedResult) {
			facade.doProcessing([
				"oo",
				"--avg_timings",
				HOME+"\\processing_report_test.log"
			])
		}
	}
}
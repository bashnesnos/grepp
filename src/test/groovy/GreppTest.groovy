import org.smltools.grepp.*
import org.smltools.grepp.filters.entry.PropertiesFilter
import org.smltools.grepp.config.ParamHolderFactory
import org.smltools.grepp.config.CLIParamHolderFactory
import org.smltools.grepp.config.XMLConfigHolder
import org.smltools.grepp.config.ConfigHolder
import org.smltools.grepp.config.Param
import org.smltools.grepp.util.GreppUtil
import groovy.xml.DOMBuilder
import groovy.xml.dom.DOMCategory
import groovy.util.GroovyTestCase
import java.text.SimpleDateFormat

class GreppTest extends GroovyTestCase {

	ConfigHolder config
	ParamHolderFactory paramFactory
	def BASE_HOME = System.getProperty("grepp.home")
	def HOME = BASE_HOME + "\\build\\resources\\test"
	def GREPP_CONFIG = BASE_HOME + "\\build\\resources\\main\\config\\config.xml"
	def GREPP_CONFIG_XSD = BASE_HOME + "\\build\\resources\\main\\config\\config.xsd"

	void setUp() {
		config = new XMLConfigHolder(GREPP_CONFIG, GREPP_CONFIG_XSD)
		paramFactory = new CLIParamHolderFactory(config);
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
		assertTrue(expectedResult == actualResult)
	}

//	void testGetOptions(){
//		config.getOptions()
//	}
	
	void testMainVarsProcessing() {
		def params = paramFactory.getParamHolder("-L test test $HOME\\fpTest*".tokenize(" "))
		assertTrue( params.get(Param.LOG_ENTRY_PATTERN) == "test" )
		assertTrue( params.get(Param.FILTER_PATTERN) == "test" )
		assertTrue( params.get(Param.FILES) == [
			new File(HOME+"\\fpTest_test.log")]
		)
		assertTrue( params.get(Param.FOLDER_SEPARATOR) != null )
	}
	
	void testFileMTimeFiltering() {
		def fileTime = new Date(new File(HOME+"\\processing_time_test.log").lastModified())
		def dateFormat = new SimpleDateFormat("yyyy-MM-dd")
		def testTimeStringFrom = dateFormat.format(new Date(fileTime.getTime() + 24*60*60*1000))

		def expectedResult = ""
		assertGreppOutput(expectedResult) {
			Grepp.main("--dtime $testTimeStringFrom;+ Foo $HOME\\processing_time_test.log".split(" "))
		}

	}
	
	void testConfigsProcessing() {
		def params = paramFactory.getParamHolder("--to_test --predef $HOME\\fpTest*".tokenize(" "))
		assertTrue( params.get(Param.LOG_ENTRY_PATTERN) == "####\\[\\D{1,}\\].*(\\d{4}-\\d{1,2}-\\d{1,2} \\d{2}:\\d{2}:\\d{2})" )
		assertTrue( params.get(Param.FILTER_PATTERN) == "Something::" )
		assertTrue( params.get(Param.FILES) == [
			new File(HOME+"\\fpTest_test.log")]
		)
		assertTrue( params.get(Param.FOLDER_SEPARATOR) != null )
	}

	void testExtendedPatternProcessing() {

		def params = paramFactory.getParamHolder("-L test test%and%tets $HOME\\test*".tokenize(" "))
		assertTrue( params.get(Param.FILTER_PATTERN) == "test%and%tets" )
	}

	void testComplexVarsProcessing() {

		def params = paramFactory.getParamHolder("-L test --dtime 2013-01-25T12:00:00;+ test $HOME\\test*".tokenize(" "))
		assertTrue( params.get(Param.FROM_DATE) != null )
	}

	void testAutomationProcessing() {
		def params = paramFactory.getParamHolder("test $HOME\\fpTest_*".tokenize(" "))
		params.refresh(params.get(Param.FILES)[0])
		assertTrue( params.get(Param.LOG_ENTRY_PATTERN) == /####\[\D{1,}\].*(\d{4}-\d{1,2}-\d{1,2} \d{2}:\d{2}:\d{2})/)
		assertTrue( params.get(Param.LOG_DATE_FORMAT) == "yyyy-MM-dd HH:mm:ss" )
	}

	void testMoreComplexVarsProcessing() {

		def params = paramFactory.getParamHolder("-s -L stCommand --some_timings cmd_only_1.log".tokenize(" "))
		assertTrue('logEntry', "stCommand".equals(params.get(Param.LOG_ENTRY_PATTERN))  )
		assertTrue('filterPattern', "oo".equals(params.get(Param.FILTER_PATTERN))  )
		assertTrue('files', params.get(Param.FILES).containsAll([new File("cmd_only_1.log")]))
		assertTrue('separator', "\\\\".equals(params.get(Param.FOLDER_SEPARATOR)))
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
			Grepp.main("--dtime $testTimeStringFrom;+60 Foo $HOME\\processing_time_test.log".split(" "))
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
			Grepp.main("--dtime $testTimeStringFrom;+ Foo $HOME\\processing_time_test.log".split(" "))
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
			Grepp.main("--dtime +;$testTimeStringTo --foo $HOME\\processing_time_test.log".split(" "))
		}
	}

	void testPostFiltering() {

		def expectedResult = """\
some_cmd,count_of_operands
Foo,3
Koo,1"""

		assertGreppOutput(expectedResult) {
			Grepp.main("--some_timings $HOME\\processing_report_test.log".split(" "))
		}
	}

	void testPostAverageFiltering() {

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
cwms_debug_ {
	dateFormat {
		value='yyyy-MM-dd HH:mm:ss,SSS'
		regex='(\\\\d{4}-\\\\d{2}-\\\\d{2} \\\\d{2}:\\\\d{2}:\\\\d{2},\\\\d{3})'
	}
	starter='\\\\#\\\\#\\\\#\\\\#\\\\[[TRACEDBUGINFLOWSV]* *\\\\].*'
	pattern='cwms_debug_.*\\\\.log'
}
"""	
		def propFilter = new PropertiesFilter(null)
		assertTrue(propFilter.filter(configString).replace("\r\n", "\n") == expectedResult)
	}

	void testPropertiesProcessing() {

		Grepp.main("--parse $HOME\\test.properties".split(" "))
		def cfgDoc = DOMBuilder.parse(new FileReader(GREPP_CONFIG))
		def root = cfgDoc.documentElement
		use(DOMCategory) {
			def config = root.config.find { it.'@id' == "cwms_debug_" }
			assertTrue(config != null)
			assertTrue(config.logDateFormat.text() == "cwms_debug_")
			assertTrue(config.starter.text() == "\\#\\#\\#\\#\\[[TRACEDBUGINFLOWSV]* *\\].*")
			assertTrue(config.pattern.text() == "cwms_debug_.*\\.log")
		}

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
				Grepp.main("-L # asd".split(" "))
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
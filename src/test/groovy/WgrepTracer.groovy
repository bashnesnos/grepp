import groovy.xml.dom.DOMCategory
import org.smlt.tools.wgrep.*
import org.smlt.tools.wgrep.config.WgrepConfig
import java.text.SimpleDateFormat
import java.util.regex.Matcher
import org.smlt.tools.wgrep.config.varparsers.*
import java.util.*
import WGrepTest

def BASE_HOME = System.getProperty("wgrep.home")
def WGREP_CONFIG = BASE_HOME + "\\build\\resources\\test\\config.xml"
//def WGREP_CONFIG = BASE_HOME + "\\dev\\config.xml"
def HOME = BASE_HOME + "\\build\\resources\\test"

WgrepConfig config = new WgrepConfig(WGREP_CONFIG)
WgrepFacade facade = new WgrepFacade(config)

        def fileTime = new Date(new File(HOME+"\\processing_time_test.log").lastModified())
        def dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH")
        def logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH")
        def testTimeStringFrom = dateFormat.format(fileTime)
        def testTimeStringTo = dateFormat.format(new Date(fileTime.getTime() + 60*60*1000))

def test = new WGrepTest()
	def actualResult = test.getOutput {			facade.doProcessing([
				"-i",
				"Foo",
				HOME+"\\processing_test.log"
			])
	}
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
	println actualResult
	println actualResult == expectedResult
//println actualResult.toString()
//println expectedResult == actualResult.toString()

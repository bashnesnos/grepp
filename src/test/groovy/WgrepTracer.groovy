import groovy.xml.dom.DOMCategory
import org.smlt.tools.wgrep.*
import java.text.SimpleDateFormat
import java.util.regex.Matcher

def BASE_HOME = System.getProperty("wgrep.home")
def WGREP_CONFIG = BASE_HOME + "\\build\\resources\\test\\config.xml"
def HOME = BASE_HOME + "\\build\\resources\\test"

WgrepFacade facade = WgrepFacade.getInstance([WGREP_CONFIG])
        def fileTime = new Date(new File(HOME+"\\processing_time_test.log").lastModified())
        def dateFormat = new SimpleDateFormat('yyyy-MM-dd')
        def testTimeString = dateFormat.format(fileTime)
        facade.processInVars(["-ate", "Command%or%Foo", "--dtime", testTimeString, "+", "--some_timings", HOME+"\\processing_report_test.log"])

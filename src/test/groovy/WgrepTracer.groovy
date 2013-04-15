import groovy.xml.dom.DOMCategory
import org.smlt.tools.wgrep.*
import org.smlt.tools.wgrep.config.WgrepConfig
import java.text.SimpleDateFormat
import java.util.regex.Matcher

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
        
        facade.doProcessing(["Foo", "--dtime", testTimeStringFrom, testTimeStringTo, HOME+"\\processing_time_test.log"])
		
    

// println actualResult.toString()
//	println expectedResult == actualResult.toString()

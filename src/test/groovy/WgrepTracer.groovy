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

        facade.doProcessing(["-i", "Foo", HOME+"\\processing_test.log"])
		
    

// println actualResult.toString()
//	println expectedResult == actualResult.toString()

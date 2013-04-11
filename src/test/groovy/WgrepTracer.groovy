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

        config.processInVars(["-ti","test", HOME+"\\fpTest_*"])
        config.refreshConfigByFileName(config.getParam('FILES')[0])
		
def expectedResult = """\
#
$testTimeString 05:05:56,951 [ACTIVE] ThreadStart: '22' 
Foo Koo

"""
    

// println actualResult.toString()
//	println expectedResult == actualResult.toString()

import groovy.xml.dom.DOMCategory
import org.smlt.tools.wgrep.*

def BASE_HOME = System.getProperty("wgrep.home")
def WGREP_CONFIG = BASE_HOME + "\\build\\resources\\test\\config.xml"
def HOME = BASE_HOME + "\\build\\resources\\test"

WgrepFacade facade = WgrepFacade.getInstance([WGREP_CONFIG])
        facade.processInVars(["-ts", "stCommand", "queryTime", "--some_timings", "cmd_only_1.log"])


//println facade.LOG_ENTRY_PATTERN
//println facade.FILTER_PATTERN 
//println facade.FILES 
//facade.startProcessing()
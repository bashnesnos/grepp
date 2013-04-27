import groovy.xml.dom.DOMCategory
import org.smlt.tools.wgrep.*
import org.smlt.tools.wgrep.config.WgrepConfig
import java.text.SimpleDateFormat
import java.util.regex.Matcher
import org.smlt.tools.wgrep.config.varparsers.*
import WGrepTest


import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory


def BASE_HOME = System.getProperty("wgrep.home")
def WGREP_CONFIG = BASE_HOME + "\\build\\resources\\test\\config.xml"
def WGREP_CONFIG_XSD = BASE_HOME + "\\build\\resources\\main\\config.xsd"
//def WGREP_CONFIG = BASE_HOME + "\\dev\\config.xml"
def HOME = BASE_HOME + "\\build\\resources\\test"
def config = new WgrepConfig(WGREP_CONFIG, WGREP_CONFIG_XSD)
def facade = new WgrepFacade(config)

		config.processInVars([
			"--to_test",
			"--predef",
			HOME+"\\fpTest*"
		])
		
		println config.getParam('LOG_ENTRY_PATTERN')
		println config.getParam('FILTER_PATTERN') 
		println config.getParam('FILES') 
		println config.getParam('FOLDER_SEPARATOR') 
		println config.getParam('HOME_DIR') 

//println actualResult.toString()
//println expectedResult == actualResult.toString()

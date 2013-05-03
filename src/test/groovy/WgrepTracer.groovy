import groovy.xml.dom.DOMCategory
import org.smlt.tools.wgrep.*
import org.smlt.tools.wgrep.config.WgrepConfig
import java.text.SimpleDateFormat
import java.util.regex.Matcher
import org.smlt.tools.wgrep.config.varparsers.*
import org.smlt.tools.wgrep.util.WgrepUtil
import WGrepTest


import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory


def BASE_HOME = System.getProperty("wgrep.home")
//def WGREP_CONFIG = BASE_HOME + "\\build\\resources\\test\\config.xml"
//def WGREP_CONFIG_XSD = BASE_HOME + "\\build\\resources\\main\\config.xsd"
def WGREP_CONFIG = WgrepUtil.getResourcePathOrNull("config.xml")
def WGREP_CONFIG_XSD = WgrepUtil.getResourcePathOrNull("config.xsd")

//def WGREP_CONFIG = BASE_HOME + "\\dev\\config.xml"
def HOME = BASE_HOME + "\\build\\resources\\test"

//println WGREP_CONFIG
//println WGREP_CONFIG_XSD

def config = new WgrepConfig(WGREP_CONFIG, WGREP_CONFIG_XSD)
def facade = new WgrepFacade(config)

			WGrep.main([
				"Foo",
				HOME+"\\processing_tgest.log",
				HOME+"\\fpTest_test.log"
			].toArray(new String[2]))

//println actualResult.toString()
//println expectedResult == actualResult.toString()

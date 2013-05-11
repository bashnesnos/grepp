import groovy.xml.dom.DOMCategory
import org.smlt.tools.wgrep.*
import org.smlt.tools.wgrep.config.PatternAutomationConfig
import java.text.SimpleDateFormat
import java.util.regex.Matcher
import org.smlt.tools.wgrep.config.varparsers.*
import org.smlt.tools.wgrep.util.WgrepUtil


import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory


def BASE_HOME = System.getProperty("wgrep.home")
def WGREP_CONFIG = BASE_HOME + "\\build\\resources\\test\\config.xml"
def WGREP_CONFIG_XSD = BASE_HOME + "\\build\\resources\\main\\config\\config.xsd"
//def WGREP_CONFIG = WgrepUtil.getResourcePathOrNull("config.xml")
//def WGREP_CONFIG_XSD = WgrepUtil.getResourcePathOrNull("config.xsd")

def HOME = BASE_HOME + "\\build\\resources\\test"

//def config = new PatternAutomationConfig(WGREP_CONFIG, WGREP_CONFIG_XSD)

//config.enforceTrace(null, null)

//println WGREP_CONFIG
//println WGREP_CONFIG_XSD
			WGrep.main((String[]) ["-ft",
				"--avg_timings",
				HOME+"\\processing_report_test.log"])

			//WGrep.main((String[]) ["-ft",
			//	"--avg_timings",
			//	HOME+"\\processing_report_test.log"])
//println actualResult.toString()
//println expectedResult == actualResult.toString()

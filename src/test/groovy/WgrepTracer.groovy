import groovy.xml.dom.DOMCategory
import org.smlt.tools.wgrep.*
import org.smlt.tools.wgrep.config.PatternAutomationConfig
import java.text.SimpleDateFormat
import java.util.regex.Matcher
import org.smlt.tools.wgrep.config.varparsers.*
import org.smlt.tools.wgrep.util.WgrepUtil
import org.smlt.tools.wgrep.filters.entry.PropertiesFilter
import groovy.xml.DOMBuilder
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
//			WGrep.main((String[]) ["-ft",
				//"--avg_timings",
				//HOME+"\\processing_report_test.log"])

//WGrep.main("-t -L log4j.logger -p $HOME\\test.properties".split(" "))

WGrep.main("-t Foo $HOME\\processing_test.log $HOME\\fpTest_test.log".split(" "))


//		def data = """\
//<config id='cwms_debug_'>
//  <date_format>yyyy-MM-dd HH:mm:ss,SSS</date_format>
//  <date>(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3})</date>
//  <starter>\\#\\#\\#\\#\\[[TRACEDBUGINFOWLSV]* *\\].*</starter>
//  <log_threshold>0</log_threshold>
//  <pattern>cwms_debug_.*\\.log</pattern>
//</config>"""
//
//		def customConfig = DOMBuilder.newInstance(false, false).parseText(data.toString()).documentElement
//		println customConfig
//		def cfgDoc = DOMBuilder.parse(new FileReader(WGREP_CONFIG))
//		def root = cfgDoc.documentElement
//		use(DOMCategory) {
//			def configId = customConfig.'@id'
//			println configId
//			println customConfig.config[0]
////			def config = root.custom.config.find { it.'@id' == configId }
////			println config
////			println config.hasAttribute("xmlns")
//			println customConfig.hasAttribute("xmlns")
//			def importedNode = cfgDoc.importNode(customConfig, true)
//	        println importedNode
//	        println importedNode.hasAttribute("xmlns")
//	        importedNode.removeAttribute("xmlns") //it is appearing by default
//	        println importedNode.hasAttribute("xmlns")
//					//root.custom[0].insertBefore(importedNode, root.custom.config[0])	
//		}


//println actualResult.toString()
//println expectedResult == actualResult.toString()

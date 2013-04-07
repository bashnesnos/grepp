import groovy.xml.dom.DOMCategory
import org.smlt.tools.wgrep.*
import java.text.SimpleDateFormat
import java.util.regex.Matcher

def BASE_HOME = System.getProperty("wgrep.home")
def WGREP_CONFIG = BASE_HOME + "\\build\\resources\\test\\config.xml"
//def WGREP_CONFIG = BASE_HOME + "\\dev\\config.xml"
def HOME = BASE_HOME + "\\build\\resources\\test"

WgrepFacade facade = WgrepFacade.getInstance([WGREP_CONFIG])

        facade.processInVars(["-t", "oo", "--avg_operands", HOME+"\\processing_report_test.log"])

            facade.startProcessing()

        def expectedResult = """\
#
some_cmd,avg_processing
Foo,150
Koo,200
"""

	//println actualResult.toString()

import groovy.xml.dom.DOMCategory
import org.smlt.tools.wgrep.*
import org.smlt.tools.wgrep.config.WgrepConfig
import java.text.SimpleDateFormat
import java.util.regex.Matcher
import org.smlt.tools.wgrep.config.varparsers.*
import java.util.*

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

            //config.processInVars(["-t", "Foo", "--dtime", "-10", "+", HOME+"\\processing_time_test.log"])
            def offsetStr = "+10"
            def matcher = offsetStr =~ /^\+.*/
            println matcher.find()
            def date;
            switch(offsetStr) {
                case ~/^\+.*/:
                    println("adding")
                    date = new Date(new Date().getTime() + 10*60*1000) //adding minutes to NOW
                    break
                case ~/^-.*/:
                    println("subtracting")
                    date = new Date(new Date().getTime() - 10*60*1000) //subtracting minutes to NOW
                    break
            }
            println date;
//println actualResult.toString()
//println expectedResult == actualResult.toString()

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

        def oldStdout = System.out
        def pipeOut = new PipedOutputStream()
        def pipeIn = new PipedInputStream(pipeOut)
        System.setOut(new PrintStream(pipeOut))

        try
        {
            facade.doProcessing(["oo", "--avg_timings", HOME+"\\processing_report_test.log"])
        }
        catch (Exception e) {
            pipeOut.close()
            System.setOut(oldStdout)
            throw e
        }
        finally {
            System.setOut(oldStdout)
            pipeOut.close()
        }

        def outputReader = new BufferedReader(new InputStreamReader(pipeIn))

        def line = '#'
        StringBuffer actualResult = new StringBuffer()

        if (outputReader.ready())
        {
            while (line != null)
            {
                actualResult = actualResult.append(line).append('\n')
                line = outputReader.readLine()
            }
        }

        def expectedResult = """\
#
some_cmd,avg_processing
Foo,150
Koo,200

"""

        //assertTrue( expectedResult == actualResult.toString() )
println actualResult.toString()
	println expectedResult == actualResult.toString()

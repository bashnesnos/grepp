import groovy.xml.dom.DOMCategory
import org.smlt.tools.wgrep.*
import java.text.SimpleDateFormat
import java.util.regex.Matcher

def BASE_HOME = System.getProperty("wgrep.home")
def WGREP_CONFIG = BASE_HOME + "\\build\\resources\\test\\config.xml"
def HOME = BASE_HOME + "\\build\\resources\\test"

WgrepFacade facade = WgrepFacade.getInstance([WGREP_CONFIG])
        facade.processInVars(["-a", "oo", "--some_timings", HOME+"\\processing_report_test.log"])
        def oldStdout = System.out
        def pipeOut = new PipedOutputStream()
        def pipeIn = new PipedInputStream(pipeOut)
        System.setOut(new PrintStream(pipeOut))

        try
        {
            facade.startProcessing()
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
some_cmd,count_of_operands
Foo,3
Koo,1
Boo
"""
println actualResult.toString()
println expectedResult
println expectedResult == actualResult.toString()
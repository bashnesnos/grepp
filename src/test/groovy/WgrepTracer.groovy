import groovy.xml.dom.DOMCategory
import org.smlt.tools.wgrep.*

def BASE_HOME = System.getProperty("wgrep.home")
def WGREP_CONFIG = BASE_HOME + "\\build\\resources\\test\\config.xml"
def HOME = BASE_HOME + "\\build\\resources\\test"

WgrepFacade facade = WgrepFacade.getInstance([WGREP_CONFIG])
        facade.processInVars(["-a", "Foo", "--dtime", "2013-03-25T05", "2013-03-25T06", HOME+"\\processing_time_test.log"])
        def oldStdout = System.out
        def pipeOut = new PipedOutputStream()
        def pipeIn = new PipedInputStream(pipeOut)
        System.setOut(new PrintStream(pipeOut))
        facade.startProcessing()
        def outputReader = new BufferedReader(new InputStreamReader(pipeIn))
        System.setOut(oldStdout)
        pipeOut.close()

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
2013-03-25 05:05:56,951 [ACTIVE] ThreadStart: '22' 
Foo Koo

"""

println actualResult.toString()
println expectedResult == actualResult.toString()
//println facade.LOG_ENTRY_PATTERN
//println facade.FILTER_PATTERN 
//println facade.FILES 
//facade.startProcessing()
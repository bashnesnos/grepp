import groovy.xml.dom.DOMCategory
import org.smlt.tools.wgrep.*

def BASE_HOME = System.getProperty("wgrep.home")
def WGREP_CONFIG = BASE_HOME + "\\build\\resources\\test\\config.xml"
def HOME = BASE_HOME + "\\build\\resources\\test"

WgrepFacade facade = WgrepFacade.getInstance([WGREP_CONFIG])
        facade.processInVars(["-i", "Foo", HOME+"\\processing_test.log"])
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
        println actualResult.toString()
def expectedResult = """\
#
2012-09-20 05:05:56,951 [ACTIVE] ThreadStart: '22' 
Foo Koo

2013-09-20 05:05:57,951 [ACTIVE] ThreadStart: '22' SkipPattern
Too
2014-09-20 05:05:57,951 [ACTIVE] ThreadStart: '22' ThreadEnd1
Goo
2012-10-20 05:05:56,951 [ACTIVE] ThreadStart: '1' 
Foo Man Chu
#basic
2012-10-20 05:05:57,952 [ACTIVE] ThreadStart: '1' SkipPattern
Loo
2012-10-20 05:05:57,953 [ACTIVE] ThreadStart: '1' ThreadEnd2
Voo
#complex
"""
    println expectedResult == actualResult.toString()

//println facade.LOG_ENTRY_PATTERN
//println facade.FILTER_PATTERN 
//println facade.FILES 
//facade.startProcessing()
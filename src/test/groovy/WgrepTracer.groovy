import groovy.xml.dom.DOMCategory
import org.smlt.tools.wgrep.*
import java.text.SimpleDateFormat
import java.util.regex.Matcher

def BASE_HOME = System.getProperty("wgrep.home")
def WGREP_CONFIG = BASE_HOME + "\\build\\resources\\test\\config.xml"
//def WGREP_CONFIG = BASE_HOME + "\\dev\\config.xml"
def HOME = BASE_HOME + "\\build\\resources\\test"

WgrepFacade facade = WgrepFacade.getInstance()
facade.initConfig(WGREP_CONFIG)

//        def oldStdout = System.out
//        def pipeOut = new PipedOutputStream()
//        def pipeIn = new PipedInputStream(pipeOut)
//        System.setOut(new PrintStream(pipeOut))
//
//        try
//        {
            facade.startProcessing(["Foo%and%Man Chu", HOME+"\\processing_test.log"])
//        }
//        catch (Exception e) {
//            pipeOut.close()
//            System.setOut(oldStdout)
//            throw e
//        }
//        finally {
//            System.setOut(oldStdout)
//            pipeOut.close()
//        }
//
//        def outputReader = new BufferedReader(new InputStreamReader(pipeIn))
//
//        def line = '#'
//        StringBuffer actualResult = new StringBuffer()
//
//        if (outputReader.ready())
//        {
//            while (line != null)
//            {
//                actualResult = actualResult.append(line).append('\n')
//                line = outputReader.readLine()
//            }
//        }

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
    

//	println actualResult.toString()
//	println expectedResult == actualResult.toString()

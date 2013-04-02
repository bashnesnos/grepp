import groovy.xml.dom.DOMCategory
import org.smlt.tools.wgrep.*
import java.text.SimpleDateFormat
import java.util.regex.Matcher

def BASE_HOME = System.getProperty("wgrep.home")
def WGREP_CONFIG = BASE_HOME + "\\build\\resources\\test\\config.xml"
def HOME = BASE_HOME + "\\build\\resources\\test"

WgrepFacade facade = WgrepFacade.getInstance([WGREP_CONFIG])
def fileTime = new Date(new File(HOME+"\\processing_time_test.log").lastModified())
        def dateFormat = new SimpleDateFormat('yyyy-MM-dd')
        def testTimeString = dateFormat.format(fileTime)

        facade.processInVars(["-i","test", HOME+"\\fpTest_*"])
        facade.refreshConfigByFileName(facade.getParam('FILES')[0])
        println facade.getParam('LOG_ENTRY_PATTERN')
        println facade.getParam('LOG_DATE_FORMAT')
//        def oldStdout = System.out
//        def pipeOut = new PipedOutputStream()
//        def pipeIn = new PipedInputStream(pipeOut)
//        System.setOut(new PrintStream(pipeOut))
//
//        try
//        {
//            facade.startProcessing()
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
$testTimeString 05:05:56,951 [ACTIVE] ThreadStart: '22' 
Foo Koo

"""
    
//        println actualResult.toString()
//        println expectedResult == actualResult.toString() 

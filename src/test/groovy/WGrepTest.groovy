import org.smlt.tools.wgrep.*

import groovy.util.GroovyTestCase
import java.text.SimpleDateFormat

class WGrepTest extends GroovyTestCase
{
    WgrepFacade facade = null
    def BASE_HOME = System.getProperty("wgrep.home")
    def HOME = BASE_HOME + "\\build\\resources\\test"
    def defalutOut = System.out

    void setUp() 
    {
        facade = WgrepFacade.getInstance([BASE_HOME + "\\build\\resources\\test\\config.xml"])
    }
    
    void tearDown()
    {
        WgrepFacade.reset()
        System.setOut(defalutOut)
    }
    
    void testMainVarsProcessing()
    {
        
        facade.processInVars(["-","test","test",HOME+"\\fpTest*"])
        assertTrue( facade.getParam('LOG_ENTRY_PATTERN') == "test" )
        assertTrue( facade.getParam('FILTER_PATTERN') == "test" )
        //assertTrue( facade.getParam('FILES') == [HOME+"\\fpTest_test.log"] )
        assertTrue( facade.getParam('FOLDER_SEPARATOR') == "\\\\" )
        assertTrue( facade.getParam('HOME_DIR') != null )
    }

    void testFailAutomationProcessing()
    {
        
        facade.processInVars(["-i","test", HOME+"\\config*"])
        facade.startProcessing()
        assertTrue( true )
    }
    
    void testExtendedPatternProcessing()
    {
        
        facade.processInVars(["-e","test","test%and%tets",HOME+"\\test*"])
        assertTrue( facade.getParam('EXTNDD_PATTERN') == 'e')
        assertTrue( facade.getParam('FILTER_PATTERN') == "test%and%tets" )
    }
    
    void testComplexVarsProcessing()
    {
        
        facade.processInVars(["-","test","test","--dtime", "2013-01-25T12:00:00", "+", HOME+"\\test*"])
        assertTrue( facade.getParam('DATE_TIME_FILTER') == "dtime" )
    }

    void testAutomationProcessing()
    {
        
        facade.processInVars(["-i","test", HOME+"\\fpTest_*"])
        facade.refreshConfigByFileName(facade.getParam('FILES')[0])
        assertTrue( facade.getParam('LOG_ENTRY_PATTERN') == /####\[\D{1,}\].*(\d{4}-\d{1,2}-\d{1,2} \d{2}:\d{2}:\d{2})/)
        assertTrue( facade.getParam('LOG_DATE_FORMAT') == "yyyy-MM-dd HH:mm:ss" )
    }

    void testMoreComplexVarsProcessing()
    {
        
        facade.processInVars(["-s", "stCommand", "queryTime", "--some_timings", "cmd_only_1.log"])
        assertTrue( facade.getParam('LOG_ENTRY_PATTERN') == "stCommand" )
        assertTrue( facade.getParam('FILTER_PATTERN') == "queryTime" )
        assertTrue( facade.getParam('FILES') == ["cmd_only_1.log"] )
        assertTrue( facade.getParam('FOLDER_SEPARATOR') == "\\\\" )
        assertTrue( facade.getParam('HOME_DIR') != null )
    }

    void testTraceForComplexProcessing()
    {
        def fileTime = new Date(new File(HOME+"\\processing_time_test.log").lastModified())
        def dateFormat = new SimpleDateFormat('yyyy-MM-dd')
        def testTimeString = dateFormat.format(fileTime)

        facade.processInVars(["-ate", "Command%or%Foo", "--dtime", testTimeString, "+", "--some_timings", HOME+"\\processing_report_test.log"])
        facade.startProcessing()
        assertTrue( true )
    }

    void testComplexFiltering()
    {
        
        facade.processInVars(["-i", "Foo", HOME+"\\processing_test.log"])
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
    
        assertTrue( expectedResult == actualResult.toString() )
    }

    void testComplexUserPatternFiltering()
    {
        
        facade.processInVars(["-ae", "Foo%and%Man Chu", HOME+"\\processing_test.log"])
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
2012-10-20 05:05:56,951 [ACTIVE] ThreadStart: '1' 
Foo Man Chu
#basic
"""
    
        assertTrue( expectedResult == actualResult.toString() )
    }

    void testBasicFiltering()
    {
        
        facade.processInVars(["-a", "Foo", HOME+"\\processing_test.log"])
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
2012-09-20 05:05:56,951 [ACTIVE] ThreadStart: '22' 
Foo Koo

2012-10-20 05:05:56,951 [ACTIVE] ThreadStart: '1' 
Foo Man Chu
#basic
"""
    
        assertTrue( expectedResult == actualResult.toString() )
    }

    void testTimeFiltering()
    {
        
        def fileTime = new Date(new File(HOME+"\\processing_time_test.log").lastModified())
        def dateFormat = new SimpleDateFormat('yyyy-MM-dd')
        def testTimeString = dateFormat.format(fileTime)

        facade.processInVars(["-a", "Foo", "--dtime", testTimeString+"T05", testTimeString+"T06", HOME+"\\processing_time_test.log"])
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
$testTimeString 05:05:56,951 [ACTIVE] ThreadStart: '22' 
Foo Koo

"""
    
        assertTrue( expectedResult == actualResult.toString() )
    }

    void testTimeLeftBoundOnlyFiltering()
    {
        
        def fileTime = new Date(new File(HOME+"\\processing_time_test.log").lastModified())
        def dateFormat = new SimpleDateFormat('yyyy-MM-dd')
        def testTimeString = dateFormat.format(fileTime)

        facade.processInVars(["-a", "Foo", "--dtime", testTimeString+"T05", "+", HOME+"\\processing_time_test.log"])
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
$testTimeString 05:05:56,951 [ACTIVE] ThreadStart: '22' 
Foo Koo

$testTimeString 07:05:56,951 [ACTIVE] ThreadStart: '1' 
Foo Man Chu
#basic
"""
    
        assertTrue( expectedResult == actualResult.toString() )
    }

    void testTimeRightBoundOnlyFiltering()
    {
        
        def fileTime = new Date(new File(HOME+"\\processing_time_test.log").lastModified())
        def dateFormat = new SimpleDateFormat('yyyy-MM-dd')
        def testTimeString = dateFormat.format(fileTime)

        facade.processInVars(["-a", "Foo", "--dtime", "+", testTimeString+"T06", HOME+"\\processing_time_test.log"])
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
$testTimeString 05:05:56,951 [ACTIVE] ThreadStart: '22' 
Foo Koo

"""
    
        assertTrue( expectedResult == actualResult.toString() )
    }
    void testPostFiltering()
    {
        
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

        assertTrue( expectedResult == actualResult.toString() )
    }
}
import org.smlt.tools.wgrep.*

import groovy.util.GroovyTestCase

class WGrepTest extends GroovyTestCase
{
    WgrepFacade facade = null
    def BASE_HOME = System.getProperty("wgrep.home")
    def HOME = BASE_HOME + "\\build\\resources\\test"

    void setUp() 
    {
        facade = WgrepFacade.getInstance([BASE_HOME + "\\build\\resources\\test\\config.xml"])
    }
    
    void cleanUp()
    {
        //GENERAL
        facade.LOG_ENTRY_PATTERN = null
        facade.LEP_OVERRIDED = null
        facade.FILTER_PATTERN = null
        facade.FP_OVERRIDED = null
        facade.FILES = []

        //OPTIONS
        facade.VERBOSE = null
        facade.TRACE = null
        facade.SPOOLING = null
        facade.FILE_MERGING = null
        facade.ATMTN_LEVEL = null
        facade.EXTNDD_PATTERN = null
        facade.PRESERVE_THREAD = null
        facade.POST_PROCESSING = null
        facade.DATE_TIME_FILTER = null
        facade.paHelper = null
        facade.PREDEF_TAG = null
        facade.additionalVarParsers = []
        facade.fProcessor = null


    }
    
    void testMainVarsProcessing()
    {
        cleanUp()
        facade.processInVars(["-","test","test",HOME+"\\fpTest*"])
        assertTrue( facade.LOG_ENTRY_PATTERN == "test" )
        assertTrue( facade.FILTER_PATTERN == "test" )
        assertTrue( facade.FILES == [HOME+"\\fpTest_test.log"] )
        assertTrue( facade.FOLDER_SEPARATOR == "\\\\" )
        assertTrue( facade.HOME_DIR != null )
    }

    void testFailAutomationProcessing()
    {
        cleanUp()
        shouldFail(java.lang.IllegalArgumentException)
        {
            facade.processInVars(["-i","test", HOME+"\\test*"])
        }
    }
    
    void testExtendedPatternProcessing()
    {
        cleanUp()
        facade.processInVars(["-e","test","test%and%tets",HOME+"\\test*"])
        assertTrue( facade.EXTNDD_PATTERN == 'e')
        assertTrue( facade.FILTER_PATTERN == "test%and%tets" )
    }
    
    void testComplexVarsProcessing()
    {
        cleanUp()
        facade.processInVars(["-t","test","test","--dtime", "2013-01-25T12:00:00", "+", HOME+"\\test*"])
        assertTrue( facade.DATE_TIME_FILTER == "dtime" )
    }

    void testAutomationProcessing()
    {
        cleanUp()
        facade.processInVars(["-i","test", HOME+"\\test_*"])
        assertTrue( facade.LOG_ENTRY_PATTERN == /####\[\D{1,}\].*(\d{4}-\d{1,2}-\d{1,2} \d{2}:\d{2}:\d{2})/)
        assertTrue( facade.getExtraParam('LOG_DATE_FORMAT') == "yyyy-MM-dd HH:mm:ss" )
    }

    void testMoreComplexVarsProcessing()
    {
        cleanUp()
        facade.processInVars(["-ts", "stCommand", "queryTime", "--some_timings", "cmd_only_1.log"])
        assertTrue( facade.LOG_ENTRY_PATTERN == "stCommand" )
        assertTrue( facade.FILTER_PATTERN == "queryTime" )
        assertTrue( facade.FILES == ["cmd_only_1.log"] )
        assertTrue( facade.FOLDER_SEPARATOR == "\\\\" )
        assertTrue( facade.HOME_DIR != null )
    }

    void testComplexFiltering()
    {
        cleanUp()
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
        cleanUp()
        facade.processInVars(["-ae", "Foo%and%Man Chu", HOME+"\\processing_test.log"])
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
2012-10-20 05:05:56,951 [ACTIVE] ThreadStart: '1' 
Foo Man Chu
#basic
"""
    
        assertTrue( expectedResult == actualResult.toString() )
    }

    void testBasicFiltering()
    {
        cleanUp()
        facade.processInVars(["-a", "Foo", HOME+"\\processing_test.log"])
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
        cleanUp()
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
    
        assertTrue( expectedResult == actualResult.toString() )
    }
}
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
        assertTrue( facade.LOG_ENTRY_PATTERN == /####\[\D{1,}\].*\d{4}-\d{1,2}-\d{1,2} \d{2}:\d{2}:\d{2}/)
        assertTrue( facade.getExtraParam('LOG_DATE_FORMAT') == "yyyy-MM-dd HH:mm:ss" )
    }

    void testMoreComplexVarsProcessing()
    {
        cleanUp()
        facade.processInVars(["-ts", "stCommand", "queryTime", "--some_timings", "cmd_only_1.log"   ])
        assertTrue( facade.LOG_ENTRY_PATTERN == "stCommand" )
        assertTrue( facade.FILTER_PATTERN == "queryTime" )
        assertTrue( facade.FILES == ["cmd_only_1.log"] )
        assertTrue( facade.FOLDER_SEPARATOR == "\\\\" )
        assertTrue( facade.HOME_DIR != null )
    }

    void testFiltering()
    {
        cleanUp()
        facade.processInVars(["-i", "Foo", HOME+"\\processing_test.log"])
        facade.startProcessing()
    }
}
import groovy.xml.dom.DOMCategory
import org.smlt.tools.wgrep.*
import java.text.SimpleDateFormat
import java.util.regex.Matcher

def BASE_HOME = System.getProperty("wgrep.home")
def WGREP_CONFIG = BASE_HOME + "\\build\\resources\\test\\config.xml"
def HOME = BASE_HOME + "\\build\\resources\\test"

WgrepFacade facade = WgrepFacade.getInstance([WGREP_CONFIG])
//def fileTime = new Date(new File(HOME+"\\processing_time_test.log").lastModified())
//def dateFormat = new SimpleDateFormat('yyyy-MM-dd');

//def line = "####<Dec 1, 2012 7:04:01 AM NZDT> <Info> <com.netcracker.solutions.tnz.cwms.soap.incoming.CWMS.SYNCHRONIZATION.NotifyScheduleUpdate> <hp3463.telecom.co.nz> <clust1> <QuartzScheduler_Worker-9> <Administrator> <BEA1-7618E3AE535BF3A3456D> <> <1354298641286> <000000> <WF<> INFO  com.netcracker.solutions.tnz.cwms.soap.incoming.CWMS.SYNCHRONIZATION.NotifyScheduleUpdate - Correlation Key=1354298640677_787296146; Procesing Time=608;"
//def pattern = /####<(... \d{1,2}, \d{4} \d{1,2}:\d{1,2}:\d{1,2} ..)/
//Matcher entryMtchr = line =~ pattern
//println entryMtchr.find()
//println entryMtchr.toString()
//println entryMtchr.group()
//println entryMtchr.groupCount()
        def fileTime = new Date(new File(HOME+"\\processing_time_test.log").lastModified())
        def dateFormat = new SimpleDateFormat('yyyy-MM-dd')
        def testTimeString = dateFormat.format(fileTime)
facade.processInVars(["-a", "Foo", "--dtime", testTimeString+"T05", testTimeString+"T06", HOME+"\\processing_time_test.log"])

//def oldStdout = System.out
//def pipeOut = new PipedOutputStream()
//def pipeIn = new PipedInputStream(pipeOut)
//System.setOut(new PrintStream(pipeOut))
facade.startProcessing()
//def outputReader = new BufferedReader(new InputStreamReader(pipeIn))
//System.setOut(oldStdout)
//pipeOut.close()

//def line = '#'
//StringBuffer actualResult = new StringBuffer()

//if (outputReader.ready())
//{
//    while (line != null)
//    {
//        actualResult = actualResult.append(line).append('\n')
//        line = outputReader.readLine()
//    }
//}

def expectedResult = """\
#
some_cmd,count_of_operands
Foo,3
Koo,1
Boo

"""

//println actualResult.toString()
//println expectedResult == actualResult.toString()
//println facade.LOG_ENTRY_PATTERN
//println facade.FILTER_PATTERN 
//println facade.FILES 
//facade.startProcessing()
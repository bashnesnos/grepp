import groovy.xml.dom.DOMCategory
import org.smlt.tools.wgrep.*
import java.text.SimpleDateFormat
import java.util.regex.Matcher

def BASE_HOME = System.getProperty("wgrep.home")
//def WGREP_CONFIG = BASE_HOME + "\\build\\resources\\test\\config.xml"
def WGREP_CONFIG = BASE_HOME + "\\dev\\config.xml"
def HOME = BASE_HOME + "\\build\\resources\\test"

WgrepFacade facade = WgrepFacade.getInstance([WGREP_CONFIG])

facade.processInVars(["-ae", "incoming.CWMS.SYNCHRONIZATION.NotifyScheduleUpdate%and%Procesing", "--nbup_times", "--dtime", "2012-12-01T07", "2012-12-01T18", "D:\\alse\\20130320\\clust*.log*"])

//facade.getFileProcessor().getFiles().each { println it.name }
        //println actualResult.toString()
        //println expectedResult == actualResult.toString() 

import org.smltools.grepp.Grepp
import org.smltools.grepp.config.XMLConfigHolder
import java.util.Date
import java.text.SimpleDateFormat

def BASE_HOME = System.getProperty("grepp.home")
def HOME = BASE_HOME + "\\build\\resources\\main\\config"

//		def fileTime = new Date(new File(HOME+"\\processing_time_test.log").lastModified())
//		def dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH")
//		def logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH")
//		def testTimeStringTo = dateFormat.format(new Date(fileTime.getTime() + 60*60*1000))
//
//
//Grepp.main("-t --dtime +;$testTimeStringTo --foo $HOME\\processing_time_test.log".split(" "))

println new XMLConfigHolder("$HOME\\config.xml", "$HOME\\config.xsd").savedConfigs

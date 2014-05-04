import org.smltools.grepp.Grepp
import org.smltools.grepp.config.XMLConfigHolder
import java.util.Date
import java.text.SimpleDateFormat

def BASE_HOME = System.getProperty("grepp.home")
<<<<<<< Updated upstream
def HOME = BASE_HOME + "\\build\\resources\\test"
def WGREP_CONFIG = BASE_HOME + "\\build\\resources\\test\\config.xml"
def WGREP_CONFIG_XSD = BASE_HOME + "\\build\\resources\\main\\config\\config.xsd"
//def defalutOut = System.out

def	config = new ConfigHolder(WGREP_CONFIG, WGREP_CONFIG_XSD) 
println "${config.getOptions()}"

//config.withRoot{ root ->
//	println root.options.opt[0].text()
//}
//PredictingParamsHolderFactory paramFactory = new PredictingParamsHolderFactory(config)
//def params = paramFactory.getParamsHolder("-tsL stCommand queryTime --some_timings cmd_only_1.log".split(" "))

//Grepp.main("-tf --avg_timings $HOME\\processing_report_test.log".split(" "))

//def fileTime = new Date(new File(HOME+"\\processing_time_test.log").lastModified())
//def dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
//def testTimeStringFrom = dateFormat.format(new Date(fileTime.getTime() + 24*60*60*1000))
//
//def expectedResult = ""
//Grepp.main("-t Foo --dtime $testTimeStringFrom + $HOME\\processing_time_test.log".split(" "))


 
=======
def HOME = (BASE_HOME.equals("") ? "C:\\Users\\asemelit\\Projects\\grepp" : BASE_HOME) + "\\build\\resources\\main\\config"

//		def fileTime = new Date(new File(HOME+"\\processing_time_test.log").lastModified())
//		def dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH")
//		def logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH")
//		def testTimeStringTo = dateFormat.format(new Date(fileTime.getTime() + 60*60*1000))
//
//
//Grepp.main("-t --dtime +;$testTimeStringTo --foo $HOME\\processing_time_test.log".split(" "))

println new XMLConfigHolder("$HOME\\config.xml", "$HOME\\config.xsd").savedConfigs
>>>>>>> Stashed changes

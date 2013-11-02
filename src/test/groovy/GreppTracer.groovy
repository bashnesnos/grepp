import org.smltools.grepp.config.PredictingParamsHolderFactory
import org.smltools.grepp.config.ConfigHolder
import org.smltools.grepp.config.Param
import org.smltools.grepp.Grepp

def BASE_HOME = System.getProperty("grepp.home")
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

 
import org.smltools.grepp.cli.Grepp
import org.smltools.grepp.config.ConfigHolder
import java.util.Date
import java.text.SimpleDateFormat
import org.smltools.grepp.filters.entry.PropertiesFilter
import groovy.xml.MarkupBuilder
import org.smltools.grepp.cli.CLIFacade

def BASE_HOME = System.getProperty("grepp.home")
//def HOME = BASE_HOME + "\\build\\resources\\main\\config"
def HOME = BASE_HOME + "\\build\\resources\\test"
def GREPP_CONFIG = BASE_HOME + "\\build\\resources\\main\\config\\config.groovy"
//ConfigHolder config = new ConfigHolder(new URL('file', '/', GREPP_CONFIG))
//CLIFacade facade = new CLIFacade(config);

//def options = facade.parseOptions("-t -e Foo $HOME\\processing_test.log".split(" "))
//println options.m
//def runtimeConfig = facade.makeRuntimeConfig()
//def entryFilterChain = facade.makeEntryFilterChain(runtimeConfig, options)
//println runtimeConfig
//		def fileTime = new Date(new File(HOME+"\\processing_time_test.log").lastModified())
//		def dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH")
//		def logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH")
//		def testTimeStringTo = dateFormat.format(new Date(fileTime.getTime() + 60*60*1000))
//
//
//Grepp.main("-t --dtime +;$testTimeStringTo --foo $HOME\\processing_time_test.log".split(" "))
//Grepp.main("-h".split(" "))
//Grepp.main("-t --some_timings $HOME\\processing_report_test.log".split(" "))
//Grepp.main("-t --parse $HOME\\test.properties".split(" "))

Grepp.main("-t --parse $HOME\\test.properties".split(" "))


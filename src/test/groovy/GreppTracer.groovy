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
ConfigHolder config = new ConfigHolder(new File(GREPP_CONFIG).toURI().toURL())
CLIFacade facade = new CLIFacade(config);

		def fileTime = new Date(new File(HOME+"\\processing_time_test.log").lastModified())
		def dateFormat = new SimpleDateFormat("yyyy-MM-dd")
		def testTimeStringFrom = dateFormat.format(new Date(fileTime.getTime() + 24*60*60*1000))


def options = facade.parseOptions((String[]) ["--add", "myconfig", "--threadProp", "Thread [\\d{2}];;the end", "--dateProp", "yyyy-MM-dd;(\\d{4}-\\d{2}-\\d{2})", "-e", "-d", "$testTimeStringFrom;+", "-l", "Foo", "oo", "$HOME\\processing_time_test.log"])

		def runtimeConfig = facade.makeFilterChains(facade.makeRuntimeConfig(), options)
		def entryFilterChain = runtimeConfig.entryFilterChain
		def fileFilterChain = runtimeConfig.fileFilterChain

		if (runtimeConfig.data.containsKey('files')) {
        	List<File> filteredData = fileFilterChain.filter(runtimeConfig.data.files)
			if (filteredData != null) {
				runtimeConfig.data.files = filteredData
			}
			else {
				return //nothing to process
			}
		}		

		if (options.add) {
			if (entryFilterChain.configIdExists(options.add) || fileFilterChain.configIdExists(options.add)) {
				println "ConfigId $options.add already exists for a given filter chain; try different one"
				return
			}
		}

		if (options.add) {
			//log.info("Saving config to {}", options.add)
			config.merge(entryFilterChain.getAsConfig(options.add))
			config.merge(fileFilterChain.getAsConfig(options.add))
			config.save()
		}

		config = new ConfigHolder(new File(GREPP_CONFIG).toURI().toURL())
		facade = new CLIFacade(config);
		options = facade.parseOptions("--lock -e -d $testTimeStringFrom;+ --myconfig $HOME\\processing_time_test.log".split(" "))
		runtimeConfig = facade.makeFilterChains(facade.makeRuntimeConfig(), options)
		entryFilterChain = runtimeConfig.entryFilterChain
		fileFilterChain = runtimeConfig.fileFilterChain
		def tempConfig = entryFilterChain.getAsConfig(null)
		tempConfig.merge(fileFilterChain.getAsConfig(null))
		println tempConfig

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

//Grepp.main("-d 2012;2013;yyyy-MM-dd;'(\d{4}-\d{2}-\d{2})' -l test test $HOME\\test.properties".split(" "))




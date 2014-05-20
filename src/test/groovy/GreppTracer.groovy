import org.smltools.grepp.cli.Grepp
import org.smltools.grepp.config.ConfigHolder
import java.util.Date
import java.text.SimpleDateFormat
import org.smltools.grepp.filters.entry.PropertiesFilter
import groovy.xml.MarkupBuilder
import org.smltools.grepp.cli.CLIFacade
import java.lang.reflect.*
import org.smltools.grepp.filters.StringAggregator
import org.smltools.grepp.filters.FilterChain

def HOME = System.getProperty("grepp.home")
def GREPP_CONFIG = System.getProperty("grepp.config")
ConfigHolder config = new ConfigHolder(new File(GREPP_CONFIG).toURI().toURL())
CLIFacade facade = new CLIFacade(config);
		def options = facade.parseOptions("--to_test --predef $HOME\\fpTest*".split())
		def runtimeConfig = facade.makeRuntimeConfig()
		def entryFilterChain = facade.makeFilterChains(runtimeConfig, options).entryFilterChain
		def newConfig = entryFilterChain.getAsConfig(null)
println newConfig
//def options = facade.parseOptions("--repProp group(command=\"(.*?)\",cmd);counter((operand),count_of_operand) oo $RESOURCES\\processing_report_test.log".split(" "))
//def runtimeConfig = facade.makeRuntimeConfig()
//facade.makeFilterChains(runtimeConfig, options)
//println runtimeConfig.entryFilterChain.getAsConfig('new_report')

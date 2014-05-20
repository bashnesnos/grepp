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

def BASE_HOME = System.getProperty("grepp.home")
def GREPP_CONFIG = System.getProperty("grepp.config")
ConfigHolder config = new ConfigHolder(new File(GREPP_CONFIG).toURI().toURL())
CLIFacade facade = new CLIFacade(config);
println FilterChain.getConfigIdToFilterClassMap(config).collect { configId, classes -> 
	String.format("--%-20s\t\t\tmay configure: %s", configId, classes.collect { it.simpleName }.join(','))
}.join('\n')
//def options = facade.parseOptions("--repProp group(command=\"(.*?)\",cmd);counter((operand),count_of_operand) oo $RESOURCES\\processing_report_test.log".split(" "))
//def runtimeConfig = facade.makeRuntimeConfig()
//facade.makeFilterChains(runtimeConfig, options)
//println runtimeConfig.entryFilterChain.getAsConfig('new_report')

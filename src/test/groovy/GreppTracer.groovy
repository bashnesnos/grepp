import org.smltools.grepp.cli.Grepp
import org.smltools.grepp.config.ConfigHolder
import java.util.Date
import java.text.SimpleDateFormat
import org.smltools.grepp.filters.entry.PropertiesFilter
import groovy.xml.MarkupBuilder
import org.smltools.grepp.cli.CLIFacade
import org.smltools.grepp.filters.entry.ThreadFilter
import java.lang.reflect.*
import org.smltools.grepp.filters.StringAggregator
import org.smltools.grepp.filters.FilterChain

def BASE_HOME = System.getProperty("grepp.home")
//def HOME = BASE_HOME + "\\build\\resources\\main\\config"
def RESOURCES = BASE_HOME + "\\build\\resources\\test"
def GREPP_CONFIG = BASE_HOME + "\\build\\resources\\main\\config\\config.groovy"
ConfigHolder config = new ConfigHolder(new File(GREPP_CONFIG).toURI().toURL())
CLIFacade facade = new CLIFacade(config);
def options = facade.parseOptions("--repProp group(command=\"(.*?)\",cmd);counter((operand),count_of_operand) oo $RESOURCES\\processing_report_test.log".split(" "))
def runtimeConfig = facade.makeRuntimeConfig()
facade.makeFilterChains(runtimeConfig, options)
println runtimeConfig.entryFilterChain.getAsConfig('new_report')

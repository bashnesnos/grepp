import org.smltools.grepp.cli.CLIFacade
import org.smltools.grepp.cli.Grepp
import org.smltools.grepp.config.ConfigHolder
import java.util.Date
import java.text.SimpleDateFormat
import org.smltools.grepp.filters.entry.PropertiesFilter
import groovy.xml.MarkupBuilder
import org.smltools.grepp.util.PropertiesParserFactory
import org.smltools.grepp.output.SimpleOutput
import org.smltools.grepp.filters.StringAggregator
import org.smltools.grepp.filters.FilterChain
import java.util.regex.Pattern

def HOME = System.getProperty("grepp.home")
def GREPP_CONFIG = System.getProperty("grepp.config")
//ConfigHolder config = new ConfigHolder(new File(GREPP_CONFIG).toURI().toURL())
//CLIFacade facade = new CLIFacade(config);
//		def options = facade.parseOptions("--to_test --predef $HOME\\fpTest*".split())
//		def runtimeConfig = facade.makeRuntimeConfig()
//		def entryFilterChain = facade.makeFilterChains(runtimeConfig, options).entryFilterChain
//		def newConfig = entryFilterChain.getAsConfig(null)
//println newConfig


def propParser = PropertiesParserFactory.getParserInstanceById('logback')
propParser.setGreppOutput(new SimpleOutput<String>(null, null, new PrintWriter(System.out, true)))
propParser.process([new File("$HOME\\config\\logback.xml")])
//Grepp.main("--parse logback $HOME\\config\\logback.xml".split(" "))

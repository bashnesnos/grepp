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
def HOME = BASE_HOME + "\\build\\resources\\test"
def GREPP_CONFIG = BASE_HOME + "\\build\\resources\\main\\config\\config.groovy"
ConfigHolder config = new ConfigHolder(new File(GREPP_CONFIG).toURI().toURL())
def findSuperclassParameter = { clazz ->
	def superclazz = clazz.getGenericSuperclass()
	if (superclazz != null) {
		if (superclazz instanceof ParameterizedType) {
			return superclazz.getActualTypeArguments().find {it instanceof Class}
		}
		else {
			return findSuperclassParameter(superclazz)
		}
	}
	else {
		return null
	}
}

def findParameterClass = { clazz ->
	if (clazz instanceof ParameterizedType) {
		println "itself"
		return clazz.getTypeParameters().findResult { it.getBounds().findResult { if (!Object.class.equals(it)) it } }
	}
	else {
		def parametrizedInterface = clazz.getGenericInterfaces().find { it instanceof ParameterizedType }
		if (parametrizedInterface != null) {
			println "interface"
			return parametrizedInterface.getActualTypeArguments().find {it instanceof Class}
		}
		else {
			println "super"
			return findSuperclassParameter(clazz)	
		}
	}
}
def myclazz = new FilterChain(config, new StringAggregator()).getClass()
myclazz.getTypeParameters().each { println it.getBounds() }
//def parameter = findParameterClass(myclazz)
//println parameter
//println (String.class.equals(parameter))


//CLIFacade facade = new CLIFacade(config);
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




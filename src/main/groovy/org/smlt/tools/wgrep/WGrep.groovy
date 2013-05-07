package org.smlt.tools.wgrep

/**
* CLI program to analyze text files in a regex manner. Adding a feature of a log record splitting, thread-coupling and reporting.
* <p>
* Relies on the config.xml, i.e. it is based on the rule 'declare-first'. Could be used with default config.xml, but that would require more typing. <br>
* If analysis is needed frequently, it is always better to declare those patterns inside config.xml.
* 
* @author Alexander Semelit
*/
import groovy.util.logging.Slf4j
import org.smlt.tools.wgrep.config.WgrepConfig
import org.smlt.tools.wgrep.util.WgrepUtil

@Slf4j
class WGrep 
{

	public static void main(String[] args)
	{
		Date startTime = new Date()
		log.info(args.toString())
		if (args == null || args.size() < 1) return
		
		def argsToParse = args
		def WGREP_CONFIG = argsToParse[0]
		def WGREP_CONFIG_XSD = null

		if (WGREP_CONFIG =~ /config.xml$/) {
			if (args.size() > 1) {
				argsToParse = args[1..args.size() - 1] //excluding config.xml file path from parameters		
			}
			else {
				println "Nothing to do except config parsing"
				return //only config.xml was passed
			}
		}
		else {
			WGREP_CONFIG = WgrepUtil.getResourcePathOrNull("config.xml") //looking in the classpath
		}

		if (WGREP_CONFIG == null){ // it has not been found
			println "config.xml should be either in classpath or specified explicitly"
			return
		}

		WGREP_CONFIG_XSD = WgrepUtil.getResourcePathOrNull("config.xsd") //looking in the classpath
		if (WGREP_CONFIG_XSD == null){ //xsd has not been found 
			log.warn("config.xsd cannot be found in the classpath. Validation will be skipped")
		}

		WgrepConfig config = new WgrepConfig(WGREP_CONFIG, WGREP_CONFIG_XSD)
		WgrepFacade facade = new WgrepFacade(config)
		facade.doProcessing(argsToParse)
		log.info("Processing time = " + ((new Date().getTime() - startTime.getTime())/1000)) + " sec"
	}


}

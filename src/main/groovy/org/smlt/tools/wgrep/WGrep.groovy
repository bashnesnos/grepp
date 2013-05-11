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
import org.springframework.beans.factory.xml.XmlBeanFactory
import org.springframework.core.io.ClassPathResource


@Slf4j
class WGrep 
{
	public static void main(String[] args)
	{
		Date startTime = new Date()
		log.info("{}", args)
		if (args == null || args.size() < 1) return

		ClassPathResource springConfig = new ClassPathResource("wgrep-context.xml");
		XmlBeanFactory beanFactory = new XmlBeanFactory(springConfig);

		def argsToParse = args
		def WGREP_CONFIG = argsToParse[0]

		if (WGREP_CONFIG =~ /config.xml$/) {
			if (args.size() > 1) {
				argsToParse = args[1..args.size() - 1] //excluding config.xml file path from parameters		
			}
			else {
				println "Nothing to do except config parsing"
				return //only config.xml was passed
			}
			beanFactory.getBean("configInstance").loadConfig(WGREP_CONFIG)
		}

		def facade = beanFactory.getBean("wgrepFacade")
		facade.doProcessing(argsToParse)
		log.info("Processing time = {} sec", ((new Date().getTime() - startTime.getTime())/1000))
	}


}

package org.smltools.grepp.cli

/**
* CLI program to analyze text files in a regex manner. Adding a feature of a log record splitting, thread-coupling and reporting.
* <p>
* Relies on the config.xml, i.e. it is based on the rule 'declare-first'. Could be used with default config.xml, but that would require more typing. <br>
* If analysis is needed frequently, it is always better to declare those patterns inside config.xml.
* 
* @author Alexander Semelit
*/
import org.smltools.grepp.config.ConfigHolder
import org.smltools.grepp.cli.CLIFacade
import org.smltools.grepp.util.GreppUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Grepp {
	public static void main(String[] args) {
		Logger log = LoggerFactory.getLogger('Grepp')
		Date startTime = new Date()
		log.info("{}", args)
		if (args == null || args.size() < 1) return

		ConfigHolder configHolder = null
		def configPath = GreppUtil.getResourceOrNull("config.groovy");
		if (configPath != null) {
			configHolder = new ConfigHolder(configPath)
		}
		else {
			System.err.println("Can't find config file to initialize")
			System.exit(1)
		}

		try {
			CLIFacade facade = new CLIFacade(configHolder)
			facade.process(args)
		}
		catch(Exception e)
		{
			log.error("An unexpected exception occured", e)
			System.err.println("Error occured")
			System.exit(1)
		}
				
		log.info("Processing time = {} sec", ((new Date().getTime() - startTime.getTime())/1000))
	}


}

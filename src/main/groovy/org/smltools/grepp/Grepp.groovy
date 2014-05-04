package org.smltools.grepp

/**
* CLI program to analyze text files in a regex manner. Adding a feature of a log record splitting, thread-coupling and reporting.
* <p>
* Relies on the config.xml, i.e. it is based on the rule 'declare-first'. Could be used with default config.xml, but that would require more typing. <br>
* If analysis is needed frequently, it is always better to declare those patterns inside config.xml.
* 
* @author Alexander Semelit
*/
import org.smltools.grepp.processors.DataProcessorFactory
import org.smltools.grepp.config.XMLConfigHolder;
import org.smltools.grepp.config.ParamHolderFactory;
import org.smltools.grepp.config.CLIParamHolderFactory
import org.smltools.grepp.util.GreppUtil
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Grepp 
{
	public static void main(String[] args)
	{
		Logger log = LoggerFactory.getLogger('Grepp')
		Date startTime = new Date()
		log.info("{}", args)
		if (args == null || args.size() < 1) return

		def configPath = GreppUtil.getResourcePathOrNull("config.xml");
		def configXSDPath = GreppUtil.getResourcePathOrNull("config.xsd");
		
		if (configPath == null) {
			throw new IllegalArgumentException("Config file should present in classpath or specified explicitly")
		}
		log.trace("Initializing: {}, {}", configPath, configXSDPath)
		
		try {
			def configHolder = new XMLConfigHolder(configPath, configXSDPath)
			ParamHolderFactory<List<String>> paramFactory = new CLIParamHolderFactory(configHolder)
			ParamHolder paramHolder = paramFactory.getParamHolder(args)
			DataProcessorFactory.process(paramHolder)
		}
		catch(Exception e)
		{
			log.error("An unexpected exception occured", e)
		}
				
		log.info("Processing time = {} sec", ((new Date().getTime() - startTime.getTime())/1000))
	}


}

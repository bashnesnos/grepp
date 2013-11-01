package org.smltools.grepp

/**
* CLI program to analyze text files in a regex manner. Adding a feature of a log record splitting, thread-coupling and reporting.
* <p>
* Relies on the config.xml, i.e. it is based on the rule 'declare-first'. Could be used with default config.xml, but that would require more typing. <br>
* If analysis is needed frequently, it is always better to declare those patterns inside config.xml.
* 
* @author Alexander Semelit
*/
import groovy.util.logging.Slf4j

import org.codehaus.groovy.classgen.Verifier.DefaultArgsAction;
import org.codehaus.groovy.transform.NewifyASTTransformation;
import org.smltools.grepp.config.ConfigHolder;
import org.smltools.grepp.config.ParamsHolder
import org.smltools.grepp.config.ParamsHolderFactory;
import org.smltools.grepp.config.PredictingParamsHolderFactory
import org.smltools.grepp.processors.DataProcessorFactory
import org.smltools.grepp.filters.entry.LogEntryFilter;
import org.smltools.grepp.util.GreppUtil

@Slf4j
class Grepp 
{
	public static void main(String[] args)
	{
		Date startTime = new Date()
		log.info("{}", args)
		if (args == null || args.size() < 1) return

		def configPath = GreppUtil.getResourcePathOrNull("config.xml");
		def configXSDPath = GreppUtil.getResourcePathOrNull("config.xsd");
		
		if (configPath == null) {
			throw new IllegalArgumentException("Config file should present in classpath or specified explicitly")
		}
		log.trace("Initializing: {}, {}", configPath, configXSDPath)
		def configHolder = new ConfigHolder(configPath, configXSDPath)
		
		try {
			ParamsHolderFactory<?> paramsFactory = configHolder.getParamsHolderFactoryClass().getConstructor(ConfigHolder.class).newInstance(configHolder)
			ParamsHolder paramsHolder = paramsFactory.getParamsHolder(args)
			DataProcessorFactory.process(paramsHolder)
		}
		catch(Exception e)
		{
			log.error("An unexpected exception occured", e)
		}
				
		log.info("Processing time = {} sec", ((new Date().getTime() - startTime.getTime())/1000))
	}


}

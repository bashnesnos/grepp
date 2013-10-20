package org.smltools.grepp

import groovy.util.logging.Slf4j
import org.smltools.grepp.processors.DataProcessorFactory
import org.smltools.grepp.processors.DataProcessor
import org.smltools.grepp.config.ParamsHolderFactory;
import org.smltools.grepp.config.ParamsHolder;

/**
 * A Facade linking config and modules. Provides facade methods to do the processing.
 *
 * @author Alexander Semelit 
 */
@Slf4j
public class GreppFacade {
	
	private ParamsHolderFactory<?> paramsHolderFactory

	/**
	 * Returns <code>DataProcessorFactory</code> instance associated with this facade.
	 * @return <code>DataProcessorFactory</code>
	 */

	ParamsHolderFactory<?> getParamsHolderFactory()
	{
		return paramsHolderFactory
	}

	void setParamsHolderFactory(ParamsHolderFactory<?> paramsHolderFactory_)
	{
		paramsHolderFactory = paramsHolderFactory_
	}

	//General

	/**
	 * Method for processing in CLI-style
	 * Sequence is the following:
	 * <li>1. Passes supplied arguments to the <code>configInstance</code></li>
	 * <li>2. Performs validation via <code>check</code> method</li>
	 * <li>3. Calls processing method of initialized a DataProcessor given by DataProcessorFactory</li>
	 * @param args Command-line style arguments
	 */

	public void doCLProcessing(String[] args)
	{
		try {
			ParamsHolder paramsHolder = paramsHolderFactory.getParamsHolder(args)
			DataProcessorFactory.process(paramsHolder)
		}
		catch(Exception e)
		{
			log.error("An unexpected exception occured", e)
		}
	}

}
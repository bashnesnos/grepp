package org.smlt.tools.wgrep

import groovy.util.logging.Slf4j
import org.smlt.tools.wgrep.processors.DataProcessorFactory
import org.smlt.tools.wgrep.processors.DataProcessor
import org.smlt.tools.wgrep.config.ParamsHolderFactory;
import org.smlt.tools.wgrep.config.ParamsHolder;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A Facade linking config and modules. Provides facade methods to do the processing.
 *
 * @author Alexander Semelit 
 */
@Slf4j
public class WgrepFacade {
	
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

	public void doCLProcessing(def args)
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
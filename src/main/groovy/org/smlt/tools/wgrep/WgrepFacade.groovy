package org.smlt.tools.wgrep

import groovy.util.logging.Slf4j
import org.smlt.tools.wgrep.processors.DataProcessorFactory
import org.smlt.tools.wgrep.config.ModuleBase;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A Facade linking config and modules. Provides facade methods to do the processing.
 *
 * @author Alexander Semelit 
 */
@Slf4j
class WgrepFacade extends ModuleBase {
	
	@Autowired
	private DataProcessorFactory dataProcessorFactory

	/**
	 * Returns <code>DataProcessorFactory</code> instance associated with this facade.
	 * @return <code>DataProcessorFactory</code>
	 */

	DataProcessorFactory getDataProcessorFactory()
	{
		return dataProcessorFactory
	}

	void setDataProcessorFactory(DataProcessorFactory processorFactory)
	{
		dataProcessorFactory = processorFactory
	}

	//General

	/**
	 * Method for processing in CLI-style
	 * Sequence is the following:
	 * <li>1. Passes supplied arguments to the <code>configInstance</code></li>
	 * <li>2. Performs validation via <code>check</code> method</li>
	 * <li>3. Calls processing method of initialized FileProcessor</li>
	 * @param args Command-line style arguments
	 */

	void doCLProcessing(def args)
	{
		try {
			configInstance.processInVars(args)
			if (!check(['FILES'], null)) {
				return
			}
			dataProcessorFactory.getProcessorInstance().process(getParam('FILES'))
		}
		catch(Exception e)
		{
			log.error("An unexpected exception occured", e)
		}
	}

}
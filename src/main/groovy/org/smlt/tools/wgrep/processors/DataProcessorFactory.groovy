package org.smlt.tools.wgrep.processors;

import org.smlt.tools.wgrep.config.ModuleBase
import org.smlt.tools.wgrep.filters.FilterChainFactory
import org.smlt.tools.wgrep.output.OutputFactory
import org.springframework.beans.factory.annotation.Autowired;

class DataProcessorFactory extends ModuleBase {
	
	@Autowired
	private OutputFactory outputFactory

	OutputFactory getOutputFactory()
	{
		return outputFactory
	}

	void setOutputFactory(OutputFactory outputFactory_)
	{
		outputFactory = outputFactory_
	}

	/**
	 * Simple factory method, intializes appropriate processors judging by configInstance.       
	 *        
	 * @return new DataProcessor instance
	 */
    DataProcessor<?> getProcessorInstance() 
    {
		if (check(['FILES'], null)) {
			return new FileProcessor(outputFactory.getOutputInstance(), outputFactory.getFilterFactory().createFileFilterChain(), !checkParamIsEmpty('FILE_MERGING'))
		}
		else {
			return new InputStreamProcessor(outputFactory.getOutputInstance())
		}
		
    }
}
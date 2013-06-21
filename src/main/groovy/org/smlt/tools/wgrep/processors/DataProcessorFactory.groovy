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
	 * Simple factory method, which takes WgrepConfig instance and initializes appropriate filter chains.       
	 *        
	 * @param config WgrepConfig instance
	 * @return new FileProcessor instance
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
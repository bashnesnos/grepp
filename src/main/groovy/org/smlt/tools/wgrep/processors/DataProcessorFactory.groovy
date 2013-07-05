package org.smlt.tools.wgrep.processors;

import org.smlt.tools.wgrep.config.ModuleBase
import org.smlt.tools.wgrep.filters.FilterChainFactory
import org.smlt.tools.wgrep.output.OutputFactory
import org.smlt.tools.wgrep.output.WgrepOutput
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
    DataProcessor<?> getProcessorInstance(Object data) 
    {

		final WgrepOutput<?,?> output = outputFactory.getOutputInstance()
		if (data instanceof List<?>) {
			if (((List<?>)data).get(0) instanceof File) {
				return new FileProcessor(output, outputFactory.getFilterFactory().createFileFilterChain(), !checkParamIsEmpty('FILE_MERGING'))
			}
		}
		else if (data instanceof InputStream){
			return new InputStreamProcessor(output)
		}
		else {
			return new DataProcessor<Object>() {
				private final WgrepOutput<?,?> someOutput = output 
				public void process(Object pData) {
					if (pData != null) {
						someOutput.println("Couldn't find DataProcessor for " + pData.getClass().toString())
					}
					else {
						someOutput.println("Nothing to process")
					}
				}
			}
		}
    }
}
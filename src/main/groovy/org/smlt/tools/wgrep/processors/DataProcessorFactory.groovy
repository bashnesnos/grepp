package org.smlt.tools.wgrep.processors;

import org.smlt.tools.wgrep.config.ParamsHolder
import org.smlt.tools.wgrep.config.Param
import org.smlt.tools.wgrep.filters.FilterChainFactory
import org.smlt.tools.wgrep.output.OutputFactory
import org.smlt.tools.wgrep.output.WgrepOutput


class DataProcessorFactory {


	/**
	 * Simple factory method, intializes appropriate processors judging by configInstance.       
	 *        
	 * @return new DataProcessor instance
	 */
    public static DataProcessor<?> getProcessorInstance(ParamsHolder paramsHolder) 
    {
		final WgrepOutput<?,?> output = OutputFactory.getOutputInstance(paramsHolder)
		def data = paramsHolder.getProcessingData()
		if (data instanceof List<?>) {
			if (((List<?>)data).get(0) instanceof File) {
				return new FileProcessor(output, FilterChainFactory.createFileFilterChain(paramsHolder), !paramsHolder.checkParamIsEmpty(Param.FILE_MERGING))
			}
		}
		else if (data instanceof InputStream){
			return new InputStreamProcessor(output)
		}
		else {
			return new DataProcessor<Object>() {
				public void process(Object pData) {
					if (pData != null) {
						System.out.println("Couldn't find DataProcessor for " + pData.getClass().toString())
					}
					else {
						System.out.println("Nothing to process")
					}
				}
			}
		}
    }
	
	public static void process(ParamsHolder paramsHolder) {
		getProcessorInstance(paramsHolder).process(paramsHolder.getProcessingData())
	}
	
}
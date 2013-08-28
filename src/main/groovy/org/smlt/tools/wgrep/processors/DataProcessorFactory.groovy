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
		def data = paramsHolder.getProcessingData()
		return getProcessorInstanceForData(paramsHolder, data)
    }
	
	public static void process(ParamsHolder paramsHolder) {
		def data = paramsHolder.getProcessingData()
		getProcessorInstanceForData(paramsHolder, data).process(data)
	}
	
	public static <T> DataProcessor<T> getProcessorInstanceForData(ParamsHolder paramsHolder, T data) {
		final WgrepOutput<?,?> output = OutputFactory.getOutputInstance(paramsHolder)
		
		if (data instanceof List<File>) {
				return new FileProcessor(output, FilterChainFactory.createFileFilterChain(paramsHolder), !paramsHolder.checkParamIsEmpty(Param.FILE_MERGING))
		}
		else if (data instanceof InputStream){
			return new InputStreamProcessor(output)
		}
		else {
			throw new IllegalArgumentException("Unsupported data type: " + data.getClass().getCanonicalName());
		}

	}
	
}
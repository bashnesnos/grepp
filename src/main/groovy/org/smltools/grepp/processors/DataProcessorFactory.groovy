package org.smltools.grepp.processors;

import org.smltools.grepp.config.ParamHolder
import org.smltools.grepp.config.Param
import org.smltools.grepp.output.OutputFactory
import org.smltools.grepp.output.GreppOutput
import org.smltools.grepp.processors.DataProcessor;
import org.smltools.grepp.filters.FilterChainFactory;


class DataProcessorFactory {


	/**
	 * Simple factory method, intializes appropriate processors judging by configInstance.       
	 *        
	 * @return new DataProcessor instance
	 */
    public static DataProcessor<?> getProcessorInstance(ParamHolder paramsHolder) 
    {
		def data = paramsHolder.getProcessingData()
		return getProcessorInstanceForData(paramsHolder, data)
    }
	
	public static void process(ParamHolder paramsHolder) {
		def data = paramsHolder.getProcessingData()
		getProcessorInstanceForData(paramsHolder, data).process(data)
	}
	
	public static <T> DataProcessor<T> getProcessorInstanceForData(ParamHolder paramsHolder, T data) {
		final GreppOutput<?,?> output = OutputFactory.getOutputInstance(paramsHolder)
		
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
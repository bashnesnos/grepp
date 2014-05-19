package org.smltools.grepp.processors

import java.io.File;
import java.io.InputStream
import java.util.List;
import org.smltools.grepp.filters.enums.Event
import org.smltools.grepp.filters.FilterBase;
import org.smltools.grepp.output.GreppOutput;
import org.smltools.grepp.processors.DataProcessor;

import groovy.util.logging.Slf4j;
import org.smltools.grepp.exceptions.*

/**
 * Simple processor to process a given InputStream
 * 
 * @author Alexander Semelit
 *
 */

@Slf4j
class InputStreamProcessor implements DataProcessor<InputStream> {

	GreppOutput output
	
	InputStreamProcessor(GreppOutput output_)
	{
		output = output_
	}
	
	/**
	 * Method which does processing of one portion of data, which contains lines. <br>
	 * It is considered to be an InputSream.
	 * 
	 * @param data InputStream containing the data to process
	 */
    void processData(InputStream data)
    {
        if (data == null || data.available() == 0) return
        def curLine = 0
        GreppOutput output = output //shadowing to get rid of GetEffectivePogo in the loop
		
        try {
            data.eachLine { String line ->
                log.trace("curLine: {}", curLine)
                curLine += 1
                output.printToOutput(line)
            }
        }
        catch(FilteringIsInterruptedException e) {
            log.trace("No point to read file further as identified by filter chain")
        }

        output.processEvent(Event.CHUNK_ENDED)
        log.info("Stream ended. Lines processed: {}", curLine)
    }

	@Override
	public void process(InputStream data) {
			processData(data)
			output.processEvent(Event.ALL_CHUNKS_PROCESSED)
			output.closeOutput()
	}
}

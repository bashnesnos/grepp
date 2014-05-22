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

@Slf4j("LOGGER")
public class InputStreamProcessor implements DataProcessor<InputStream> {

	GreppOutput<String> output
	
	public InputStreamProcessor(GreppOutput<String> output) {
		this.output = output
	}
	
	/**
	 * Method which does processing of one portion of data, which contains lines. <br>
	 * It is considered to be an InputSream.
	 * 
	 * @param data InputStream containing the data to process
	 */
    protected void processStream(InputStream data) {
        def curLine = 0
        GreppOutput output = output //shadowing to get rid of GetEffectivePogo in the loop
		
        try {
            data.eachLine { String line ->
                LOGGER.trace("curLine: {}", curLine)
                curLine += 1
                output.print(line)
            }
        }
        catch(FilteringIsInterruptedException e) {
            LOGGER.trace("No point to read strean further as identified by filter chain")
        }

        output.processEvent(Event.CHUNK_ENDED)
        LOGGER.info("Stream ended. Lines processed: {}", curLine)
    }

	@Override
	public void process(InputStream data) {
		if (data == null || data.available() == 0) {
			LOGGER.trace("No stream was given ot it is empty")
			return
		}

		processStream(data)
		output.processEvent(Event.ALL_CHUNKS_PROCESSED)
		output.close()
	}
}

package org.smlt.tools.wgrep.processors

import java.io.File;
import java.io.InputStream
import java.util.List;
import org.smlt.tools.wgrep.filters.enums.Event
import org.smlt.tools.wgrep.filters.FilterBase;
import org.smlt.tools.wgrep.output.WgrepOutput;
import groovy.util.logging.Slf4j;
import org.smlt.tools.wgrep.exceptions.*

@Slf4j
class InputStreamProcessor implements DataProcessor<InputStream> {

	WgrepOutput output
	
	InputStreamProcessor(WgrepOutput output_)
	{
		output = output_
	}
	
	/**
	 * Method which does processing of one portion of data, which contains lines. <br>
	 * It is considered to be a File, but could be anything which supports eachLine returning a String.
	 * 
	 * @param data Supposed to be a File, or anything that supports eachLine method which returns String
	 */
    void processData(InputStream data)
    {
        if (data == null) return
        def curLine = 0
        WgrepOutput output = output //shadowing to get rid of GetEffectivePogo in the loop
		
        try {
            data.eachLine { String line ->
                log.trace("curLine: {}", curLine)
                curLine += 1
                output.printToOutput(line)
            }
        }
        catch(FilteringIsInterruptedException e) {
            log.trace("No point to read file further as identified by filter chain")
            output.processEvent(Event.FLUSH)
        }
		
		output.processEvent(Event.FILE_ENDED)
        log.info("Stream ended. Lines processed: {}", curLine)
    }

	@Override
	public void process(InputStream data) {
			processData(data)
			output.processEvent(Event.ALL_FILES_PROCESSED)
			output.closeOutput()
	}
}

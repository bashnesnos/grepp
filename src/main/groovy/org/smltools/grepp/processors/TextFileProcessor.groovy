package org.smltools.grepp.processors

import groovy.util.logging.Slf4j;

import java.util.regex.Matcher
import java.lang.StringBuilder
import org.smltools.grepp.filters.enums.Event
import org.smltools.grepp.output.GreppOutput
import org.smltools.grepp.output.RefreshableOutput
import org.smltools.grepp.processors.DataProcessor;
import org.smltools.grepp.exceptions.*

/**
 * Class which triggers and controls text file processing.
 * 
 * @author Alexander Semelit
 *
 */
@Slf4j("LOGGER")
public class TextFileProcessor implements DataProcessor<List<File>> {
   
    private boolean isMerging;
	private GreppOutput<String> output;
 
	/**
	 * Create new instance with supplied filter chains and {@link WgrepConfig} instance.
	 * 
	 * @param config WgrepConfig instance which will be used as param source
	 * @param filterChain_ FilterBase chain which will be used to filter each file line
	 * @param filesFilterChain_ FilterBase chain which will be used to filter filename List
	 */
    public TextFileProcessor(GreppOutput<String> output, boolean isMerging) {
		this.output = output
        this.isMerging = isMerging
        LOGGER.trace("Is merging? {}", isMerging)
    }


	/**
	 * Hook method which is called prior to file processing. Needed for check and configInstance refreshing if it is on. 
	 * 
	 * @param file_ a File instance which is needed to be initialized.
	 * @return File instance if it was successfully initialized. null otherwise
	 */
    private File initFile(File file) {
        LOGGER.info("Initializating {}", file.name)
        if (output instanceof RefreshableOutput) {
			output.refreshFilters(file.name)
		}
        return file
    }

	/**
	 * Method which does processing of one File, which contains lines. <br>
	 * 
	 * @param data a File which needs to be processed
	 */
    protected void processSingleFile(File data) {
        if (data == null) return
        LOGGER.info("File {} started", data.name)
        def curLine = 1
        GreppOutput output = output //shadowing to get rid of GetEffectivePogo in the loop
        try {
            data.eachLine { String line ->
                LOGGER.trace("curLine: {}", curLine)
                curLine += 1
                output.print(line)
            }
        }
        catch(FilteringIsInterruptedException e) {
            LOGGER.trace("No point to read file further as identified by filter chain")
        }

        if (!isMerging) { 
        	output.processEvent(Event.CHUNK_ENDED)
        }
        
        LOGGER.info("File {} ended. Lines processed: {}", data.name, curLine)
    }

	@Override
	public void process(List<File> data) {
		if (data != null) {
			data.each {
				processSingleFile(initFile(it))
			}
			output.processEvent(Event.ALL_CHUNKS_PROCESSED)
			output.close()
		}
		else {
			LOGGER.trace("No files given; nothing to process")
		}
	}
}
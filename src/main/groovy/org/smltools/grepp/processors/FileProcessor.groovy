package org.smltools.grepp.processors

import groovy.util.logging.Slf4j;

import java.util.regex.Matcher
import java.lang.StringBuilder
import org.smltools.grepp.filters.FilterBase
import org.smltools.grepp.filters.enums.Event
import org.smltools.grepp.output.WgrepOutput
import org.smltools.grepp.processors.DataProcessor;
import org.smltools.grepp.exceptions.*

/**
 * Class which triggers and controls file processing.
 * 
 * @author Alexander Semelit
 *
 */
@Slf4j
class FileProcessor implements DataProcessor<List<File>>
{
   
    private boolean isMerging;
	private WgrepOutput output;
	private FilterBase<List<File>> fileFilter;
 
	/**
	 * Create new instance with supplied filter chains and {@link WgrepConfig} instance.
	 * 
	 * @param config WgrepConfig instance which will be used as param source
	 * @param filterChain_ FilterBase chain which will be used to filter each file line
	 * @param filesFilterChain_ FilterBase chain which will be used to filter filename List
	 */
    FileProcessor(WgrepOutput output_, FilterBase<List<File>> fileFilter_, boolean isMerging_) 
    {
		output = output_
        isMerging = isMerging_
		fileFilter = fileFilter_
    }


	/**
	 * Hook method which is called prior to file processing. Needed for check and configInstance refreshing if it is on. 
	 * 
	 * @param file_ a File instance which is needed to be initialized.
	 * @return File instance if it was successfully initialized. null otherwise
	 */
    private File initFile(File file_)
    {
        log.info("Initializating {}", file_.name)
		output.refreshFilters(file_.name)
        return file_
    }

	/**
	 * Method which does processing of one File, which contains lines. <br>
	 * 
	 * @param data a File which needs to be processed
	 */
    void processData(File data)
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
		
		if (!isMerging) output.processEvent(Event.FILE_ENDED)
        log.info("File ended. Lines processed: {}", curLine)
    }

	@Override
	public void process(List<File> data) {
        List<File> filteredData = fileFilter.filter(data)
		if (filteredData != null) {
			filteredData.each {
				processData(initFile(it))
			}
			output.processEvent(Event.ALL_FILES_PROCESSED)
			output.closeOutput()
		}
	}
}
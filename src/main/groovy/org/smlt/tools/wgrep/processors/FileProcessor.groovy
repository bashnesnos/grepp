package org.smlt.tools.wgrep.processors

import groovy.util.logging.Slf4j;

import java.util.regex.Matcher
import java.lang.StringBuilder
import org.smlt.tools.wgrep.filters.*
import org.smlt.tools.wgrep.filters.enums.Event
import org.smlt.tools.wgrep.output.WgrepOutput
import org.smlt.tools.wgrep.exceptions.*
import org.smlt.tools.wgrep.config.ModuleBase;
import org.smlt.tools.wgrep.config.WgrepConfig

/**
 * Class which triggers and controls file processing.
 * 
 * @author Alexander Semelit
 *
 */
@Slf4j
class FileProcessor extends ModuleBase implements DataProcessor
{
    private ArrayList<File> fileList = []
    
    private boolean isMerging = null
    private FilterBase filterChain = null
    private FilterBase filesFilterChain = null
	private WgrepOutput output = null
 
	/**
	 * Simple factory method, which takes WgrepConfig instance and initializes appropriate filter chains.       
	 *        
	 * @param config WgrepConfig instance
	 * @return new FileProcessor instance
	 */
    static FileProcessor getInstance(WgrepConfig config, WgrepOutput output) 
    {
        return new FileProcessor(config, output, FilterChainFactory.createFilterChainByConfig(config), FilterChainFactory.createFileFilterChainByConfig(config))
    }

	/**
	 * Create new instance with supplied filter chains and {@link WgrepConfig} instance.
	 * 
	 * @param config WgrepConfig instance which will be used as param source
	 * @param filterChain_ FilterBase chain which will be used to filter each file line
	 * @param filesFilterChain_ FilterBase chain which will be used to filter filename List
	 */
    FileProcessor(WgrepConfig config, WgrepOutput output_, FilterBase filterChain_, FilterBase filesFilterChain_) 
    {
        super(config)
		output = output_
		filterChain = filterChain_
        filesFilterChain = filesFilterChain_
		List<String> files_ = getParam('FILES')
        log.trace("Total files to analyze: " + files_.size())
        fileList = filesFilterChain.filter(files_)
        isMerging = getParam('FILE_MERGING') != null

    }

	/**
	 * Overriden method which check specific to FileProcessot params
	 */
	@Override
    boolean isConfigValid() {
        boolean checkResult = super.isConfigValid()
        if (getParam('FILES') == null)
        {
            log.warn('FILES are not specified')
            checkResult = false
        }
        return checkResult
    }

	/**
	 * Hook method which is called prior to file processing. Needed for check and configInstance refreshing if it is on. 
	 * 
	 * @param file_ a File instance which is needed to be initialized.
	 * @return File instance if it was successfully initialized. null otherwise
	 */
    private File initFile(File file_)
    {
        log.info("Initializating " + file_.name)
        try {
            if (refreshConfigByFileName(file_.name))
            {            
                filterChain = FilterChainFactory.createFilterChainByConfig(configInstance)
            }                
        }
        catch(IllegalArgumentException e) {
            e.printStackTrace()
            return null
        }   
        return file_
    }

	/**
	 * Method which does processing of one portion of data, which contains lines. <br>
	 * It is considered to be a File, but could be anything which supports eachLine returning a String.
	 * 
	 * @param data Supposed to be a File, or anything that supports eachLine method which returns String
	 */
    void processData(def data)
    {
        if (data == null) return
        def curLine = 0
        FilterBase chain = filterChain //reassignig to get rid of GetEffectivePogo in the loop
        try {
            data.eachLine { String line ->
                if (log.isTraceEnabled())
                {
                    log.trace("curLine: $curLine")
                }
                curLine += 1
                output.printToOutput(chain.filter(line))
            }
        }
        catch(TimeToIsOverduedException e) {
            log.trace("No point to read file further since supplied date TO is overdued")
            chain.processEvent(Event.FLUSH)
        }
		
		if (!isMerging) chain.processEvent(Event.FILE_ENDED)
        log.info("File ended. Lines processed: " + curLine)
    }

	/**
	 * Getter for files list to process.
	 * 
	 * @return current instance files list
	 */
    List<File> getFiles()
    {
        return fileList
    }

	@Override
	public void process() {
        fileList.each {
            processData(initFile(it))
        }
        output.printToOutput(filterChain.processEvent(Event.ALL_FILES_PROCESSED))
		output.closeOutput()
	}
}
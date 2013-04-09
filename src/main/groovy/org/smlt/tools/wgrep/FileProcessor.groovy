package org.smlt.tools.wgrep

import groovy.util.logging.Slf4j;

import java.util.regex.Matcher
import java.lang.StringBuilder
import org.smlt.tools.wgrep.filters.*
import org.smlt.tools.wgrep.filters.enums.Event
import org.smlt.tools.wgrep.exceptions.*

@Slf4j
class FileProcessor extends ModuleBase
{
    //Reads logs files
    private ArrayList<File> fileList = []
    
    private boolean isMerging = null
    private int curLine = 0
    private FilterBase filterChain = null
    private FilterBase filesFilterChain = null
        
    static FileProcessor getInstance(WgrepConfig config) 
    {
        return new FileProcessor(config, FilterChainFactory.createFilterChainByConfig(config), FilterChainFactory.createFileFilterChainByConfig(config))
    }

    FileProcessor(WgrepConfig config, FilterBase filterChain_, FilterBase filesFilterChain_) 
    {
        super(config)
		filterChain = filterChain_
        filesFilterChain = filesFilterChain_
		List<String> files_ = getParam('FILES')
        log.trace("Total files to analyze: " + files_.size())
        fileList = filesFilterChain.filter(files_)
        isMerging = getParam('FILE_MERGING') != null

    }

    boolean isConfigValid() {
        boolean checkResult = super.isConfigValid()
        if (getParam('FILES') == null)
        {
            log.warn('FILES are not specified')
            checkResult = false
        }
        return checkResult
    }

    void processAll()
    {
        fileList.each {
            process(openFile(it))
        }
        filterChain.processEvent(Event.ALL_FILES_PROCESSED)
    }

    private File openFile(File file_)
    {
        log.info("Opening " + file_.name)
        try {
            if (refreshConfigByFileName(file_.name))
            {            
                filterChain = FilterChainFactory.createFilterChainByConfig(configInstance)
                filesFilterChain.processEvent(Event.CONFIG_REFRESHED)
            }                
        }
        catch(IllegalArgumentException e) {
            e.printStackTrace()
            return null
        }   
        curLine = 0
        return file_
    }

    void process(def data)
    {
        if (data == null) return
        def endLine = null
        
        try {
            data.eachLine { String line ->
                log.trace("curLine: " + curLine)
                curLine += 1
                filterChain.filter(line)
            }
        }
        catch(TimeToIsOverduedException e) {
            log.trace("No point to read file further since supplied date TO is overdued")
        }
		
		if (!isMerging) filterChain.processEvent(Event.FILE_ENDED)
        log.info("File ended. Lines processed: " + curLine)
    }

    List<File> getFiles()
    {
        return fileList
    }
}
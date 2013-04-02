package org.smlt.tools.wgrep

import java.util.regex.Matcher
import java.lang.StringBuilder
import org.smlt.tools.wgrep.filters.*
import org.smlt.tools.wgrep.exceptions.*

class FileProcessor extends ModuleBase
{
    //Reads logs files
    private ArrayList<File> fileList = []
    
    private boolean isMerging = null
    private int curLine = 0
    private FilterBase filterChain = null
    private FilterBase filesFilterChain = null
        
    static FileProcessor getInstance() 
    {
        return new FileProcessor(FilterChainFactory.createFilterChainByFacade(), FilterChainFactory.createFileFilterChainByFacade(), getFacade().getParam('FILES'), getFacade().getParam('FILE_MERGING'))
    }

    FileProcessor(def filterChain_, def filesFilterChain_, def files_, def merging_) 
    {
        filterChain = filterChain_
        filesFilterChain = filesFilterChain_
        if (isVerboseEnabled()) verbose("Total files to analyze: " + files_.size())
        fileList = filesFilterChain.filter(files_)
        isMerging = merging_ != null

    }

    void processAll()
    {
        fileList.each {
            process(openFile(it))
        }
    }

    private def openFile(File file_)
    {
        if (isVerboseEnabled()) trace("Opening " + file_.name)
        try {
            if (getFacade().refreshConfigByFileName(file_.name))
            {            
                filterChain = FilterChainFactory.createFilterChainByFacade()
                filesFilterChain.refresh()
            }                
        }
        catch(IllegalArgumentException e) {
            e.printStackTrace()
            return null
        }   
        curLine = 0
        return file_
    }

    def process(def data)
    {
        if (data == null) return
        def endLine = null
        
        try {
            data.eachLine { String line ->
                if (isTraceEnabled()) trace("curLine: " + curLine)
                curLine += 1
                filterChain.filter(line)
            }
        }
        catch(TimeToIsOverduedException e) {
            if (isTraceEnabled()) trace("No point to read file further since supplied date TO is overdued")
        }

        filterChain.finalize()
        if (isVerboseEnabled()) verbose("File ended. Lines processed: " + curLine)
    }

    def getFiles()
    {
        return fileList
    }
}
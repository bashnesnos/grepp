package org.smlt.tools.wgrep

import java.util.regex.Matcher
import java.lang.StringBuilder
import org.smlt.tools.wgrep.filters.*
import org.smlt.tools.wgrep.exceptions.*

class FileProcessor extends ModuleBase
{
    //Reads logs files
    private def fileList = []
    private def curDir = null
    private boolean isMerging = null
    private int curLine = 0
    private FilterBase filterChain = null
    private def fSeparator = null
    

    static FileProcessor getInstance() 
    {
        return new FileProcessor(FilterChainFactory.createFilterChainByFacade(), getFacade().getParam('FILES'), getFacade().getParam('FOLDER_SEPARATOR'), getFacade().getParam('CWD'), getFacade().getParam('FILE_MERGING'))
    }

    FileProcessor(def filterChain_, def files_, def separator_, def curDir_, def merging_) 
    {
        filterChain = filterChain_
        fileList = files_
        fSeparator = separator_
        curDir = curDir_
        isMerging = merging_ != null
        if (isVerboseEnabled()) verbose("Total files to analyze: " + this.fileList.size())
        this.analyzeList()
    }


    private def analyzeList()
    {
        def newFileList = []
        def removeFiles = []
        fileList.each
        { fil ->
            if (isTraceEnabled()) trace("analyzing supplied file: " + fil);
            if (fil =~ /\*/)
            {
                removeFiles.add(fil);
                def flname = fil;
                if (fil =~ fSeparator)
                {
                    curDir = (fil =~/.*(?=$fSeparator)/)[0]
                    flname = (fil =~ /.*$fSeparator(.*)/)[0][1]
                }
                def files = new File(curDir).listFiles();
                if (isTraceEnabled()) trace("files found " + files)
                def ptrn = flname.replaceAll(/\*/) {it - '*' + '.*'};
                files.each
                {
                    if (it.name ==~ /$ptrn/)
                    {
                        if (isTraceEnabled()) trace("adding file " + it)
                        newFileList.add(it)
                    }
                }
            }
        }
        
        removeFiles.each { rmFil -> fileList.remove(rmFil)  }
        if (newFileList.size() > 0) newFileList.sort() { it.lastModified() }.each { fileList.add(it.absolutePath) }
        if (isTraceEnabled()) trace("Total files for wgrep: " + fileList)
    }

    void processAll()
    {
        fileList.each {
            process(openFile(it))
        }
    }

    private def openFile(def fName)
    {
        if (isVerboseEnabled()) trace("Opening " + fName)
        def fileObj = new File(fName)
        if (getFacade().checkFileTime(fileObj))
        {
            if (isTraceEnabled()) trace("Done.")
            try {
                if (getFacade().refreshConfigByFileName(fName))
                {            
                    filterChain = FilterChainFactory.createFilterChainByFacade()
                }                
            }
            catch(IllegalArgumentException e) {
                e.printStackTrace()
                return null
            }
            curLine = 0
            return fileObj
        }
        else
            return null
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
}
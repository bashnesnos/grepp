package org.smlt.tools.wgrep

import java.util.regex.Matcher
import java.lang.StringBuilder

class FileProcessor extends ModuleBase
{
    //Reads logs files
    private def fileList = []
    private def curDir = null
    private boolean isMerging = null
    private int curLine = 0
    private def filterChain = null
    private def fSeparator = null

    static FileProcessor getInstance() 
    {
        def filterChain_ = new PrintFilter()
        def dateTimeChecker_ = null
        if (getFacade().getParam('POST_PROCESSING') != null)
        {
            filterChain_ = new PostFilter(filterChain_)
        } 

        if (getFacade().getParam('EXTNDD_PATTERN') != null || getFacade().getParam('PRESERVE_THREAD') != null)
        {
            filterChain_ = new ComplexFilter(filterChain_)
        } 
        else
        {
            filterChain_ = new BasicFilter(filterChain_)
        }

        if (getFacade().getParam('DATE_TIME_FILTER') != null)
        {
            dateTimeChecker_ = new DateTimeChecker()
        } 
        return new FileProcessor(filterChain_, getFacade().getParam('FILES'), getFacade().getParam('FOLDER_SEPARATOR'), getFacade().getParam('CWD'), getFacade.getParam('FILE_MERGING'))
    }

    private FileProcessor(def filterChain_, def files_, def separator_, def curDir_, def merging_) 
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
        if (dateTimeChecker == null || dateTimeChecker.check(fileObj))
        {
            if (isTraceEnabled()) trace("Done.")
            getFacade().checkEntryPattern(fName)
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

        if (isVerboseEnabled()) verbose("File ended. Lines processed: " + curLine)
    }
}
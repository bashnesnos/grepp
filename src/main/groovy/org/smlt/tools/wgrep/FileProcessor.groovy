package org.smlt.tools.wgrep

class FileProcessor extends ModuleBase
{
    //Reads logs files
    protected def fileList = []
    protected int curLine = 0
    protected boolean isBlockMatched = false;
    protected StringBuffer curBlock = null;
    protected def filterChain = null;
    protected def dateTimeChecker = null;
    protected def fSeparator = null;
    protected def logEntryPattern = null;

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
        return new FileProcessor(filterChain_, dateTimeChecker_)
    }

    private FileProcessor(def filterChain_, def dateTimeChecker_) 
    {
        filterChain = filterChain_
        dateTimeChecker = dateTimeChecker_
        fileList = getFacade().getParam('FILES')
        fSeparator = getFacade().getParam('FOLDER_SEPARATOR')
        logEntryPattern = getFacade().getParam('LOG_ENTRY_PATTERN')
        if (isVerboseEnabled()) verbose("Total files to analyze: " + this.fileList.size())
        this.analyzeList()
    }


    protected def analyzeList()
    {
        def dir = getFacade().CWD
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
                    dir = (fil =~/.*(?=$fSeparator)/)[0]
                    flname = (fil =~ /.*$fSeparator(.*)/)[0][1]
                }
                def files = new File(dir).listFiles();
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

    protected def openFile(def fName)
    {
        if (isTraceEnabled()) trace("Opening " + fName)
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
        data.eachLine { line ->
            if (isTraceEnabled()) trace("curLine: " + curLine)
            curLine += 1
            if (endLine != null) 
            {
                processLine(endLine, logEntryPattern)
            }
            endLine = processLine(line, logEntryPattern)
        }

        if (endLine != null) //if file ended with matching line
        {
            processLine(endLine, logEntryPattern)
        }

        if (isVerboseEnabled()) verbose("File ended. Lines processed: " + curLine)
        if (getFacade().getParam('FILE_MERGING') == null && isBlockMatched) returnBlock(curBlock.toString())
        else (isVerboseEnabled() && !isBlockMatched) ? verbose("Block continues"): verbose("Matched block continues")
    }

    def processLine(def line, def pattern)
    {
        def entry = (line =~ pattern);
        if (entry)
        {
            if (!isBlockMatched && (dateTimeChecker == null || dateTimeChecker.check(entry[0])))
            {
                isBlockMatched = true
                if (isTraceEnabled()) trace("appending")
                appendCurBlock(line)
            }
            else if (isBlockMatched)
            {
                isBlockMatched = false
                if (isTraceEnabled()) trace("returning block")
                returnBlock(curBlock.toString())
                return line
            }
            else
            {
                if (isTraceEnabled()) trace("skipping")
                return line
            }
        }
        else if (isBlockMatched)
        {
            if (isTraceEnabled()) trace("appending")
            appendCurBlock(line)
        }
        return
    }

    def processAll()
    {
        fileList.each {
            process(openFile(it))
        }
    }

    protected def appendCurBlock(def line)
    {
        if (curBlock != null)
        {
            curBlock = curBlock.append('\n').append(line)
        }
        else 
        {
            curBlock = new StringBuffer(line)
        }
    }
    
    protected def returnBlock(def block)
    {
        filterChain.filter(block)
        curBlock = null
    }

}
package org.smlt.tools.wgrep

class FileProcessor extends ModuleBase
{
    //Reads logs files
    def fileList = []
    int curLine = 0
    boolean isBlockMatched = false;
    StringBuffer curBlock = null;
    def filterChain = null;
    def dateTimeChecker = null;
    def fSeparator = null;

    static FileProcessor getInstance() 
    {
        def filterChain_ = new PrintFilter()
        def dateTimeChecker_ = null
        if (getFacade().getParam('POST_PROCESSING'))
        {
            filterChain_ = new PostFilter(filterChain_)
        } 

        if (getFacade().getParam('EXTNDD_PATTERN') || getFacade().getParam('PRESERVE_THREAD'))
        {
            filterChain_ = new ComplexFilter(filterChain_)
        } 
        else
        {
            filterChain_ = new BasicFilter(filterChain_)
        }

        if (getFacade().getParam('DATE_TIME_FILTER'))
        {
            dateTimeChecker_ = new DateTimeChecker()
        } 
        return new FileProcessor(filterChain_, dateTimeChecker_)
    }

    private FileProcessor(def filterChain_, def dateTimeChecker_) 
    {
        filterChain = filterChain_
        dateTimeChecker = dateTimeChecker_
        fileList = getFacade().getParam("FILES")
        fSeparator = getFacade().getParam('FOLDER_SEPARATOR')
        if (isVerboseEnabled()) verbose("Total files to analyze: " + this.fileList.size())
        this.analyzeList()
    }


    def analyzeList()
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

    def openFile(def fName)
    {
        if (isTraceEnabled()) trace("Opening " + fName)
        def fileObj = new File(fName)
        if (!dateTimeChecker || dateTimeChecker.check(fileObj))
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
        if (!data) return
        def endLine = null
        data.eachLine { line ->
            if (isTraceEnabled()) trace("curLine: " + curLine)
            curLine += 1
            if (endLine) 
            {
                processLine(endLine)
            }
            endLine = processLine(line)
        }

        if (endLine) //if file ended with matching line
        {
            processLine(endLine)
        }

        if (isVerboseEnabled()) verbose("File ended. Lines processed: " + curLine)
        if (!getFacade().getParam('FILE_MERGING') && isBlockMatched) returnBlock(curBlock.toString())
        else (isVerboseEnabled() && !isBlockMatched) ? verbose("Block continues"): verbose("Matched block continues")
    }

    def processLine(def line)
    {
        def entry = (line =~ getFacade().getParam('LOG_ENTRY_PATTERN'));
        if (entry)
        {
            if (!isBlockMatched && (!dateTimeChecker || dateTimeChecker.check(entry[0])))
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

    def appendCurBlock(def line)
    {
        if (curBlock)
            curBlock = curBlock.append('\n').append(line)
        else 
            curBlock = new StringBuffer(line)
    }
    
    def returnBlock(def block)
    {
        filterChain.filter(block)
        curBlock = null
    }

    def addFile(def filename)
    {
        fileList.add(filename)
    }
}
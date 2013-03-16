package org.smlt.tools.wgrep

class FileProcessor extends FacadeBase
{
    //Reads logs files
    def fileList = []
    int curLine = 0
    boolean isBlockMatched = false;
    StringBuffer curBlock = null;

    FileProcessor(def files) {
        setCallingClass(this.getClass())
        if (!files)
        {
            files = getFacade().getParam("FILES")
        }
        fileList = files
        verbose("Total files to analyze: " + this.fileList.size())
        this.analyzeList()
    }


    def analyzeList()
    {
        def dir = getFacade().CWD
        def newFileList = []
        def removeFiles = []
        fileList.each
        { fil ->
            trace("analyzing supplied file: " + fil);
            if (fil =~ /\*/)
            {
                removeFiles.add(fil);
                def flname = fil;
                if (fil =~ getFacade().FOLDER_SEPARATOR)
                {
                    dir = (fil =~/.*(?=${getFacade().FOLDER_SEPARATOR})/)[0]
                    flname = (fil =~ /.*${getFacade().FOLDER_SEPARATOR}(.*)/)[0][1]
                }
                def files = new File(dir).listFiles();
                trace("files found " + files)
                def ptrn = flname.replaceAll(/\*/) {it - '*' + '.*'};
                files.each
                {
                    if (it.name ==~ /$ptrn/)
                    {
                        trace("adding file " + it)
                        newFileList.add(it)
                    }
                }
            }
        }
        
        removeFiles.each { rmFil -> fileList.remove(rmFil)  }
        if (newFileList.size() > 0) newFileList.sort() { it.lastModified() }.each { fileList.add(it.absolutePath) }
        trace("Total files for wgrep: " + fileList)
    }

    def openFile(def fName)
    {
        trace("Opening " + fName)
        def fileObj = getFacade().checkFileTime(fName)
        if (fileObj)
        {
            trace("Done.")
            getFacade().checkEntryPattern(fName)
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
            trace("curLine: " + curLine)
            curLine += 1
            if (endLine) 
            {
                processLine(endLine)
            }
            endLine = processLine(line)
        }

        verbose("File ended. Lines processed: " + curLine)
        if (!getFacade().getParam('FILE_MERGING') && isBlockMatched) printBlock(curBlock.toString())
        else !isBlockMatched?verbose("Block continues"):verbose("Matched block continues")
    }

    def processLine(def line)
    {
        def entry = (line =~ getFacade().LOG_ENTRY_PATTERN);
        if (entry)
        {
            if (!isBlockMatched && getFacade().checkEntryTime(entry[0]))
            {
                isBlockMatched = true
                trace("appending")
                appendCurBlock(line)
            }
            else if (isBlockMatched)
            {
                isBlockMatched = false
                trace("returning block")
                printBlock(curBlock.toString())
                return line
            }
            else
            {
                trace("skipping")
                return line
            }
        }
        else if (isBlockMatched)
        {
            trace("appending")
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
    
    def printBlock(def block)
    {
        getFacade().filter(block)
        curBlock = null
    }

    def addFile(def filename)
    {
        fileList.add(filename)
    }
}
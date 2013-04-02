package org.smlt.tools.wgrep.filters

import java.util.regex.Matcher

class FileNameFilter extends FilterBase
{    
    private def fSeparator = null
    private def curDir = null

    FileNameFilter(FilterBase nextFilter_) 
    {
        super(nextFilter_, null)
        fSeparator = getFacade().getParam('FOLDER_SEPARATOR')
        curDir = getFacade().getParam('CWD')
    }


    def filter(def fileNames) {
        if (! fileNames instanceof ArrayList<String> ) throw new IllegalArgumentException("FileNameFilter accepts string list only")

        def files = analyzeFileNames(fileNames)
        if (files != null)
        {
            if (nextFilter != null) 
            {
                if (isTraceEnabled()) trace("Passing to next filter")
                nextFilter.filter(files)    
            }
            else 
            {
                if (isTraceEnabled()) trace("passed")
                return files
            }
        }
        else
        {
            if (isTraceEnabled()) trace("not passed")
            return Collections.emptyList()
        }  
    }

    def analyzeFileNames(ArrayList<String> fileNames)
    {
        if (fileNames == null) return fileNames
        
        def newFileList = []
        def removeFiles = []
        def fileList = fileNames.clone()
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
        fileList.each { newFileList.add(new File(it)) }
        if (isTraceEnabled()) trace("Total files for wgrep: " + newFileList)
        return newFileList
    }
}
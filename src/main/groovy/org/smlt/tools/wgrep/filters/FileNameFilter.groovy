package org.smlt.tools.wgrep.filters

import groovy.util.logging.Slf4j;

import java.util.regex.Matcher

@Slf4j
class FileNameFilter extends FilterBase
{    
    private def fSeparator = null
    private def curDir = null

    FileNameFilter(FilterBase nextFilter_, def fSeparator_, def cwd_) 
    {
        super(nextFilter_, null)
        fSeparator = fSeparator_
        curDir = cwd_
    }

    def filter(def fileNames) {
        if (! fileNames instanceof ArrayList<String> ) throw new IllegalArgumentException("FileNameFilter accepts string list only")

        def files = analyzeFileNames(fileNames)
        if (files != null)
        {
            if (nextFilter != null) 
            {
                log.trace("Passing to next filter")
                nextFilter.filter(files)    
            }
            else 
            {
                log.trace("passed")
                return files
            }
        }
        else
        {
            log.trace("not passed")
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
            log.trace("analyzing supplied file: " + fil);
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
                log.trace("files found " + files)
                def ptrn = flname.replaceAll(/\*/) {it - '*' + '.*'};
                files.each
                {
                    if (it.name ==~ /$ptrn/)
                    {
                        log.trace("adding file " + it)
                        newFileList.add(it)
                    }
                }
            }
        }
        
        removeFiles.each { rmFil -> fileList.remove(rmFil)  }
        fileList.each { newFileList.add(new File(it)) }
        log.trace("Total files for wgrep: " + newFileList)
        return newFileList
    }
}
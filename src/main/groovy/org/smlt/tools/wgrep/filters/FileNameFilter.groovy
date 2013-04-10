package org.smlt.tools.wgrep.filters

import groovy.util.logging.Slf4j;

import java.util.regex.Matcher

/**
 * Provides file name filtering. Which means in this particular place replacing patterns by real files with absolute paths. <br>
 * Is a simple filter, i.e. does not require WgrepConfig to work.
 * 
 * @author Alexander Semelit 
 */

@Slf4j
class FileNameFilter extends FilterBase
{    
    private String fSeparator = null
    private String curDir = null

	/**
	 *  Accepts file separator, which needs to be a valid regex pattern. And current working dir absolte path
	 */
	
    FileNameFilter(FilterBase nextFilter_, String fSeparator_, String cwd_) 
    {
        super(nextFilter_)
        fSeparator = fSeparator_
        curDir = cwd_
    }
	/**
	 * Replaces supplied fileNames with List containing File instances.
	 * 
	 * @return list of File or empty list otherwise
	 * @throws IllegalArgumentException if supplied fileNames are not an instanceof List containing strings
	 */
	@Override
    def filter(def fileNames) {
        if (! fileNames instanceof List<String> ) throw new IllegalArgumentException("FileNameFilter accepts string list only")

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

	/**
	 * Replaces fileName with appropriate File instances
	 *
	 * 
	 * @param fileNames file names list for analysis 
	 * @return list of File representing supplied fileNames list
	 */
	
    List<File> analyzeFileNames(List<String> fileNames)
    {
        if (fileNames == null) return fileNames
        
        List<File> fileList = []
        List<String> removeFiles = []
        List<String> fileNameList = fileNames.clone()
        fileNameList.each
        { fil ->
            log.trace("analyzing supplied file: " + fil);
            if (fil =~ /\*/)
            {
                removeFiles.add(fil);
                String flname = fil;
                if (fil =~ fSeparator)
                {
                    curDir = (fil =~/.*(?=$fSeparator)/)[0]
                    flname = (fil =~ /.*$fSeparator(.*)/)[0][1]
                }
                List<File> files = new File(curDir).listFiles();
                log.trace("files found " + files)
                String ptrn = flname.replaceAll(/\*/) {it - '*' + '.*'};
                files.each
                { file ->
                    if (file.name ==~ /$ptrn/)
                    {
                        log.trace("adding file " + file)
                        fileList.add(file)
                    }
                }
            }
        }
        
        removeFiles.each { rmFil -> fileNameList.remove(rmFil)  }
        fileNameList.each { fileList.add(new File(it)) }
        log.trace("Total files for wgrep: " + fileList)
        return fileList
    }
}
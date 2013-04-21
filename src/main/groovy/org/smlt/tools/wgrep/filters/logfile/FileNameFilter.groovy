package org.smlt.tools.wgrep.filters.logfile

import org.smlt.tools.wgrep.filters.FilterBase
import java.util.regex.Matcher

/**
 * Provides file name filtering. Which means in this particular case replacing patterns by real files with absolute paths. <br>
 * Is a simple filter, i.e. does not require WgrepConfig to work.
 * 
 * @author Alexander Semelit 
 */

class FileNameFilter extends FilterBase
{    
    protected String fSeparator = null
    protected String curDir = null
    protected List fileList = []

	/**
	 *  Accepts file separator, which needs to be a valid regex pattern. And current working dir absolte path
	 */
	
    FileNameFilter(FilterBase nextFilter_, String fSeparator_, String cwd_) 
    {
        super(nextFilter_, FileNameFilter.class)
        fSeparator = fSeparator_
        curDir = cwd_
    }

    /**
     * Analyzes all fileNames and builds List<File> instance
     * 
     * @throws IllegalArgumentException if supplied fileNames are not an instanceof List containing strings
     * @return true if fileList is not empty
     */

    @Override
    boolean check(def fileNames) {
        if (! fileNames instanceof List<String> ) throw new IllegalArgumentException("FileNameFilter accepts string list only")
        fileList = [] //invelidating file list
        fileList = analyzeFileNames(fileNames)
        return fileList != null && fileList.size() > 0
    }

    /**
    * Passes analyzed List<File> instead of receieved fileNames in <code>this.filter</code>
    */
	@Override
    void beforePassing(def fileNames) {
        passingVal = fileList
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
            if (log.isTraceEnabled()) log.trace("analyzing supplied file: " + fil);
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
                if (log.isTraceEnabled()) log.trace("files found " + files)
                String ptrn = flname.replaceAll(/\*/) {it - '*' + '.*'};
                files.each
                { file ->
                    if (file.name ==~ /$ptrn/)
                    {
                        if (log.isTraceEnabled()) log.trace("adding file " + file)
                        fileList.add(file)
                    }
                }
            }
        }
        
        removeFiles.each { rmFil -> fileNameList.remove(rmFil)  }
        fileNameList.each { fileList.add(new File(it)) }
        if (log.isTraceEnabled()) log.trace("Total files for wgrep: " + fileList)
        return fileList
    }
}
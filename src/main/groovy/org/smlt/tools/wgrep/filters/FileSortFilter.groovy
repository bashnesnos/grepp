package org.smlt.tools.wgrep.filters

class FileSortFilter extends FilterBase
{    

    FileSortFilter(FilterBase nextFilter_) {
        super(nextFilter_, null)
    }

    def filter(def files) {
        if (! files instanceof ArrayList<File> ) throw new IllegalArgumentException("FileSortFilter accepts file list only")
        
        def files_ = sortFiles(files)

        if (files_ != null)
        {
            if (nextFilter != null) 
            {
                if (isTraceEnabled()) trace("Passing to next filter")
                nextFilter.filter(files_)    
            }
            else 
            {
                if (isTraceEnabled()) trace("passed")
                return files_
            }
        }
        else
        {
            if (isTraceEnabled()) trace("not passed")
            return Collections.emptyList()
        }  
    }

    def sortFiles(ArrayList<File> files)
    {
        if (files == null) return files
        if (files.size() < 2) return files
        def fileList = files.clone()
        fileList.sort { it.lastModified() }

        if (isTraceEnabled()) 
        { 
            trace("FileList has been sorted")
            fileList.each { println it.name}
        }

        return fileList
    }
}
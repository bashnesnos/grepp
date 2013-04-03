package org.smlt.tools.wgrep.filters

import java.text.SimpleDateFormat
import java.util.regex.Matcher
import groovy.xml.dom.DOMCategory

class FileDateFilter extends FilterBase
{
    //Checking dates everywhere

    SimpleDateFormat FILE_DATE_FORMAT = null
    def INPUT_DATE_PTTRNS = []
    Date FROM_DATE = null
    Date TO_DATE = null
    int LOG_FILE_THRESHOLD = 24
    int LOG_FILE_THRESHOLD_MLTPLR = 60*60*1000

    FileDateFilter(FilterBase nextFilter_, def logFileThreshold_, def fileDateFormat_, def from_, def to_)
    {
        super(nextFilter_, null)
        FROM_DATE = from_
        TO_DATE = to_
        FILE_DATE_FORMAT = new SimpleDateFormat(fileDateFormat_)
        if (trshld != null) LOG_FILE_THRESHOLD = Integer.valueOf(logFileThreshold_)
    }

    FileDateFilter(FilterBase nextFilter_, def fileDateFormat_, def from_, def to_)
    {
        super(nextFilter_, null)
        FROM_DATE = from_
        TO_DATE = to_
        FILE_DATE_FORMAT = new SimpleDateFormat(fileDateFormat_)
        fillRefreshableParams()
    }

    def fillRefreshableParams() {
        def trshld = getFacade().getParam('LOG_FILE_THRESHOLD') 
        if (trshld != null) LOG_FILE_THRESHOLD = Integer.valueOf(trshld)
    }

    def filter(def files) {
        if (! files instanceof ArrayList<File> ) throw new IllegalArgumentException("FileDateFilter accepts file list only")
        if (check(files))
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

    /**
    * Facade method to check if supplied filename, and corresponding {@link File} object suits desired date and time. 
    * Calls {@link dtChecker.check()} method if {@link DATE_TIME_FILTER} is not null.
    *
    * @param fName A String with filename
    */
    def check(ArrayList<File> files) {
        return files.findAll { file -> checkFileTime(file) }
    }

    def checkFileTime(def file)
    {
        if (file == null) return
        def fileTime = new Date(file.lastModified())
        if (isTraceEnabled()) trace("fileTime:" + FILE_DATE_FORMAT.format(fileTime))
        if (FROM_DATE == null || FROM_DATE.compareTo(fileTime) <= 0)
        {
            if (TO_DATE != null)
            {
                if (isTraceEnabled()) trace(" Checking if file suits TO " +  FILE_DATE_FORMAT.format(TO_DATE))
                if (TO_DATE.compareTo(fileTime) >= 0)
                {
                    return true
                }
                if (TO_DATE.compareTo(fileTime) < 0)
                {
                    if (isTraceEnabled()) trace("Passed TO_DATE")
                    if (fileTime.before(new Date(TO_DATE.getTime() + LOG_FILE_THRESHOLD*LOG_FILE_THRESHOLD_MLTPLR))) return file
                    else
                    {
                        if (isTraceEnabled()) trace("File is too far")
                        return false
                    }
                }
            }
            if (isTraceEnabled()) trace("Passed FROM_DATE only")
            return true
        }
        else
        {
            if (isTraceEnabled()) trace("Not passed")
            return false
        }
    }

    def refresh()
    {
        fillRefreshableParams()
        super.refresh()
    }

}
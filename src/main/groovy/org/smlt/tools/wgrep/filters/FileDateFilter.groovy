package org.smlt.tools.wgrep.filters

import java.text.SimpleDateFormat
import java.util.regex.Matcher

import groovy.util.logging.Slf4j;
import groovy.xml.dom.DOMCategory
import org.smlt.tools.wgrep.WgrepConfig
import org.smlt.tools.wgrep.filters.enums.Event

@Slf4j
class FileDateFilter extends FilterBase
{
    //Checking dates everywhere

    SimpleDateFormat FILE_DATE_FORMAT = null
    List INPUT_DATE_PTTRNS = []
    Date FROM_DATE = null
    Date TO_DATE = null
    int LOG_FILE_THRESHOLD = 24
    int LOG_FILE_THRESHOLD_MLTPLR = 60*60*1000

    FileDateFilter(FilterBase nextFilter_, WgrepConfig config)
    {
        super(nextFilter_, null, config)
        FROM_DATE = getParam('FROM_DATE')
        TO_DATE = getParam('TO_DATE')
        FILE_DATE_FORMAT = new SimpleDateFormat(getParam('FILE_DATE_FORMAT'))
		fillRefreshableParams()
    }

    void fillRefreshableParams() {
        def trshld = getParam('LOG_FILE_THRESHOLD') 
        if (trshld != null) LOG_FILE_THRESHOLD = Integer.valueOf(trshld)
    }

    def filter(def files) {
        if (! files instanceof List<File> ) throw new IllegalArgumentException("FileDateFilter accepts file list only")
        def newFiles = check(files)
        log.trace("total files after:" + newFiles.size())
        if (newFiles != null)
        {
            if (nextFilter != null) 
            {
                log.trace("Passing to next filter")
                nextFilter.filter(newFiles)    
            }
            else 
            {
                log.trace("passed")
                return newFiles
            }
        }
        else
        {
            log.trace("not passed")
            return Collections.emptyList()
        }  
    }

    /**
    * Facade method to check if supplied filename, and corresponding {@link File} object suits desired date and time. 
    * Calls {@link dtChecker.check()} method if {@link DATE_TIME_FILTER} is not null.
    *
    * @param fName A String with filename
    */
    List<File> check(List<File> files) {
        log.trace("total files:" + files.size())
        return files.findAll { file -> checkFileTime(file) }
    }

    boolean checkFileTime(def file)
    {
        if (file == null) return
        Date fileTime = new Date(file.lastModified())
        log.trace("fileTime:" + FILE_DATE_FORMAT.format(fileTime))
        if (FROM_DATE == null || FROM_DATE.compareTo(fileTime) <= 0)
        {
            if (TO_DATE != null)
            {
                log.trace(" Checking if file suits TO " +  FILE_DATE_FORMAT.format(TO_DATE))
                if (TO_DATE.compareTo(fileTime) >= 0)
                {
                    return true
                }
                if (TO_DATE.compareTo(fileTime) < 0)
                {
                    log.trace("Passed TO_DATE")
                    if (fileTime.before(new Date(TO_DATE.getTime() + LOG_FILE_THRESHOLD*LOG_FILE_THRESHOLD_MLTPLR))) return file
                    else
                    {
                        log.trace("File is too far")
                        return false
                    }
                }
            }
            log.trace("Passed FROM_DATE only")
            return true
        }
        else
        {
            log.trace("Not passed")
            return false
        }
    }

    def processEvent(def event) {
        switch (event)
        {
            case Event.CONFIG_REFRESHED:
                fillRefreshableParams()
                break
            default:
                break
        }
        super.processEvent(event)
    }

}
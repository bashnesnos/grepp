package org.smlt.tools.wgrep.filters

import groovy.util.logging.Slf4j;

import java.util.regex.Matcher
import java.text.SimpleDateFormat
import org.smlt.tools.wgrep.WgrepConfig
import org.smlt.tools.wgrep.exceptions.TimeToIsOverduedException
import org.smlt.tools.wgrep.filters.enums.Event

@Slf4j
class EntryDateFilter extends FilterBase{

	private SimpleDateFormat DATE_FORMAT
    private Date FROM_DATE
    private Date TO_DATE
    private boolean isDateFromPassed = false

    EntryDateFilter(FilterBase nextFilter_, WgrepConfig config) {
		super(nextFilter_, null, config)
		fillRefreshableParams()        
		FROM_DATE = configInstance.getParam('FROM_DATE')
        TO_DATE = configInstance.getParam('TO_DATE')
        log.trace("Added on top of " + nextFilter.getClass().getCanonicalName())
	}

    def fillRefreshableParams()
    {
        setPattern(getParam('LOG_DATE_PATTERN'))
        def logDateFormatPtrn = getParam('LOG_DATE_FORMAT')
        if (logDateFormatPtrn != null) DATE_FORMAT = new SimpleDateFormat(logDateFormatPtrn)
    }

    /**
    * Facade method to check if supplied entry suits desired date and time. 
    * Calls {@link dtChecker.check()} method if {@link DATE_TIME_FILTER} and <code>entry</code> are not null.
    *
    * @param entry A String to be checked
    */

    def filter(def data) {
        if (check(data))
        {
            if (nextFilter != null) 
            {
                log.trace("Passing to next filter")
                nextFilter.filter(data)    
            }
            else 
            {
                throw new RuntimeException("shouldn't be the last in chain")
            }
        }
        else
        {
            log.trace("not passed")
        }  
    }


    boolean check(String blockData)
    {
        if (blockData != null && filterPtrn != null && DATE_FORMAT != null)
        {
           
            Date entryDate = null

            if (!isDateFromPassed || TO_DATE != null)
            {
                String timeString = null
                if (blockData instanceof String) 
                {
                    log.trace("Checking log entry " + blockData + " for log date pattern |" + filterPtrn + "| and formatting to |" +  DATE_FORMAT.toPattern() + "|")
                    Matcher entryDateMatcher = (blockData =~ filterPtrn)
                    if (entryDateMatcher.find())
                    {
                        timeString = entryDateMatcher.group(1)
                    }
                    else
                    {
                        log.trace("No signs of time in here")                        
                        return false
                    }
                }
                else if (blockData instanceof Matcher)
                {
                    log.trace("Checking matcher " + blockData.group()  + " and formatting to |" +  DATE_FORMAT.toPattern() + "|")                    
                    timeString = blockData.group(1)
                }
                else
                {
                    throw new IllegalArgumentException("blockData should be either Matcher or String")
                }

                entryDate =  DATE_FORMAT.parse(timeString)
            }
            else
            {
                log.trace("Date check was skipped, dateFromPassed=" + isDateFromPassed + ", TO_DATE=" + TO_DATE)
                return isDateFromPassed
            }

            if (entryDate != null && (FROM_DATE == null || FROM_DATE.compareTo(entryDate) <= 0))
            {
                isDateFromPassed = true
                if (TO_DATE != null)
                {
                    if (TO_DATE.compareTo(entryDate) >= 0)
                    {
                        log.trace("Passed TO_DATE")
                        return true
                    }
                    else
                    {
                        log.trace("Not passed")
                        throw new TimeToIsOverduedException(DATE_FORMAT.format(TO_DATE))
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
        log.trace("Date check was totally skipped, filterPtrn=" + filterPtrn)
        return true
    }

    def processEvent(def event) {
        switch (event)
        {
            case Event.FILE_ENDED:
                isDateFromPassed = false
                break
            default:
                break
        }
        super.processEvent(event)
    }

}
package org.smlt.tools.wgrep.filters

import groovy.util.logging.Slf4j;

import java.util.regex.Matcher
import java.text.SimpleDateFormat
import org.smlt.tools.wgrep.WgrepConfig
import org.smlt.tools.wgrep.exceptions.TimeToIsOverduedException
import org.smlt.tools.wgrep.filters.enums.Event

/**
 * Class provides entry date filtering for supplied FROM and TO dates.
 * 
 * @author Alexander Semelit
 *
 */
@Slf4j
class EntryDateFilter extends FilterBase{

	private SimpleDateFormat DATE_FORMAT
    private Date FROM_DATE
    private Date TO_DATE
    private boolean isDateFromPassed = false

	/**
	 * Creates new EntryDateFilter on top of the supplied filter chain. <br>
	 * Fills appropriate params from supplied WgrepConfig instance
	 * 
	 * @param nextFilter_ FilterBase next filter
	 * @param config WgrepConfig instance
	 */
    EntryDateFilter(FilterBase nextFilter_, WgrepConfig config) {
		super(nextFilter_, null, config)
		fillRefreshableParams()        
		FROM_DATE = configInstance.getParam('FROM_DATE')
        TO_DATE = configInstance.getParam('TO_DATE')
        log.trace("Added on top of " + nextFilter.getClass().getCanonicalName())
	}

	@Override
    boolean isConfigValid() {
        boolean checkResult = super.isConfigValid()
        if (getParam('FROM_DATE') == null && getParam('TO_DATE')  == null)
        {
            log.warn('Both FROM_DATE and TO_DATE are not specified')
        }
        return checkResult
    }
	
	/**
	 * Fills params that can be refreshed from config
	 * 
	 */
    void fillRefreshableParams()
    {
        setPattern(getParam('LOG_DATE_PATTERN'))
        def logDateFormatPtrn = getParam('LOG_DATE_FORMAT')
        if (logDateFormatPtrn != null) DATE_FORMAT = new SimpleDateFormat(logDateFormatPtrn)
    }

    /**
    * Checks if supplied entry suits desired from and to date and time. 
    *
    * @param entry A String to be checked
    * @throws IllegalArgumentException if supplied blockData is not String or Matcher instance
    * @throws TimeToIsOverduedException if TO_DATE was passed 
    */

	@Override
    boolean check(def blockData)
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

	/**
	 * Listens for FILE_ENDED event. Cleans isDateFromPassed in that case.
	 * 
	 */
	@Override
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
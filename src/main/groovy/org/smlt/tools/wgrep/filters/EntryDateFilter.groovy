package org.smlt.tools.wgrep.filters

import java.util.regex.Matcher
import java.text.SimpleDateFormat
import org.smlt.tools.wgrep.exceptions.TimeToIsOverduedException

class EntryDateFilter extends FilterBase{

	private SimpleDateFormat DATE_FORMAT
    private Date FROM_DATE
    private Date TO_DATE
    private boolean isDateFromPassed = false

    EntryDateFilter(FilterBase nextFilter_, def logDatePtrn_, def logDateFormat_, def from_, def to_) {
		super(nextFilter_, logDatePtrn_)
        DATE_FORMAT = logDateFormat_
        FROM_DATE = from_
        TO_DATE = to_
        if (isTraceEnabled()) trace("Added on top of " + nextFilter.getClass().getCanonicalName())
	}

    /**
    * Facade method to check if supplied entry suits desired date and time. 
    * Calls {@link dtChecker.check()} method if {@link DATE_TIME_FILTER} and <code>entry</code> are not null.
    *
    * @param entry A String to be checked
    */

    def filter(def blockData) {
        if (check(blockData))
        {
            if (nextFilter != null) 
            {
                if (isTraceEnabled()) trace("Passing to next filter")
                nextFilter.filter(blockData)    
            }
            else 
            {
                throw new RuntimeException("EntryDateFilter shouldn't be the last in chain")
            }
        }
        else
        {
            if (isTraceEnabled()) trace("EntryDateFilter not passed")
        }  
    }


    def check(def blockData)
    {
        if (blockData != null && filterPtrn != null)
        {
           
            def entryDate = null

            if (!isDateFromPassed || TO_DATE != null)
            {
                def timeString = null
                if (blockData instanceof String) 
                {
                    if (isTraceEnabled()) trace("Checking log entry " + blockData + " for log date pattern |" + filterPtrn + "| and formatting to |" +  DATE_FORMAT.toPattern() + "|")
                    Matcher entry = (blockData =~ filterPtrn)
                    if (entry.find())
                    {
                        timeString = entry.group(1)
                    }
                    else
                    {
                        if (isTraceEnabled()) trace("No signs of time in here")                        
                        return false
                    }
                }
                else if (blockData instanceof Matcher)
                {
                    if (isTraceEnabled()) trace("Checking matcher " + blockData.group()  + " and formatting to |" +  DATE_FORMAT.toPattern() + "|")                    
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
                return isDateFromPassed
            }

            if (entryDate != null && (FROM_DATE == null || FROM_DATE.compareTo(entryDate) <= 0))
            {
                isDateFromPassed = true
                if (TO_DATE != null)
                {
                    if (TO_DATE.compareTo(entryDate) >= 0)
                    {
                        if (isTraceEnabled()) trace("Passed TO_DATE")
                        return true
                    }
                    else
                    {
                        if (isTraceEnabled()) trace("Not passed")
                        throw new TimeToIsOverduedException(DATE_FORMAT.format(TO_DATE))
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
        return true
    }

    def clearState() {
        isDateFromPassed = false
        super.clearState()
    }
}
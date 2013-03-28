package org.smlt.tools.wgrep.filters

import java.util.regex.Matcher

class EntryDateFilter extends FilterBase{

	private SimpleDateFormat dateFormat
    private Date FROM_DATE
    private Date TO_DATE

    EntryDateFilter(def nextOne, def logDatePtrn_, def logDateFormat_, def from_, def to_) {
		super(nextOne, logDatePtrn_)
        dateFormat = logDateFormat_
        FROM_DATE = from_
        TO_DATE = to_
        if (isTraceEnabled()) trace("Added on top of " + nextFilter.getClass().getCanonicalName())
	}

    /**
    * Basic filter method.
    * <p> 
    * Is called against each block. Current sequence is following:
    * <li>1. Checks if block contains {@link FILTER_PATTERN}</li>
    * <li>2. Passes block and matching result to {@link processComplexBlock} method</li>
    * <li>3. Passes block to {@link postProcessBlockData} method</li>
    * <li>4. Passes the result of step 3 to {@link printBlock} method</li>
    *
    * @param blockData A String to be filtered.
    */

    def filter(def blockData)
    {
        if (entry != null && filterPtrn != null)
        {
            if (isTraceEnabled()) trace("Checking log entry " + entry + " for log date pattern |" + filterPtrn + "| and formatting to |" +  logDateFormat_.toPattern() + "|")
            
            def entryDate = null

            if (!isDateFromPassed || TO_DATE != null)
            {
                def dateMtchr = (entry =~ filterPtrn).find()
                entryDate =  logDateFormat_.parse(entry.group(1))
            }
            else
            {
                return isDateFromPassed
            }

            if (entryDate != null && (TO_DATE == null || FROM_DATE.compareTo(entryDate) <= 0))
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
                        throw new TimeToIsOverduedException(LOG_DATE_FORMAT.format(TO_DATE))
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

    }
}
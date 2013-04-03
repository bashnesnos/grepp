package org.smlt.tools.wgrep.filters

import java.util.regex.Matcher
import org.smlt.tools.wgrep.exceptions.TimeToIsOverduedException

class LogEntryFilter extends FilterBase{

    private boolean isBlockMatched = false;
    private StringBuilder curBlock = null;
    private def dateTimeChecker = null;

    LogEntryFilter(FilterBase nextFilter_, def filterPtrn_) {
        super(nextFilter_, filterPtrn_)
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
        Matcher entryMtchr = blockData =~ filterPtrn
        if ( entryMtchr.find() )
        {
            boolean isDateTimePassed = dateTimeChecker == null || dateTimeChecker.check(entryMtchr)
            
            if (!isDateTimePassed)
            {
                if (isTraceEnabled()) trace("Time not passed, invalidating")
                isBlockMatched = false
            }

            if (!isBlockMatched && isDateTimePassed)
            {
                isBlockMatched = true
                if (isTraceEnabled()) trace("appending")
                appendCurBlock(blockData)
            }
            else if (isBlockMatched)
            {
                if (isTraceEnabled()) trace("returning block")
                returnBlock()
                if (isTraceEnabled()) trace("appending end, since it is the start of new block")
                appendCurBlock(blockData)
            }
        }
        else if (isBlockMatched)
        {
            if (isTraceEnabled()) trace("appending")
            appendCurBlock(blockData)
        }
    }

    private void appendCurBlock(String line)
    {
        if (curBlock != null)
        {
            if (curBlock.length() != 0) curBlock = curBlock.append('\n')
            curBlock = curBlock.append(line)
        }
        else 
        {
            curBlock = new StringBuilder(line)
        }
    }
    
    private void returnBlock(def block)
    {
        if (nextFilter != null) 
        {
            if (isTraceEnabled()) trace("Passing to next filter")
            try {
                nextFilter.filter(curBlock.toString())                
            }
            catch(TimeToIsOverduedException e) {
                curBlock.setLength(0)
                throw e
            }
            finally
            {
                curBlock.setLength(0)
            }
        }
        else 
        {
            throw new RuntimeException("shouldn't be the last in chain")
        }
    }

    def clearState() {
        returnBlock()
        super.clearState()
    }

    def refresh() {
        setPattern(getFacade().getParam('LOG_ENTRY_PATTERN'))
        super.refresh()
    }
}
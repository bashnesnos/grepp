package org.smlt.tools.wgrep.filters

import java.util.regex.Matcher

class LogEntryFilter extends FilterBase{

    private boolean isBlockMatched = false;
    private StringBuilder curBlock = null;
    protected def dateTimeChecker = null;

    LogEntryFilter(def nextOne) {
        super(nextOne, getFacade().getParam('LOG_ENTRY_PATTERN'))
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
        if (isTraceEnabled()) trace("Filtering with /" + filterPtrn + "/")
        Matcher blockMtchr = blockData =~ filterPtrn
        if (blockMtchr.find()) 
        {
            if (nextFilter != null) 
            {
                if (isTraceEnabled()) trace("Passing to next filter")
                nextFilter.filter(blockData)    
            }
            else 
            {
                throw new RuntimeException("BasicFilter shouldn't be the last in chain")
            }
        }
        else
        {
            if (isTraceEnabled()) trace("BasicFilter not passed")
        }  
    }

        void processLine(String line, String pattern)
    {
        Matcher entryMtchr = line =~ pattern
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
                appendCurBlock(line)
            }
            else if (isBlockMatched)
            {
                if (isTraceEnabled()) trace("returning block")
                returnBlock(curBlock.toString())
                if (isTraceEnabled()) trace("appending end, since it is the start of new block")
                appendCurBlock(line)
            }
        }
        else if (isBlockMatched)
        {
            if (isTraceEnabled()) trace("appending")
            appendCurBlock(line)
        }
    }

    protected void appendCurBlock(String line)
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
    
    protected void returnBlock(def block)
    {
        filterChain.filter(block)
        curBlock.setLength(0)
    }

}
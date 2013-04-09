package org.smlt.tools.wgrep.filters

import groovy.util.logging.Slf4j;

import java.util.regex.Matcher
import org.smlt.tools.wgrep.WgrepConfig
import org.smlt.tools.wgrep.exceptions.TimeToIsOverduedException
import org.smlt.tools.wgrep.filters.enums.Event

@Slf4j
class LogEntryFilter extends FilterBase{

    private boolean isBlockMatched = false;
    private StringBuilder curBlock = null;
    private def dateTimeChecker = null;

    LogEntryFilter(FilterBase nextFilter_, WgrepConfig config) {
        super(nextFilter_, config)
		fillRefreshableParams()
        log.trace("Added on top of " + nextFilter.getClass().getCanonicalName())
    }
	
    boolean isConfigValid() {
        boolean checkResult = super.isConfigValid()
        if (getParam('LOG_ENTRY_PATTERN') == null)
        {
            log.warn('LOG_ENTRY_PATTERN is not specified')
        }
        return checkResult
    }

	void fillRefreshableParams() {
		setPattern(getParam('LOG_ENTRY_PATTERN'))
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
                log.trace("Time not passed, invalidating")
                isBlockMatched = false
            }

            if (!isBlockMatched && isDateTimePassed)
            {
                isBlockMatched = true
                log.trace("appending")
                appendCurBlock(blockData)
            }
            else if (isBlockMatched)
            {
                log.trace("returning block")
                returnBlock()
                log.trace("appending end, since it is the start of new block")
                appendCurBlock(blockData)
            }
        }
        else if (isBlockMatched)
        {
            log.trace("appending")
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
            log.trace("Passing to next filter")
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

    def processEvent(def event) {
        switch (event)
        {
            case Event.FILE_ENDED:
                returnBlock()
                break
            case Event.CONFIG_REFRESHED:
                fillRefreshableParams()
                break
            default:
                break
        }
        super.processEvent(event)
    }

}
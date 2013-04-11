package org.smlt.tools.wgrep.filters.entry

import groovy.util.logging.Slf4j;

import java.util.regex.Matcher
import org.smlt.tools.wgrep.config.WgrepConfig
import org.smlt.tools.wgrep.exceptions.TimeToIsOverduedException
import org.smlt.tools.wgrep.filters.enums.Event
import org.smlt.tools.wgrep.filters.FilterBase

/**
 * Class which provide grouping lines into log entries. <br>
 * Each entry has some pattern which defines it's start, and generally each entry's start is another one's end and vice-versa. <br>
 * This filter works on that principle.
 * 
 * @author Alexander Semelit 
 */

@Slf4j
class LogEntryFilter extends FilterBase{

    private boolean isBlockMatched = false;
    private def curBlock = null;

    /**
    * Creates filter on top of supplied fiter chain basing on supplied WgrepConfig instance.
    *
    * @param nextFilter_next filter in chain
    * @param config Instanntiated config instance
    */
    LogEntryFilter(FilterBase nextFilter_, WgrepConfig config) {
        super(nextFilter_, config)
		fillRefreshableParams()
        log.trace("Added on top of " + nextFilter.getClass().getCanonicalName())
    }
	
    @Override
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
    * Overrided check method. Once log entry start pattern was matched it accumulates data coming through until another entry start would be found. <br>
    * Once it was found, it passes all the data which was accumulated to the next filter. <br>
    *
    * @param blockData A String to be checked.
    */

    @Override
    boolean check(def blockData)
    {
        Matcher entryMtchr = blockData =~ filterPtrn
        if ( entryMtchr.find() )
        {
            if (!isBlockMatched)
            {
                isBlockMatched = true
                log.trace("appending")
                appendCurBlock(blockData)
            }
            else if (isBlockMatched)
            {
                log.trace("returning block")
                return true
            }
        }
        else if (isBlockMatched)
        {
            log.trace("appending")
            appendCurBlock(blockData)
        }
        return false
    }

    /**
    * Implementation of appender to accumulate lines
    *
    * @param line String to be appended
    */

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

    /**
    * Implementation of appender to start accumulating new block and to clear out previous block data from buffer.
    *
    * @param line String to be appended. If null was supplied simply clears previous data from buffer
    */

    private void startNewBlock(String line)
    {
        clearBuffer()
        if (line != null)
        {
            log.trace("appending end, since it is the start of new block")
            appendCurBlock(line)
        }
    }

    /**
    * Clears data from current buffer.
    *
    */

    private void clearBuffer()
    {
        curBlock.setLength(0)
    }

    /**
    * Overrided passNext, which passes accumulated block instead of recieved by filter method. <br>
    * Also it clears current buffer data, and starts new block accumulating.
    *
    * @return <code>super.passNext</code> result
    */
    
    @Override
    def passNext(def blockData)
    {
        String passingBlock = curBlock.toString()
        try {
            return super.passNext(passingBlock)                
        }
        catch(TimeToIsOverduedException e) {
            clearBuffer()
            throw e
        }
        finally
        {
            startNewBlock(blockData)
        }
    }

    /**
    *
    * Listens for FILE_ENDED event to flush current accumulated block, and CONFIG_REFRESHED to refresh params
    */

    @Override
    def processEvent(Event event) {
        switch (event)
        {
            case Event.FILE_ENDED:
                passNext(null)
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
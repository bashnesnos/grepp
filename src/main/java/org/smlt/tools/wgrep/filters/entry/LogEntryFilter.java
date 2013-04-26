package org.smlt.tools.wgrep.filters.entry;

import java.text.ParseException;
import java.util.regex.Pattern;

import org.smlt.tools.wgrep.exceptions.FilteringIsInterruptedException;
import org.smlt.tools.wgrep.exceptions.TimeToIsOverduedException;
import org.smlt.tools.wgrep.filters.enums.Event;
import org.smlt.tools.wgrep.filters.FilterBase;

/**
 * Class which provide grouping lines into log entries. <br>
 * Each entry has some pattern which defines it's start, and generally each
 * entry's start is another one's end and vice-versa. <br>
 * This filter works on that principle.
 * 
 * @author Alexander Semelit
 */

class LogEntryFilter extends FilterBase<String> {

	private boolean isBlockMatched = false;
	private StringBuilder curBlock = null;
	private Pattern logEntryPtrn = null;

	/**
	 * Creates filter on top of supplied fiter chain basing on supplied
	 * WgrepConfig instance.
	 * 
	 * @param nextFilter_next
	 *            filter in chain
	 * @param config
	 *            Instanntiated config instance
	 */
	LogEntryFilter(FilterBase<String> nextFilter_, String logEntryPtrn_) {
		super(nextFilter_, LogEntryFilter.class);
		logEntryPtrn = Pattern.compile(logEntryPtrn_);
	}

	/**
	 * Overrided check method. Once log entry start pattern was matched it
	 * accumulates data coming through until another entry start would be found. <br>
	 * Once it was found, it passes all the data which was accumulated to the
	 * next filter. <br>
	 * 
	 * @param blockData
	 *            A String to be checked.
	 */

	@Override
    public boolean check(Object blockData)
    {
    	if (blockData instanceof String)
    	{
    		if ( logEntryPtrn.matcher((String) blockData).find() ) //finding match of current blockData
    		{
    			if (!isBlockMatched)
    			{
    				isBlockMatched = true;
    				if (log.isTraceEnabled()) log.trace("appending");
    				appendCurBlock((String) blockData);
    			}
    			else if (isBlockMatched)
    			{
    				if (log.isTraceEnabled()) log.trace("returning block");
    				return true;
    			}
    		}
    		else if (isBlockMatched)
    		{
    			if (log.isTraceEnabled()) log.trace("appending");
    			appendCurBlock((String) blockData);
    		}
    		return false;
    	}
    	else throw new IllegalArgumentException("LogEntryFilter works with String only");
    }

	/**
	 * Implementation of appender to accumulate lines
	 * 
	 * @param line
	 *            String to be appended
	 */

	private void appendCurBlock(String line)
    {
        if (curBlock != null)
        {
            if (curBlock.length() != 0) curBlock = curBlock.append('\n');
            curBlock = curBlock.append(line);
        }
        else 
        {
            curBlock = new StringBuilder(line);
        }
    }

	/**
	 * Implementation of appender to start accumulating new block and to clear
	 * out previous block data from buffer.
	 * 
	 * @param line
	 *            String to be appended. If null was supplied simply clears
	 *            previous data from buffer
	 */

	private void startNewBlock(String line)
    {
        clearBuffer();
        if (line != null)
        {
            if (log.isTraceEnabled()) log.trace("appending end, since it is the start of new block");
            appendCurBlock(line);
        }
    }

	/**
	 * Clears data from current buffer.
	 * 
	 */

	private void clearBuffer()
    {
        curBlock.setLength(0);
    }

	/**
	 * Flushes all state
	 * 
	 */

	private void flush()
    {
        clearBuffer();
        passingVal = null;
    }

	/**
	 * Overrided passNext, which passes accumulated block instead of recieved by
	 * filter method. <br>
	 * Also it clears current buffer data, and starts new block accumulating.
	 * 
	 * @return <code>super.passNext</code> result
	 */

	@Override
    public void beforePassing(Object blockData)
    {
        passingVal = curBlock.toString();
        startNewBlock((String) blockData);
    }

	/**
	 * 
	 * Listens for FILE_ENDED event to flush current accumulated block, and
	 * CONFIG_REFRESHED to refresh params
	 * @throws TimeToIsOverduedException 
	 * @throws ParseException 
	 */

	@Override
	protected StringBuilder gatherPrintableState(Event event, StringBuilder agg) {
        switch (event)
        {
            case FILE_ENDED:
            	try {
            		appendNotNull(agg, (passNext(curBlock.toString())));
            	}
            	catch (FilteringIsInterruptedException e) {
            		log.error("Filtering interrupted by", e);
            	}
            	break;
            case FLUSH:
                flush();
                break;
            default:
                break;
        }
        return super.gatherPrintableState(event, agg);
    }
}
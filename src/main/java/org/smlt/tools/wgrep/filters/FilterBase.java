package org.smlt.tools.wgrep.filters;

import groovy.util.logging.Slf4j;

import org.smlt.tools.wgrep.filters.enums.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Super class for all filters. Provides filtering process template with hooking methods.<br>
 * 
 * @author Alexander Semelit 
 */

public abstract class FilterBase {
    protected FilterBase nextFilter;
    protected Object passingVal;
    private final Logger log = LoggerFactory.getLogger(FilterBase.class);

    /**
    * Constructor version for simple filters, which do not require a config.
    * @param nextFilter_ next filter to pass results to
    */

	public FilterBase(FilterBase nextFilter_) {
		nextFilter = nextFilter_;
	}
	

    /**
    * Main filtering method. Sequence is the following:
    * <li>1. {@link this.check()} is called</li>
    * <li>2a. If check returned true, {@link this.beforePassing()} is called</li>
    * <li>3a. {@link this.passNext()} is called</li>
    * <li>2b. If check returned false, <code>null</code> is returned</li>
    *
    * @param blockData which is going to be checked
    * @return result of {@link this.passNext} method if check passed, null otherwise
    */

    public Object filter(Object blockData)  {
        passingVal = null; //invalidating passingVal
        if (check(blockData))
        {
			beforePassing(blockData);
			return passNext(blockData);
        }
        else
        {
            if (log.isTraceEnabled()) log.trace("not passed");
			return null;
        }  
    }
    
    /**
    * Hook for data checking. Always returns true in base method.
    *
    * @param blockData which is going to be checked
    * @return true if data is ok to be passed, false otherwise
    */

    public boolean check(Object blockData)
    {
        return true;
    }

    /**
    *
    * Hook to do some actions before passing data further. Could be used to save state, or to extract something from needed data.
    * @param blockData which is ok to be passed further
    */

	public void beforePassing(Object blockData) {

	}
	
    /**
    *
    * Passes data further in chain. If there is no nextFilter, just returns blockData.
    *
    * @param blockData which is ok to be passed further
    * @return blockData if <code>this</code> is last in chain, result of <code>nextFilter.filter(blockData)</code> otherwise.
    */

    public Object passNext(Object blockData)
    {
        log.trace("attempting to pass to next filter");
        if (nextFilter != null)
        {
            log.trace("nextFilter " + nextFilter.getClass());
            return nextFilter.filter( passingVal != null ? passingVal : blockData );
        }
        else
        {
			log.trace("is last in chain");
			return passingVal != null ? passingVal : blockData ;
        }
    }

    /**
    *
    * Getter of next filter (which has it's own next filter etc.)
    *
    * @return nextFilter value
    */

    public FilterBase getTail()
    {
        return nextFilter;
    }

    /**
    * Base method for event processing. Simply passes it to the next filter and returns true if there is no next filter.
    *
    * @param event
    * @return result of <code>nextFilter.processEvent</code> and true if it doesn't have next filter (i.e. all filters in chain has processed that event).
    */

    public boolean processEvent(Event event) {
        log.trace("Passing event: " + event);
        if (nextFilter != null)
        {
            return nextFilter.processEvent(event);
        }
        return true;
    }
}
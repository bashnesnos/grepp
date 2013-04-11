package org.smlt.tools.wgrep.filters

import groovy.util.logging.Slf4j;

import org.smlt.tools.wgrep.ModuleBase
import org.smlt.tools.wgrep.WgrepConfig
import org.smlt.tools.wgrep.filters.enums.*

/**
 * Super class for all filters. Provides filtering process template with hooking methods.<br>
 * 
 * @author Alexander Semelit 
 */

@Slf4j
class FilterBase extends ModuleBase {
    protected FilterBase nextFilter
    protected def filterPtrn

    /**
    * Full constructor.
    * @param nextFilter_ next filter to pass results to
    * @param filterPtrn_ pattern which can be used for filtering
    * @param config initialized WgrepConfig instance
    */

	FilterBase(FilterBase nextFilter_, def filterPtrn_, WgrepConfig config) {
		super(config)
		nextFilter = nextFilter_
		filterPtrn = filterPtrn_
	}

    /**
    * Cut constructor version. Used for filters providing self-configuring by config
    * @param nextFilter_ next filter to pass results to
    * @param config initialized WgrepConfig instance
    */

	FilterBase(FilterBase nextFilter_, WgrepConfig config) {
		this(nextFilter_, null, config)
	}
	
    /**
    * Constructor version for simple filters, which do not require a config.
    * @param nextFilter_ next filter to pass results to
    */

	FilterBase(FilterBase nextFilter_) {
		nextFilter = nextFilter_
	}
	
    /**
    * Setter for filter pattern
    * @param ptrn value to be set as filter pattern
    */

    void setPattern(def ptrn) {
        filterPtrn = ptrn
        log.trace("Set filter pattern to /" + filterPtrn + "/")
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

    def filter(def blockData)  {
        if (check(blockData))
        {
			beforePassing(blockData)
			return passNext(blockData)
        }
        else
        {
            log.trace("not passed")
			return null
        }  
    }
    
    /**
    * Hook for data checking. Always returns true in base method.
    *
    * @param blockData which is going to be checked
    * @return true if data is ok to be passed, false otherwise
    */

    boolean check(def blockData)
    {
        return true
    }

    /**
    *
    * Hook to do some actions before passing data further. Could be used to save state, or to extract something from needed data.
    * @param blockData which is ok to be passed further
    */

	void beforePassing(def blockData) {
		
	}
	
    /**
    *
    * Passes data further in chain. If there is no nextFilter, just returns blockData.
    *
    * @param blockData which is ok to be passed further
    * @return blockData if <code>this</code> is last in chain, result of <code>nextFilter.filter(blockData)</code> otherwise.
    */

    def passNext(def blockData)
    {
        log.trace("attempting to pass to next filter")
        if (nextFilter != null)
        {
            return nextFilter.filter(blockData)
        }
        else
        {
			log.warn("is last in chain")
			return blockData
        }
    }

    /**
    *
    * Getter of next filter (which has it's own next filter etc.)
    *
    * @return nextFilter value
    */

    FilterBase getTail()
    {
        return nextFilter
    }

    /**
    * Base method for event processing. Simply passes it to the next filter and returns true if there is no next filter.
    *
    * @param event
    * @return result of <code>nextFilter.processEvent</code> and true if it doesn't have next filter (i.e. all filters in chain has processed that event).
    */

    def processEvent(Event event) {
        log.trace("Passing event: " + event)
        if (nextFilter != null)
        {
            return nextFilter.processEvent(event)
        }
        return true
    }
}
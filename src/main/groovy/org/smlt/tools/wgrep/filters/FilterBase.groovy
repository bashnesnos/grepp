package org.smlt.tools.wgrep.filters

import groovy.util.logging.Slf4j;

import org.smlt.tools.wgrep.ModuleBase
import org.smlt.tools.wgrep.WgrepConfig
import org.smlt.tools.wgrep.filters.enums.*

@Slf4j
class FilterBase extends ModuleBase {
    protected FilterBase nextFilter
    protected def filterPtrn

	FilterBase(FilterBase nextFilter_, def filterPtrn_, WgrepConfig config) {
		super(config)
		nextFilter = nextFilter_
		filterPtrn = filterPtrn_
	}


	FilterBase(FilterBase nextFilter_, WgrepConfig config) {
		this(nextFilter_, null, config)
	}
	
	FilterBase(FilterBase nextFilter_) {
		nextFilter = nextFilter_
	}
	
	void makeLast()
	{
		isLast = true
	}
	
    void setPattern(def ptrn) {
        filterPtrn = ptrn
        log.trace("Set filter pattern to /" + filterPtrn + "/")
    }

	boolean check(def blockData)
	{
		return true
	}
	
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

	void beforePassing(def blockData) {
		
	}
	
    def passNext(def passingVal)
    {
        log.trace("attempting to pass to next filter")
        if (nextFilter != null)
        {
            return nextFilter.filter(passingVal)
        }
        else
        {
			log.warn("is last in chain")
			return passingVal
        }
    }

    FilterBase getTail()
    {
        return nextFilter
    }

    def processEvent(def event) {
        log.trace("Passing event: " + event)
        if (nextFilter != null)
        {
            nextFilter.processEvent(event)
        }
    }
}
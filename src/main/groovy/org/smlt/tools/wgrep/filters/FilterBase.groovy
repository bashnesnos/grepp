package org.smlt.tools.wgrep.filters

import org.smlt.tools.wgrep.ModuleBase
import org.smlt.tools.wgrep.filters.enums.*

class FilterBase extends ModuleBase {
    protected nextFilter
    protected filterPtrn
    protected boolean isLast

    FilterBase(FilterBase nextFilter_, def filterPtrn_) {
        nextFilter = nextFilter_
        filterPtrn = filterPtrn_
        isLast = false
    }

    FilterBase(FilterBase nextFilter_, def filterPtrn_, boolean isLast_) {
        nextFilter = nextFilter_
        filterPtrn = filterPtrn_
        isLast = isLast_
    }

    def setPattern(def ptrn) {
        filterPtrn = ptrn
        if (isTraceEnabled()) trace("Set filter pattern to /" + filterPtrn + "/")
    }

    def filter(def blockData)  {
        passNext(blockData)
    }

    def passNext(def passingVal)
    {
        if (isTraceEnabled()) trace("attempting to pass to next filter")
        if (nextFilter != null)
        {
            nextFilter.filter(passingVal)
        }
        else
        {
            if (!isLast) throw new RuntimeException("shouldn't be the last in chain")
        }
    }

    FilterBase getTail()
    {
        return nextFilter
    }

    def processEvent(def event) {
        if (isTraceEnabled()) trace("Passing event: " + event)
        if (nextFilter != null)
        {
            nextFilter.processEvent(event)
        }
    }
}
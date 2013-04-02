package org.smlt.tools.wgrep.filters

import org.smlt.tools.wgrep.ModuleBase

class FilterBase extends ModuleBase {
    protected nextFilter
    protected filterPtrn

    FilterBase(FilterBase nextFilter_, def filterPtrn_) {
        nextFilter = nextFilter_
        filterPtrn = filterPtrn_
    }

    def setPattern(def ptrn) {
        filterPtrn = ptrn
    }

    def filter(def blockData)  {
        throw new UnsupportedOperationException('Method of base class shoulnd\'t be used')
    }

    FilterBase getTail()
    {
        return nextFilter
    }

    def clearState() {
        if (isTraceEnabled()) trace("Dummy finalization")
        if (nextFilter != null)
        {
            nextFilter.clearState()
        }
    }

    def refresh() {
        if (isTraceEnabled()) trace("Dummy refresh")
        if (nextFilter != null)
        {
            nextFilter.refresh()
        }        
    }
}
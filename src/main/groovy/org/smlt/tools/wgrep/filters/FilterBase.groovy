package org.smlt.tools.wgrep.filters

class FilterBase extends ModuleBase {
    protected nextFilter
    protected filterPtrn

    FilterBase(def nextFilter, def filterPtrn_) {
        nextFilter = nextOne_
        filterPtrn = filterPtrn_
    }

    def setPattern(def ptrn) {
        filterPtrn = ptrn
    }

    def filter(def blockData)  {
        throw new UnsupportedOperationException('Method of base class shoulnd\'t be used')
    }
}
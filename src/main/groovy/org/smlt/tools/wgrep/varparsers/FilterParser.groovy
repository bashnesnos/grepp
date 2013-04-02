package org.smlt.tools.wgrep.varparsers

class FilterParser extends DefaultVarParser
{
    def parseVar(def arg)
    {
        if (isTraceEnabled()) trace("Parsing var: " + arg)
        getFacade().setFilterPattern(arg)
        getFacade().unsubscribeVarParsers([this])
    }

}
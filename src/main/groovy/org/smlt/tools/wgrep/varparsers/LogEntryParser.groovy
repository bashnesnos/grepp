package org.smlt.tools.wgrep.varparsers

class LogEntryParser extends DefaultVarParser
{
    def parseVar(def arg)
    {
        if (isTraceEnabled()) trace("Parsing var: " + arg)
        getFacade().setLogEntryPattern(arg)
        getFacade().unsubscribeVarParsers([this])
    }

}
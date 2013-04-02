package org.smlt.tools.wgrep.varparsers

class DateTimeParser extends DefaultVarParser
{

    def parseVar(def arg)
    {
        if (isTraceEnabled()) trace("Additional var: " + arg)
        if (getFacade().getParam('FROM_DATE') == null) setDateFrom(arg)
        else if (getFacade().getParam('TO_DATE') == null) 
        {
            setDateTo(arg)
            getFacade().unsubscribeVarParsers([this])
        }
    }

    def setDateFrom(def date)
    {
        getFacade().setParam('FROM_DATE', date)
    }

    def setDateTo(def date)
    {
        getFacade().setParam('TO_DATE', date)
    }
}
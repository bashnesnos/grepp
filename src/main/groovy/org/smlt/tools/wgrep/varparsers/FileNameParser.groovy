package org.smlt.tools.wgrep.varparsers

class FileNameParser extends DefaultVarParser
{
    def parseVar(def arg)
    {
        if (isTraceEnabled()) trace("Parsing var: " + arg)
        getFacade().setFileName(arg)
        //not unsubsrcibing since there could be more than one file
    }

}
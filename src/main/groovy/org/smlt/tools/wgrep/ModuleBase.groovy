package org.smlt.tools.wgrep

class ModuleBase
{
    static WgrepFacade getFacade()
    {
        return WgrepFacade.getInstance()
    }

    def isVerboseEnabled()
    {
        return getFacade().isVerboseEnabled()
    }

    def verbose(def text)
    {
		getFacade().verbose(text)
    }

    def isTraceEnabled()
    {
        return getFacade().isTraceEnabled()
    }
    
    def trace(def text)
    {
		getFacade().trace("[" + this.getClass().getCanonicalName() + "] " + text)
    }

    def getRoot()
    {
   	    return getFacade().getRoot()
    }

    def getCDATA(def node)
    {
        return getFacade().getCDATA(node)
    }

}
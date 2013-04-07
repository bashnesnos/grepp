package org.smlt.tools.wgrep

class ModuleBase
{
    def isTraceEnabled = getFacade().isTraceEnabled()
    def isVerboseEnabled = getFacade().isVerboseEnabled()

    static WgrepFacade getFacade()
    {
        return WgrepFacade.getInstance()
    }

    def isVerboseEnabled()
    {
        return isVerboseEnabled
    }

    def verbose(def text)
    {
		getFacade().verbose(text)
    }

    def isTraceEnabled()
    {
        return isTraceEnabled
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

    def refresh()
    {
        
    }

}
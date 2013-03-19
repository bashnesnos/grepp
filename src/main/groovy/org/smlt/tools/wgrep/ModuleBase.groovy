package org.smlt.tools.wgrep

class ModuleBase
{
    static WgrepFacade getFacade()
    {
        return WgrepFacade.getInstance()
    }

    def verbose(def text)
    {
		getFacade().verbose(text)
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
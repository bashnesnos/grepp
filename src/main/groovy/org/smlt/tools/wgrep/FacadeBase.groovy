package org.smlt.tools.wgrep

class FacadeBase
{
    def callingClass

    WgrepFacade getFacade()
    {
        return WgrepFacade.getInstance()
    }

    def verbose(def text)
    {
		getFacade().verbose(text)
    }
    
    def trace(def text)
    {
		getFacade().trace(callingClass + text)
    }

    def getRoot()
    {
   	    return getFacade().getRoot()
    }

    def getCDATA(def node)
    {
        return getFacade().getCDATA(node)
    }

    def setCallingClass(def clazz)
    {
        this.callingClass = "[" + clazz.getCanonicalName() + "] "
    }

}
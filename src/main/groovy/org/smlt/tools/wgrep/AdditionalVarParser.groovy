package org.smlt.tools.wgrep

class AdditionalVarParser extends ModuleBase
{
	
	def subscribe()
	{
		getFacade().subscribeVarParsers([this])
	}

	def unsubscribe()
	{
		getFacade().unsubscribeVarParsers([this])
	}

	def parseVar(def arg)
    {
        if (isTraceEnabled()) trace("Not implemented ")
    }
}
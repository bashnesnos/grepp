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
        trace("Not implemented ")
    }
}
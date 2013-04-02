package org.smlt.tools.wgrep.varparsers

import org.smlt.tools.wgrep.ModuleBase

class DefaultVarParser extends ModuleBase
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
        throw new UnsupportedOperationException('Method of base class shoulnd\'t be used')
    }
}
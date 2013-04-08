package org.smlt.tools.wgrep.varparsers

import groovy.util.logging.Slf4j;

import org.smlt.tools.wgrep.ModuleBase

@Slf4j
class DefaultVarParser extends ModuleBase
{
	
	def subscribe()
	{
		getFacade().subscribeParser([this])
	}

	def unsubscribe()
	{
		getFacade().unsubscribeParser([this])
	}

	def parseVar(def arg)
    {
        throw new UnsupportedOperationException('Method of base class shoulnd\'t be used')
    }
}
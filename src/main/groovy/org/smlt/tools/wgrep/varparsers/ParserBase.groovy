package org.smlt.tools.wgrep.varparsers

import org.smlt.tools.wgrep.WgrepConfig;
import groovy.util.logging.Slf4j;
import org.smlt.tools.wgrep.ModuleBase

@Slf4j
class ParserBase extends ModuleBase
{

	ParserBase(WgrepConfig config)
	{
		super(config)
	}
		
	def subscribe()
	{
		configInstance.subscribeVarParsers([this])
	}

	def unsubscribe()
	{
		configInstance.unsubscribeVarParsers([this])
	}

	void parseVar(def arg)
    {
        throw new UnsupportedOperationException('Method of base class shoulnd\'t be used')
    }
}
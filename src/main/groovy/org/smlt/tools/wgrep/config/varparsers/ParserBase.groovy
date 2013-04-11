package org.smlt.tools.wgrep.config.varparsers

import org.smlt.tools.wgrep.config.ModuleBase;
import org.smlt.tools.wgrep.config.WgrepConfig
import groovy.util.logging.Slf4j

/**
 * 
 * Provides base methods for an argument parser.
 * 
 * @author Alexander Semelit
 *
 */
@Slf4j
class ParserBase extends ModuleBase
{

	ParserBase(WgrepConfig config)
	{
		super(config)
	}
	
	/**
	 * Subscribes <code>this</code> as a varPasers of supplied WgrepConfig instance
	 * 	
	 */
	void subscribe()
	{
		configInstance.subscribeVarParsers([this])
	}

	/**
	 * Unsubscribes <code>this</code> from varPasers of supplied WgrepConfig instance
	 * 
	 */
	void unsubscribe()
	{
		configInstance.unsubscribeVarParsers([this])
	}

	/**
	 * Base method to do parsing. Is called by WgrepConfig to parse an argument.
	 * 
	 * @throws UnsupportedOperationException as it should be overriden by sub-class
	 * @param arg argument to be parsed
	 */
	void parseVar(def arg)
    {
        throw new UnsupportedOperationException('Method of base class shoulnd\'t be used')
    }
}
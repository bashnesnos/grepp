package org.smlt.tools.wgrep.config.varparsers

import groovy.util.logging.Slf4j;
import org.smlt.tools.wgrep.config.WgrepConfig;

/**
 * 
 * Provides filter pattern parameter parsing. <br>
 * Simply sets supplied argument as value of FILTER_PATTERN parameter of WgrepConfig instance.
 *  
 * @author Alexander Semelit
 *
 */
@Slf4j
class FilterParser extends ParserBase
{
	FilterParser(WgrepConfig config)
	{
		super(config)
	}
	
	/**
	 * Sets filter pattern and unsubsribes.
	 * 
	 */
	@Override
    void parseVar(def arg)
    {
        log.trace("Parsing var: " + arg)
        setFilterPattern(arg)
        unsubscribe()
    }

}
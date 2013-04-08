package org.smlt.tools.wgrep.varparsers

import groovy.util.logging.Slf4j;
import org.smlt.tools.wgrep.WgrepConfig;

@Slf4j
class FilterParser extends ParserBase
{
	FilterParser(WgrepConfig config)
	{
		super(config)
	}
	
    void parseVar(def arg)
    {
        log.trace("Parsing var: " + arg)
        setFilterPattern(arg)
        unsubscribe()
    }

}
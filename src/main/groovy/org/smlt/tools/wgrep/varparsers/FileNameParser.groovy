package org.smlt.tools.wgrep.varparsers

import groovy.util.logging.Slf4j;
import org.smlt.tools.wgrep.WgrepConfig

@Slf4j
class FileNameParser extends ParserBase
{
	FileNameParser(WgrepConfig config)
	{
		super(config)
	}
	
    void parseVar(def arg)
    {
        log.trace("Parsing var: " + arg)
        addFileName(arg)
        //not unsubsrcibing since there could be more than one file
    }

}
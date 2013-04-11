package org.smlt.tools.wgrep.config.varparsers

import groovy.util.logging.Slf4j;
import org.smlt.tools.wgrep.config.WgrepConfig

/**
 * Provides file name parameter parsing. <br>
 * Simply adds supplied argument to FILES parameter of WgrepConfig instance
 * 
 * @author Alexander Semelit
 *
 */
@Slf4j
class FileNameParser extends ParserBase
{
	FileNameParser(WgrepConfig config)
	{
		super(config)
	}
	
	/**
	 * Never unsubscribes, since there could be supplied more than one filename.
	 * 
	 */
	@Override
    void parseVar(def arg)
    {
        log.trace("Parsing var: " + arg)
        addFileName(arg)
        //not unsubsrcibing since there could be more than one file
    }

}
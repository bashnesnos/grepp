package org.smlt.tools.wgrep.config.varparsers

import groovy.util.logging.Slf4j;
import org.smlt.tools.wgrep.config.WgrepConfig

/**
 *
 * Provides log entry pattern parameter parsing. <br>
 * Simply sets supplied argument as value of LOG_ENTRY_PATTERN parameter of WgrepConfig instance.
 *
 * @author Alexander Semelit
 *
 */
@Slf4j
class LogEntryParser extends ParserBase
{
	LogEntryParser(WgrepConfig config)
	{
		super(config)
	}
	/**
	 * Sets log entry pattern and unsubscribes
	 */
	@Override
	void parseVar(def arg)
    {
        log.trace("Parsing var: {}", arg)
        setLogEntryPattern(arg)
        unsubscribe()
    }

}
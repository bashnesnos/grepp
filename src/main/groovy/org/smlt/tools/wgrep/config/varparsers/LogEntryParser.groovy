package org.smlt.tools.wgrep.config.varparsers

import java.util.Map;

import org.smlt.tools.wgrep.config.ConfigHolder;
import org.smlt.tools.wgrep.config.Param;

import groovy.util.logging.Slf4j;

/**
 *
 * Provides log entry pattern parameter parsing. <br>
 * Simply sets supplied argument as value of LOG_ENTRY_PATTERN parameter of WgrepConfig instance.
 *
 * @author Alexander Semelit
 *
 */
@Slf4j
class LogEntryParser implements ParamParser<String>
{

	/**
	 * Sets log entry pattern and unsubscribes
	 */

	@Override
	public boolean parseVar(ConfigHolder config, Map<Param, ?> params,
			String arg) {
		
		log.trace("Parsing var: {}", arg)
        params[Param.LOG_ENTRY_PATTERN] = arg
		return true;
	}

}
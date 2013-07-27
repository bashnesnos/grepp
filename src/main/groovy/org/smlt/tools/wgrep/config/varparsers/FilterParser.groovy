package org.smlt.tools.wgrep.config.varparsers

import java.util.Map;

import groovy.util.logging.Slf4j;

import org.smlt.tools.wgrep.config.ConfigHolder;
import org.smlt.tools.wgrep.config.Param

/**
 * 
 * Provides filter pattern parameter parsing. <br>
 * Simply sets supplied argument as value of FILTER_PATTERN parameter of WgrepConfig instance.
 *  
 * @author Alexander Semelit
 *
 */
@Slf4j
class FilterParser implements ParamParser<String>
{

	@Override
	public boolean parseVar(ConfigHolder config, Map params, String arg) {
		return parseVar(params, arg)
	}

	public boolean parseVar(Map params, String arg) {
        log.trace("Parsing var: {}", arg)
        params[Param.FILTER_PATTERN] = arg
        return true
	}

}
package org.smltools.grepp.cli.varparsers

import java.util.Map;

import groovy.util.logging.Slf4j;

import groovy.util.ConfigObject;

/**
 * 
 * Provides filter pattern parameter parsing. <br>
 * Simply sets supplied argument as value of FILTER_PATTERN parameter of WgrepConfig instance.
 *  
 * @author Alexander Semelit
 *
 */
@Slf4j
class FilterParser implements ParamParser<String> {
	private final static String FILTER_PATTERN_KEY = "filterPattern"

	@Override
	public boolean parseVar(ConfigObject config, String arg) {
        log.trace("Parsing var: {}", arg)
        config.runtime."$FILTER_PATTERN_KEY" = arg
        return true
	}

}
package org.smlt.tools.wgrep.config.varparsers

import org.smlt.tools.wgrep.config.ConfigHolder
import org.smlt.tools.wgrep.config.Param
import groovy.util.logging.Slf4j

/**
 * 
 * Provides base methods for an argument parser.
 * 
 * @author Alexander Semelit
 *
 */

interface ParamParser<T>
{
	boolean parseVar(ConfigHolder config, Map<Param, ?> params, T arg);
}
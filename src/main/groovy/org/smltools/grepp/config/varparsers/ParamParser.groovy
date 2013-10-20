package org.smltools.grepp.config.varparsers

import org.smltools.grepp.config.ConfigHolder
import org.smltools.grepp.config.Param
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
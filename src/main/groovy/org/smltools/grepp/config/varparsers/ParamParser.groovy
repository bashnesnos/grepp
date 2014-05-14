package org.smltools.grepp.config.varparsers

import groovy.util.ConfigObject
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
	boolean parseVar(ConfigObject config, T arg);
}
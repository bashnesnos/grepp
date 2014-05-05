package org.smltools.grepp.output;

import groovy.util.logging.Slf4j

import org.smltools.grepp.config.ParamHolder
import org.smltools.grepp.config.Param
import org.smltools.grepp.filters.enums.Event
import org.smltools.grepp.filters.FilterBase
import org.smltools.grepp.filters.FilterChainFactory;
import org.smltools.grepp.config.ConfigHolder
import groovy.util.ConfigSlurper
/**
 * 
 * Output writing directly to config.xml
 * 
 * @author Alexander Semelit
 *
 */

@Slf4j
final class ConfigOutput extends SimpleOutput {
	
	private ConfigHolder config;
	
	public ConfigOutput(ParamHolder paramsHolder) {
    	super(paramsHolder, null)
    	config = paramsHolder.get(Param.CONFIG)
    	if (config == null) {
			throw new IllegalArgumentException("ConfigHolder is null")
    	}
   	}


	@Override
	protected void printNotFiltered(Object data) {
		if (data instanceof String || data instanceof StringBuilder) {
			config.mergeAndSave(new ConfigSlurper().parse(data.toString()))
		}
		else {
			log.debug("No custom config found")
		}
	}

}
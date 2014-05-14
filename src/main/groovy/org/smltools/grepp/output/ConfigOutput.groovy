package org.smltools.grepp.output;

import groovy.util.logging.Slf4j
import org.smltools.grepp.filters.FilterChain
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
final class ConfigOutput extends SimpleOutput<String> {
		
	public ConfigOutput(ConfigHolder config, FilterChain<String> filterChain) {
            super(config, filterChain)
   	}


	@Override
	protected void printNotFiltered(String data) {
		if (data != null) {
			log.trace("Got config: {}", data)
            config.mergeAndSave(new ConfigSlurper().parse(data))
		}
		else {
            log.debug("No custom config found")
		}
	}

}
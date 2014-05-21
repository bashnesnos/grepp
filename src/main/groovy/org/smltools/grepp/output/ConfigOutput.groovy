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

@Slf4j("LOGGER")
public class ConfigOutput extends SimpleOutput<String> {
		
	public ConfigOutput(ConfigHolder config, FilterChain<String> filterChain) {
        super(config, filterChain)
   	}

	@Override
	protected void printNotFiltered(String data) {
		if (data != null) {
			LOGGER.trace("Got config: {}", data)
            config.merge(new ConfigSlurper().parse(data))
            config.save()
		}
		else {
            LOGGER.debug("No custom config found")
		}
	}
	
}
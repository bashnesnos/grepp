package org.smlt.tools.wgrep.output;

import groovy.util.logging.Slf4j
import org.smlt.tools.wgrep.filters.enums.Event
import org.smlt.tools.wgrep.filters.FilterChainFactory
import org.smlt.tools.wgrep.filters.FilterBase


/**
 * 
 * Output writing directly to config.xml
 * 
 * @author Alexander Semelit
 *
 */

@Slf4j
class ConfigOutput implements WgrepOutput {
	
	protected FilterChainFactory filterFactory;
	protected FilterBase filterChain;
	
	ConfigOutput(FilterChainFactory filterFactory_) {
		filterFactory = filterFactory_
		filterChain = filterFactory.createFilterChain()
	}

	/**
	 * Refreshes filters/filtering params by some criteria.
	 * 
	 * @param criteria Something that can be used for config refreshing. Filename for example
	 */
	void refreshFilters(Object criteria) {

	}
	
	/**
	 * Ensures that event is correctly printed to output. Nothing more or less.
	 * 
	 * @param event Event to be printed
	 */
	void processEvent(Event event) {
	
	}
	
	/**
	 * Ensures that data is printed to output. Nothing more or less.
	 * 
	 * @param data Data to be printed
	 */
	void printToOutput(Object data) {

	}
	
	/**
	 * 
	 * Releases output resources
	 * 
	 */
	void closeOutput() {

	}
	
}
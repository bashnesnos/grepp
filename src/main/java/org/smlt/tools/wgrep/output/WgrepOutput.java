package org.smlt.tools.wgrep.output;

import org.smlt.tools.wgrep.filters.enums.Event;

/**
 * Provides abstraction for possible output like console, file output etc. 
 * 
 * @author Alexander Semelit
 *
 */
public interface WgrepOutput<E, V> {

	/**
	 * Refreshes filters/filtering params by some criteria.
	 * 
	 * @param criteria Something that can be used for config refreshing. Filename for example
	 */
	void refreshFilters(V criteria);
	
	/**
	 * Ensures that event is correctly printed to output. Nothing more or less.
	 * 
	 * @param event Event to be printed
	 */
	void processEvent(Event event);
	
	/**
	 * Ensures that data is printed to output. Nothing more or less.
	 * 
	 * @param data Data to be printed
	 */
	void printToOutput(E data);
	
	/**
	 * 
	 * Releases output resources
	 * 
	 */
	void closeOutput();
}

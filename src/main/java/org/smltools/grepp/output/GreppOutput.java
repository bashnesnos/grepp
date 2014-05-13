package org.smltools.grepp.output;

import org.smltools.grepp.filters.enums.Event;

/**
 * Provides abstraction for possible output like console, file output etc. 
 * 
 * @author Alexander Semelit
 * @param <T>
 *
 */
public interface GreppOutput<T> {
        
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
	void printToOutput(T data);
	
    void flush();
        
	/**
	 * 
	 * Releases output resources
	 * 
	 */
	void closeOutput();
}

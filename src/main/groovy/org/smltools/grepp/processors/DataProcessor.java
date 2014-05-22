package org.smltools.grepp.processors;

/**
 *
 * Interface for all kinds processing. E.g. analysing based on file processing, pipe processing etc.
 * 
 * @author Alexander Semelit
 * @param <T>
 *
 */
public interface DataProcessor<T> {
	/**
	 * 
	 * Current method should take care of both doing analysis and providing results, either printing to file, console or whatever. <br>
	 * It is assumed that processor is appropriately initialized before the method process is called.
	 * 
         * @param data
	 */
	void process(T data);
}

package org.smlt.tools.wgrep.processors;

/**
 *
 * Interface for all kinds processing. E.g. analysing based on file processing, pipe processing etc.
 * 
 * @author Alexander Semelit
 *
 */
public interface DataProcessor<E> {
	/**
	 * 
	 * Current method should take care of both doing analysis and providing results, either printing to file, console or whatever. <br>
	 * It is assumed that processor is appropriately initialized before the method process is called.
	 * 
	 */
	void process(E data);
}

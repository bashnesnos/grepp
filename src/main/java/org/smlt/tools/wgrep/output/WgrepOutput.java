package org.smlt.tools.wgrep.output;

/**
 * Provides abstraction for possible output like console, file output etc. 
 * 
 * @author Alexander Semelit
 *
 */
public interface WgrepOutput {

	/**
	 * Ensures that data is printed to output. Nothing more or less.
	 * 
	 * @param data Data to be printed
	 */
	void printToOutput(Object data);
	
	/**
	 * 
	 * Releases output resources
	 * 
	 */
	void closeOutput();
}

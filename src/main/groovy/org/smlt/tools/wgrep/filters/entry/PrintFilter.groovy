package org.smlt.tools.wgrep.filters.entry

import groovy.util.logging.Slf4j
import org.smlt.tools.wgrep.filters.FilterBase

/**
 * Filter which simply prints all data passed to it, if it is not null.
 * 
 * @author Alexander Semelit
 *
 */
@Slf4j
class PrintFilter extends FilterBase{

	/**
	 * Simply delegates to super method.
	 * 
	 * @param nextFilter_ next in chain
	 */
	PrintFilter(FilterBase nextFilter_) {
		super(nextFilter_)
	}

	
	/**
	 * Checks if passed data is not null
	 */
    @Override
	boolean check(def blockData) {
		return blockData != null
	}

	/**
	 * Prints block data to System.out
	 */
	@Override
	void beforePassing(def blockData) {
		println blockData
	}

}
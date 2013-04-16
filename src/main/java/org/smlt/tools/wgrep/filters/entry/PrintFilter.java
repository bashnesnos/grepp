package org.smlt.tools.wgrep.filters.entry;

import org.smlt.tools.wgrep.filters.FilterBase;

/**
 * Filter which simply prints all data passed to it, if it is not null.
 * 
 * @author Alexander Semelit
 *
 */

class PrintFilter extends FilterBase{

	/**
	 * Simply delegates to super method.
	 * 
	 * @param nextFilter_ next in chain
	 */
	PrintFilter(FilterBase nextFilter_) {
		super(nextFilter_, PrintFilter.class);
	}

	
	/**
	 * Checks if passed data is not null
	 */
    @Override
	public boolean check(Object blockData) {
		return blockData != null;
	}

	/**
	 * Prints block data to System.out
	 */
	@Override
	public void beforePassing(Object blockData) {
		System.out.println(blockData);
	}

}
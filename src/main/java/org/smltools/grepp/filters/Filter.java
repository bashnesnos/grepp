package org.smltools.grepp.filters;

import org.smltools.grepp.exceptions.FilteringIsInterruptedException;

public interface Filter<T> {
	/**
	 * Main filtering method. 
	 * 
	 * @param blockData
	 *            which is going to be filtered
	 * @return filtered data
	 * @throws FilteringIsInterruptedException
	 */

	T filter(T blockData) throws FilteringIsInterruptedException;
}
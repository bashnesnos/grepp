package org.smltools.grepp.filters;

public interface Filter<T> {
	/**
	 * Main filtering method. Sequence is the following: <li>1. {@link
	 * this.check()} is called</li> <li>2a. If check returned true, {@link
	 * this.beforePassing()} is called</li> <li>3a. {@link this.passNext()} is
	 * called</li> <li>2b. If check returned false, <code>null</code> is
	 * returned</li>
	 * 
	 * @param blockData
	 *            which is going to be checked
	 * @return result of {@link this.passNext} method if check passed, null
	 *         otherwise
	 * @throws FilteringIsInterruptedException
	 * @throws ParseException
	 */

	T filter(T blockData) throws FilteringIsInterruptedException;
}
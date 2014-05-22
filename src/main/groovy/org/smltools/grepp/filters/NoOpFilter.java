package org.smltools.grepp.filters;

import org.smltools.grepp.exceptions.FilteringIsInterruptedException;


public abstract class NoOpFilter<T> implements Filter<T> {
	private NoOpFilter() {
		throw new AssertionError("Nobody is about to instantiate this one!");
	}
}
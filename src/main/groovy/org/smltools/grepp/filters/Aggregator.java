package org.smltools.grepp.filters;

public interface Aggregator<T> {
	Aggregator<T> add(T data);
	T aggregate();
}
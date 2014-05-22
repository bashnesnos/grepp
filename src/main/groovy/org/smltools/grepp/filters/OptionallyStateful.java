package org.smltools.grepp.filters;

public interface OptionallyStateful<T> extends Stateful<T>{
	boolean isStateful();
}
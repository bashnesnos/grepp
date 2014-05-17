package org.smltools.grepp.filters;

import java.util.Map;

public interface PostFilterGroupMethod<T> extends PostFilterMethod<T> {
	String getAggregatorKey();
	T processGroup(Map group);
}
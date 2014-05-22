package org.smltools.grepp.filters;

import java.util.Map;

public interface ReportGroupMethod<T> extends ReportMethod<T> {
	String getAggregatorKey();
	T processGroup(Map group);
}
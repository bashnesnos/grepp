package org.smltools.grepp.filters;

public interface Refreshable {
	void lock();
	boolean refreshByConfigId(String configId);
}
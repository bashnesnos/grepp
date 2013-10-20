package org.smltools.grepp.config;

public interface ParamsHolderFactory<V> {
	ParamsHolder getParamsHolder(V args);
	boolean refreshParams(ParamsHolder paramsHolder, Object criteria);
}

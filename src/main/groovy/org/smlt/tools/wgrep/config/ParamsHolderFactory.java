package org.smlt.tools.wgrep.config;

public interface ParamsHolderFactory<V> {
	ParamsHolder getParamsHolder(V args);
	boolean refreshParams(ParamsHolder paramsHolder, Object criteria);
}

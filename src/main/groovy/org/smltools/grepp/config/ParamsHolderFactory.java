package org.smltools.grepp.config;

import java.io.File;

public interface ParamsHolderFactory<V> {
	ParamsHolder getParamsHolder(V args);
	boolean refreshParams(ParamsHolder paramsHolder, Object criteria);
	void setWorkingDir(File cwd);
	File getWorkingDir();
}

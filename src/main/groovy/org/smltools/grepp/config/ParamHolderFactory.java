package org.smltools.grepp.config;

import java.io.File;

public interface ParamHolderFactory<T> {
	ParamHolder getParamHolder(T args);
	boolean refreshParams(ParamHolder paramsHolder, Object criteria);
	void setWorkingDir(File cwd);
	File getWorkingDir();
}

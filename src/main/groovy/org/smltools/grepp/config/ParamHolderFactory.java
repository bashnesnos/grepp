package org.smltools.grepp.config;

public interface ParamHolderFactory<T> {
	ParamHolder getParamHolder(T args);
	boolean refreshParams(ParamHolder paramsHolder, Object criteria);
	void setWorkingDir(File cwd);
	File getWorkingDir();
}

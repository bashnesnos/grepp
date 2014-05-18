package org.smltools.grepp.filters;

import groovy.util.ConfigObject;
import java.util.Map;

public interface Configurable {
    void setConfig(Map<?, ?> config);
	boolean fillParamsByConfigId(String configId);	
	ConfigObject getAsConfig(String configId);
	boolean configIdExists(String configId);
}
package org.smltools.grepp.filters;

import groovy.util.ConfigObject;

public interface Configurable {
	ConfigObject getAsConfig(String configId);
}
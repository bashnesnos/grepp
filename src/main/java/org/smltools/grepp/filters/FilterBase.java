package org.smltools.grepp.filters;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smltools.grepp.exceptions.ConfigNotExistsRuntimeException;
import org.smltools.grepp.exceptions.PropertiesNotFoundRuntimeException;

/**
 * Super class for all filters. Provides filtering process template with hooking
 * methods.<br>
 * 
 * @author Alexander Semelit
 * @param <T>
 */

public abstract class FilterBase<T> implements Filter<T>, Configurable {
	protected Map<?, ?> config = null;
	protected String configId;

    public FilterBase() {

    }

    @Override
    public void setConfig(Map<?, ?> config) {
    	this.config = config;
    }

}
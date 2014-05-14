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
	protected final Map<?, ?> config;
	protected String configId;
 	protected final Logger LOGGER;

        public FilterBase(Class<?> subclazz) {
            if (subclazz == null) {
                    throw new IllegalArgumentException("All the constructor params shouldn't be null! " + (subclazz != null));
            }

            this.config = null;
            LOGGER = LoggerFactory.getLogger(subclazz);
        }
        
	public FilterBase(Class<?> subclazz, Map<?, ?> config) {
        if (subclazz == null || config == null) {
			throw new IllegalArgumentException("All the constructor params shouldn't be null! " + (subclazz != null) + ";" + (config != null));
		}
		this.config = config;
		LOGGER = LoggerFactory.getLogger(subclazz);
	}

	protected abstract boolean fillParamsByConfigIdInternal(String configId);
}
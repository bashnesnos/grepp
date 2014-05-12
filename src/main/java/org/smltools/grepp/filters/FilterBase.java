package org.smltools.grepp.filters;

import java.text.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smltools.grepp.exceptions.FilteringIsInterruptedException;
import org.smltools.grepp.filters.FilterBase;
import org.smltools.grepp.filters.enums.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Super class for all filters. Provides filtering process template with hooking
 * methods.<br>
 * 
 * @author Alexander Semelit
 */

public abstract class FilterBase<T> implements Filter<T>, Configurable {
	protected final Map<?, ?> config;
	protected String configId;
 	protected final Logger LOGGER;

	public FilterBase(Class<?> subclazz, Map<?, ?> config) {
		this.config = config;
		LOGGER = LoggerFactory.getLogger(subclazz);
	}

	public static <V extends FilterBase> V createFilterFromConfigByConfigId(Class<V> filterClass, Map<?, ?> config, String configId) {
		if (filterClass == null || config == null || configId == null) {
			throw new IllegalArgumentException("All the method params shouldn't be null! " + (filterClass != null) + ";" + (config != null) + ";" + (configId != null));
		}

		try {
			Constructor<V> constructByConfigId = filterClass.getDeclaredConstructor(Map.class, String.class);
			V newFilter = constructByConfigId.newInstance(config, configId);
			newInstance.configId = configId;
			return newFilter;
		} catch(InvocationTargetException ite) {
			throw new RuntimeException(ite);
		} catch (NoSuchMethodException nsme) {
			throw new UnsupportedOperationException("A particular Filter implementation should have a constructor by Map and configId", nsme);
		} catch (InstantiationException ie) {
			throw new RuntimeException(ie);
		} catch (IllegalAccessException iace) {
			throw new RuntimeException(iace);
		} 
	}

	@Override
	public boolean refreshByConfigId(String configId) {
		if (configId == null) {
			throw new IllegalArgumentException("configId shoudn't be null!");
		}

		if (config != null) {
			if (this.configId.equals(configId)) {
				return false; //same configId, no need refreshing
			}

	    	try {
	    		if (fillParamsByConfigIdInternal(configId)) {
	    	   		this.configId = configId;
		    		return true;
	    		}
	    		else {
	    			return false;
	    		}
	    	}
	    	catch(ConfigNotExistsRuntimeException cnere) {
	    		LOGGER.debug("Not refreshing due to: ", cnere);
	    	}
	    	catch(PropertiesNotFoundRuntimeException pnfre) {
	    		LOGGER.debug("Not refreshing due to: ", pnfre);
	    	}
	    	return false;
		}
		else {
			LOGGER.debug("Current instance is not refreshable due to null config");
		}
	}

	protected abstract boolean fillParamsByConfigIdInternal(String configId);
}
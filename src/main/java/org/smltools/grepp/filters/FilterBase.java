package org.smltools.grepp.filters;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smltools.grepp.exceptions.ConfigNotExistsRuntimeException;
import org.smltools.grepp.exceptions.PropertiesNotFoundRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Super class for all filters. Provides filtering process template with hooking
 * methods.<br>
 * 
 * @author Alexander Semelit
 * @param <T>
 */

public abstract class FilterBase<T> implements Filter<T>, Configurable {
	private static final Logger LOGGER = LoggerFactory.getLogger(FilterBase.class);

	protected Map<?, ?> config = null;
	protected String configId;

    public FilterBase() {

    }

    @Override
    public void setConfig(Map<?, ?> config) {
    	this.config = config;
    }

    @SuppressWarnings("unchecked")
    @Override
	public boolean configIdExists(String configId) {
		if (config == null) {
			throw new IllegalStateException("Config should be supplied explicitly!");
		}

		return configIdExists(this.getClass(), config, configId);
	}

	@SuppressWarnings("unchecked")
	public static boolean configIdExists(Class<? extends Filter> filterClass, Map<?, ?> config, String configId) {
		FilterParams filterParams = filterClass.getAnnotation(FilterParams.class);
		if (filterParams != null) {
			String configIdPath = filterParams.configIdPath();
			LOGGER.debug("Full config Id path: {}", configIdPath);
			boolean exists = false;
			for (String configIdPathVariant : configIdPath.split("\\|")) {
				exists |= findConfigIdInPath(config, configId, configIdPathVariant);
				if (exists) {
					return exists;
				}
			}
			return exists;
		}
		else {
			throw new IllegalArgumentException("No FilterParams annotation on " + filterClass);
		}

	}

	@SuppressWarnings("unchecked")
	public static Map<?, ?> findConfigIds(Map<?, ?> config, String configIdPath) {
		LOGGER.debug("Fetching prop map for: {}", configIdPath);
		Map<?, ?> configs = null;
		for (String configIdPathPart : configIdPath.split("\\.")) {
			LOGGER.trace("Path part {}", configIdPathPart);
			if (configs == null) {
				LOGGER.trace("Looking in main config");
				configs = (Map<?,?>) config.get(configIdPathPart);	
			}
			else {
				LOGGER.trace("Looking in sub config");
				configs = (Map<?,?>) configs.get(configIdPathPart);	
			}
		}
		return configs;
	}

	public static boolean findConfigIdInPath(Map<?, ?> config, String configId, String configIdPath) {
		Map<?, ?> configs = findConfigIds(config, configIdPath);
		LOGGER.debug("Target configId: {}; config map keys: {}", configId, configs != null ? configs.keySet() : null);
		if (configs != null) {
			return configs.containsKey(configId);
		}
		else {
			return false;
		}
	}

}
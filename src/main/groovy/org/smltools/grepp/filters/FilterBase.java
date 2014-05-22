package org.smltools.grepp.filters;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smltools.grepp.exceptions.ConfigNotExistsRuntimeException;
import org.smltools.grepp.exceptions.PropertiesNotFoundRuntimeException;
import org.slf4j.Logger;
import org.smltools.grepp.util.GreppUtil;
import org.slf4j.LoggerFactory;
import java.util.Iterator;

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

	@Override
	public Set<Object> configIdsSet() {
		if (config == null) {
			throw new IllegalStateException("Config should be supplied explicitly!");
		}

		return configIdsSet(this.getClass(), config);

	}

	@SuppressWarnings("unchecked")
	public static Set<Object> configIdsSet(Class<? extends Filter> filterClass, Map<?, ?> config) {
		GreppUtil.throwIllegalAEifNull("All parameters are mandatory", filterClass, config);
		FilterParams filterParams = filterClass.getAnnotation(FilterParams.class);
		if (filterParams != null) {
			String configIdPath = filterParams.configIdPath();
			LOGGER.debug("Full config Id path: {}", configIdPath);
			Set<Object> result = new HashSet<Object>();
			String[] mandatoryProps = filterParams.mandatoryProps();
			int pathVariantIdx = 0;
			for (String configIdPathVariant : configIdPath.split("\\|")) {
				Map<?, ?> configMap = findConfigIds(config, configIdPathVariant);
				if (configMap != null) {
					configMap = new HashMap(configMap); //so the initial map is not changed
					if (pathVariantIdx < mandatoryProps.length) {
						String mandatoryPropList = mandatoryProps[pathVariantIdx++];
					 	if (mandatoryPropList != null && !"".equals(mandatoryPropList)) {
							for (String mandatoryProp : mandatoryPropList.split(",")) {
								Iterator<?> configIdIter = configMap.keySet().iterator();
								while (configIdIter.hasNext()) {
									Object configId = configIdIter.next();
									Object value = configMap.get(configId);
									if (value instanceof Map) {
										if (!((Map<?, ?>) value).containsKey(mandatoryProp)) {
											LOGGER.debug("{}. Removing '{}' as it doesn't have mandatory '{}'", filterClass, configId, mandatoryProp);
											configIdIter.remove();
										}
									}
									else {
										throw new IllegalArgumentException("Mandatory keys supplied, but the config itself is not a key container! ConfigId: " + configId.toString());
									}
								}
							}
						}
					}
					result.addAll(configMap.keySet());
				}
				else {
					LOGGER.debug("No props found for path: {}", configIdPathVariant);
				}
			}
			LOGGER.debug("Key set for {}: {}", filterClass, result);
			return result;
		}
		else {
			throw new IllegalArgumentException("No FilterParams annotation on " + filterClass);
		}

	}

	@SuppressWarnings("unchecked")
	public static boolean configIdExists(Class<? extends Filter> filterClass, Map<?, ?> config, String configId) {
		GreppUtil.throwIllegalAEifNull("All parameters are mandatory", filterClass, config, configId);
		LOGGER.debug("Target configId: {}", configId);
		return configIdsSet(filterClass, config).contains(configId);
	}

	@SuppressWarnings("unchecked")
	public static Map<?, ?> findConfigIds(Map<?, ?> config, String configIdPath) {
		GreppUtil.throwIllegalAEifNull("All parameters are mandatory", config, configIdPath);

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

}
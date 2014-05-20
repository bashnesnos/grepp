package org.smltools.grepp.filters.entry;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import groovy.util.ConfigObject;
import org.smltools.grepp.exceptions.PropertiesNotFoundRuntimeException;
import org.smltools.grepp.filters.FilterBase;
import org.smltools.grepp.filters.FilterParams;
import org.smltools.grepp.filters.enums.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Class provides in-flight pattern building depending on thread start, thread end patterns. If non specified works in the same way as BasicFilter. <br>
 * Forces multiline regex matching. 
 * 
 * @author Alexander Semelit
 *
 */

@FilterParams(configIdPath = SimpleFilter.FILTERS_CONFIG_KEY, order = 5)
public class SimpleFilter extends FilterBase<String> {
	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleFilter.class);	
	public final static String FILTERS_CONFIG_KEY = "filterAliases";
	public final static String FILTERS_CONFIG_VALUE_KEY = "value";
	public final static String FILTERS_CONFIG_REGEX_KEY = "noRegex";

	private Pattern filterPattern;
	private StringBuilder patternBuilder = new StringBuilder("(?ms)"); //for extended patterns
	private boolean noRegex = false;

	public void setFilterPattern(String filterPattern) {
		setFilterPattern(filterPattern, false);
	}

	public void setFilterPattern(String filterPattern, boolean noRegex) {
		this.noRegex = noRegex;
		patternBuilder = new StringBuilder("(?ms)"); 
		extractPatternParts(noRegex ? Pattern.quote(filterPattern) : filterPattern);
		this.filterPattern = Pattern.compile(patternBuilder.toString());
	}

	public String getFilterPattern() {
		return filterPattern.pattern();
	}

	@SuppressWarnings("unchecked")
	@Override
    public boolean fillParamsByConfigId(String configId) {
    	if (!configIdExists(configId)) {
    		return false;
    	}

    	boolean result = false;
		Map<?, ?> configs = (Map<?,?>) config.get(FILTERS_CONFIG_KEY);
    	Object customFilter = (Object) configs.get(configId);
    	if (customFilter != null) {
    		if (customFilter instanceof String) {
    			setFilterPattern((String) customFilter);
	    		this.configId = configId;
    			result |= true;
    		}
    		else {
    			Map<?, ?> customFilterProps = (Map<?, ?>) customFilter;
    			if (customFilterProps.containsKey(FILTERS_CONFIG_VALUE_KEY)) {
    				Boolean noRegex = customFilterProps.containsKey(FILTERS_CONFIG_REGEX_KEY) ? (Boolean) customFilterProps.get(FILTERS_CONFIG_REGEX_KEY) : false;
    				setFilterPattern((String) customFilterProps.get(FILTERS_CONFIG_VALUE_KEY), noRegex);
		    		this.configId = configId;
		    		result |= true;
    			}
    			else {
    				LOGGER.debug(FILTERS_CONFIG_VALUE_KEY + " is not filled for config: " + configId);
    			}
    		}
    	}
    	else {
    		LOGGER.debug(FILTERS_CONFIG_KEY + " is not filled for config: " + configId);
    	}
		return result;
    }

    @Override
    public ConfigObject getAsConfig(String configId) {
        if (configId == null) {
            if (this.configId == null) {
                throw new IllegalArgumentException("Can't derive configId (none was supplied)");
            }
            else {
                configId = this.configId;
            }
        }

        ConfigObject root = new ConfigObject();
    	ConfigObject filterAliases = (ConfigObject) root.getProperty(FILTERS_CONFIG_KEY);
    	if (noRegex) {
    		ConfigObject filterConfig = (ConfigObject) filterAliases.getProperty(configId);
    		filterConfig.put(FILTERS_CONFIG_VALUE_KEY, filterPattern.pattern());
    		filterConfig.put(FILTERS_CONFIG_REGEX_KEY, noRegex);
    	}
    	else {
    		filterAliases.put(configId, filterPattern.pattern());
    	}
    	return root;

	}
	

	/**
	 * Checks if data matches current pattern 
	 * @throws IllegalArgumentException if blockData is not String
	 */

	@Override
	public String filter(String blockData) {
		if (filterPattern == null) {
			throw new IllegalStateException("Filtering pattern can't be null. It should be either supllied via configId or set explicitly");
		}

		Matcher blockMtchr = filterPattern.matcher(blockData);
		if (blockMtchr.find()) {
			return blockData;
		}
		else {
			return null;
		}

	}

	/**
	 * Appends to current pattern new part which is could be a thread coupling pattern or just a different thing to look up in the data.
	 * 
	 * @param val pattern to be added
	 * @param qualifier identifies how to conjunct it with previous patterns
	 */

	protected void addExtendedFilterPattern(String val, String qualifier)
	{
		if (LOGGER.isTraceEnabled()) LOGGER.trace("adding complex pattern: val={} qual={}", val, qualifier);

		if (qualifier != null) patternBuilder = patternBuilder.append(Qualifier.valueOf(qualifier).getPattern());
		patternBuilder = patternBuilder.append("(?:").append(val).append(")");

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Built so far: {}", patternBuilder.toString());
		}
	}

	/**
	 * Parses supplied filterPattern. If it contains any qualifiers like %and&|%or% parses them into valid regex representation.
	 * 
	 * @param val pattern String
	 */
	protected void extractPatternParts(String val)
	{
		String qRegex = "";
		for (Qualifier it: Qualifier.values()) {
			qRegex += qRegex.length() > 0 ? "|%" + it + "%" : "%" + it + "%";
		}

		if (LOGGER.isTraceEnabled()) LOGGER.trace("Trying to match supplied pattern /{}/ if it contains /{}/", val, qRegex);
		Matcher qualifierMatcher = Pattern.compile(qRegex).matcher(val); //matching any qualifiers with % signs
		if (qualifierMatcher.find()) {
			if (LOGGER.isTraceEnabled()) LOGGER.trace("Processing extended pattern");
			String[] tokens = val.split("%");
			String nextQualifier = null;
			if (tokens != null)	{
				qRegex = qRegex.replaceAll("%", ""); //matching only qualifier names
				for (String grp : tokens) {
					if (LOGGER.isTraceEnabled()) LOGGER.trace("Next group in match: {}", grp);
					qualifierMatcher = Pattern.compile(qRegex).matcher(grp);
					if (qualifierMatcher.matches())	{
						nextQualifier = qualifierMatcher.group();
						continue;
					}

					addExtendedFilterPattern(grp, nextQualifier);
					nextQualifier = null;

				}
			}
			else throw new IllegalArgumentException("Check your extended pattern:/" + val + "/");
		}
		else {
			if (LOGGER.isTraceEnabled()) LOGGER.trace("No extended pattern supplied; keeping things simple");
			patternBuilder.append(val);
		}
	}

}
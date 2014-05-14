package org.smltools.grepp.filters.entry;

import java.util.*;
import java.util.regex.*;

import org.smltools.grepp.exceptions.PropertiesNotFoundRuntimeException;
import org.smltools.grepp.filters.FilterBase;
import org.smltools.grepp.filters.FilterParams;
import org.smltools.grepp.filters.enums.*;



/**
 * 
 * Class provides in-flight pattern building depending on thread start, thread end patterns. If non specified works in the same way as BasicFilter. <br>
 * Forces multiline regex matching. 
 * 
 * @author Alexander Semelit
 *
 */

@FilterParams(order = 5)
public class SimpleFilter extends FilterBase<String> {
	public final static String FILTERS_CONFIG_KEY = "filterAliases";

	protected String filterPattern;	

	//Complex pattern processing and stuff
	protected Pattern currentPattern;
	protected StringBuilder patternBuilder = new StringBuilder("(?ms)"); //for multiline support
	protected List<String> patternParts = new ArrayList<String>();
	protected Map<String, Qualifier> patternPartQualifierMap = new HashMap<String, Qualifier>();
	/**
	 * Creates non-refreshable and non-publicly modifiable, standalone and maybe stateless SimpleFilter
	 * @param filterPattern
	 *            pattern to filter data
	 */

	public SimpleFilter(String filterPattern)
	{
		super(SimpleFilter.class);
		setFilterPattern(filterPattern);
	}

	protected SimpleFilter(Class<? extends SimpleFilter> subclazz, String filterPattern)
	{
		super(subclazz);
		setFilterPattern(filterPattern);
	}


	public void setFilterPattern(String filterPattern) {
		this.filterPattern = filterPattern;
		patternBuilder = new StringBuilder("(?ms)"); 
		patternParts = new ArrayList<String>();
		patternPartQualifierMap = new HashMap<String, Qualifier>();
		extractPatternParts(filterPattern);
		currentPattern = Pattern.compile(patternBuilder.toString());
	}

	public String getFilterPattern() {
		return filterPattern;
	}

	public SimpleFilter(Class<?> subclazz, Map<?, ?> config) {
		super(subclazz, config);
	}

	/**
	* Creates SimpleFilter from config
	*
	*/
	public SimpleFilter(Map<?, ?> config, String configId) {
            this(SimpleFilter.class, config, configId);
	}

	protected SimpleFilter(Class<? extends SimpleFilter> subclazz, Map<?, ?> config, String configId) {
		super(subclazz, config);
		if (!fillParamsByConfigIdInternal(configId)) {
			throw new PropertiesNotFoundRuntimeException(FILTERS_CONFIG_KEY + "." + configId + "doesn't exist!");
		}
	}

	@SuppressWarnings("unchecked")
	@Override
    protected boolean fillParamsByConfigIdInternal(String configId) {
    	boolean result = false;
    	if (SimpleFilter.configIdExists(config, configId)) {
			Map<?, ?> configs = (Map<?,?>) config.get(FILTERS_CONFIG_KEY);
	    	String customFilter = (String) configs.get(configId);
	    	if (customFilter != null) {
	    		setFilterPattern(customFilter);
	    		result |= true;
	    	}
	    	else {
	    		LOGGER.debug(FILTERS_CONFIG_KEY + " is not filled for config: " + configId);
	    	}
    	}
		return result;
    }

    @SuppressWarnings("unchecked")
	public static boolean configIdExists(Map<?, ?> config, String configId) {
		if (config == null) {
			throw new IllegalArgumentException("Config can't be null!");
		}

		Map<?, ?> filterConfigs = (Map<?,?>) config.get(FILTERS_CONFIG_KEY);
		
		if (filterConfigs != null) {
			return filterConfigs.containsKey(configId);
		}
		else {
			return false;
		}
	}	

	/**
	 * Checks if data matches current pattern 
	 * @throws IllegalArgumentException if blockData is not String
	 */

	@Override
	public String filter(String blockData) {
		if (currentPattern == null) {
			throw new IllegalStateException("Filtering pattern can't be null. It should be either supllied via configId or set explicitly");
		}

		Matcher blockMtchr = currentPattern.matcher(blockData);
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
		patternBuilder = patternBuilder.append(val);

		patternParts.add(val);
		patternPartQualifierMap.put(val, qualifier != null ? Qualifier.valueOf(qualifier) : null);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(patternParts.toString());
			LOGGER.trace(patternPartQualifierMap.toString());
		}
	}

	/**
	 * Removes supplied pattern with it's qualifier if any.
	 * 
	 * @param val pattern for removal
	 */
	protected void removeExtendedFilterPattern(String val)
	{
		Qualifier qlfr = patternPartQualifierMap.get(val);
		String ptrn = (qlfr != null ? qlfr.getPattern() : "") + val;
		int ptrnIndex = patternBuilder.indexOf(ptrn);
		if (LOGGER.isTraceEnabled()) LOGGER.trace("to delete:/{}/ index:{}", ptrn, ptrnIndex);
		if (ptrnIndex != -1)
		{
			patternBuilder = patternBuilder.delete(ptrnIndex, ptrnIndex + ptrn.length());
			patternParts.remove(val);
			patternPartQualifierMap.remove(val);
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
		if (qualifierMatcher.find())
		{
			if (LOGGER.isTraceEnabled()) LOGGER.trace("Processing complex pattern");
			String[] tokens = val.split("%");
			String nextQualifier = null;
			if (tokens != null)
			{
				qRegex = qRegex.replaceAll("%", ""); //matching only qualifier names
				for (String grp : tokens)
				{
					if (LOGGER.isTraceEnabled()) LOGGER.trace("Next group in match: {}", grp);
					qualifierMatcher = Pattern.compile(qRegex).matcher(grp);
					if (qualifierMatcher.matches())
					{
						nextQualifier = qualifierMatcher.group();
						continue;
					}

					addExtendedFilterPattern(grp, nextQualifier);
					nextQualifier = null;

				}
			}
			else throw new IllegalArgumentException("Check your complex pattern:/" + val + "/");
		}
		else
		{
			if (LOGGER.isTraceEnabled()) LOGGER.trace("No extended pattern supplied, might be a preserve thread");
			addExtendedFilterPattern(val, null);
		}
	}

}
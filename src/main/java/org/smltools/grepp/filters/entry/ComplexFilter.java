package org.smltools.grepp.filters.entry;

import java.util.*;
import java.util.regex.*;

import org.smltools.grepp.filters.FilterBase;
import org.smltools.grepp.filters.enums.*;
import org.smltools.grepp.util.GreppUtil;



/**
 * 
 * Class provides in-flight pattern building depending on thread start, thread end patterns. If non specified works in the same way as BasicFilter. <br>
 * Forces multiline regex matching. 
 * 
 * @author Alexander Semelit
 *
 */

final class ComplexFilter extends StatefulFilterBase<String> implements OptionallyStateful<String> {
	public final static String THREADS_CONFIG_KEY = "processThreads";
	public final static String THREAD_EXTRACTORS_KEY = "extractors";
	public final static String THREAD_SKIPENDS_KEY = "skipends";
	public final static String THREAD_ENDS_KEY = "ends";

	public final static String FILTERS_CONFIG_KEY = "filterAliases";

	private String filterPattern;	

	//Simple stateless mode
	private Pattern fixedPattern;
	
	//Complex pattern processing and stuff
	private Pattern currentPattern = null;
	private StringBuilder patternBuilder = new StringBuilder("(?ms)"); //for multiline support
	private List<String> patternParts = new ArrayList<String>();
	private Map<String, Qualifier> patternPartQualifierMap = new HashMap<String, Qualifier>();
	private List<String> threadStartExtractorList;
	private List<String> threadStartPatternList;
	private List<String> threadSkipEndPatternList;
	private List<String> threadEndPatternList;

	/**
	 * Creates non-refreshable and non-publicly modifiable, standalone and maybe stateless ComplexFilter
	 * @param filterPattern
	 *            pattern to filter data
	 */

	public ComplexFilter(String filterPattern, List<String> threadStartExtractorList, 
		List<String> threadSkipEndPatternList, List<String> threadEndPatternList)
	{
		super(ComplexFilter.class, null);
		if (threadStartExtractorList != null) {
			this.threadStartExtractorList = threadStartExtractorList;
			this.threadStartPatternList = new ArrayList<String>();
			this.threadSkipEndPatternList = threadSkipEndPatternList != null ? threadSkipEndPatternList : new ArrayList<String>();
			if (threadEndPatternList != null) {
				this.threadEndPatternList = threadEndPatternList;
			}
			else {
				throw new IllegalArgumentException("Thread end patterns should be supplied, if thread starts were");
			}

			if (log.isTraceEnabled()) {
				log.trace("{}\n{}\n{}\n{}", threadStartExtractorList, threadStartPatternList, threadSkipEndPatternList, threadEndPatternList);
			}
		}
		setFilterPattern(filterPattern);
	}

	public void setFilterPattern(String filterPattern) {
		this.filterPattern = filterPattern;
		extractPatternParts(filterPattern);

		if (threadStartExtractorList == null) {
			fixedPattern = Pattern.compile(patternBuilder.toString());
		}
		else {
			fixedPattern = null;
		}
	}

	/**
	* Creates LogEntryFilter from config
	*
	*/
	public ComplexFilter(Map<?, ?> config, String configId) {
		super(ComplexFilter.class, config);
		fillParamsByConfigIdInternal(configId);
	}

	@SuppressWarnings("unchecked")
	@Override
    private boolean fillParamsByConfigIdInternal(String configId) {
    	if (!ComplexFilter.configIdExists(config, configId)) {
    		throw new ConfigNotExistsRuntimeException(configId);
    	}

    	boolean result = false;

    	Map<?, ?> configs = (Map<?,?>) config.get(THREADS_CONFIG_KEY);
    	Map<?, ?> customCfg = (Map<?,?>) configs.get(configId);

		if (customCfg.containsKey(THREAD_EXTRACTORS_KEY)) {
			threadStartExtractorList = (List<String>) customCfg.get(THREAD_EXTRACTORS_KEY);
			threadStartPatternList = new ArrayList<String>();
			threadSkipEndPatternList =  (List<String>) GreppUtil.getNotNull(customCfg, THREAD_SKIPENDS_KEY, new ArrayList<String>());
			if (customCfg.containsKey(THREAD_ENDS_KEY)) {
				threadEndPatternList =  (List<String>) customCfg.get(THREAD_ENDS_KEY);
			}
			else {
				throw new PropertiesNotFoundRuntimeException(THREADS_CONFIG_KEY + "." + THREAD_ENDS_KEY + " is not filled for config: " + configId);
			}

			if (log.isTraceEnabled()) {
				log.trace("{}\n{}\n{}\n{}", threadStartExtractorList, threadStartPatternList, threadSkipEndPatternList, threadEndPatternList);
			}

			result |= true;
		}
		else {
			LOGGER.debug(THREADS_CONFIG_KEY + "." + THREAD_EXTRACTORS_KEY + " is not filled for config: " + configId);
		}

		configs = (Map<?,?>) config.get(FILTERS_CONFIG_KEY);
    	String customFilter = (String) configs.get(configId);
    	if (customFilter != null) {
    		setFilterPattern(customFilter);
    		result |= true;
    	}
    	else {
    		LOGGER.debug(FILTERS_CONFIG_KEY + " is not filled for config: " + configId);
    	}
    	
		return result;
    }

    @SuppressWarnings("unchecked")
	public static boolean configIdExists(Map<?, ?> config, String configId) {
		Map<?, ?> threadConfigs = (Map<?,?>) config.get(THREADS_CONFIG_KEY);
		Map<?, ?> filterConfigs = (Map<?,?>) config.get(FILTERS_CONFIG_KEY);
		
		if (threadConfigs != null) {
			return threadConfigs.containsKey(configId);
		}
		else if (filterConfigs != null) {
			return filterConfigs.containsKey(configId);
		}
		else {
			return false;
		}
	}	

	/**
	 * Checks is data matches current pattern 
	 * @throws IllegalArgumentException if blockData is not String
	 */

	@Override
	public String filter(String blockData) {
		if (isStateful()) {
			String newPtrn = patternBuilder.toString();
			if(log.isTraceEnabled()) {
				log.trace("Current pattern: {}", newPtrn);
			}
		
			if (currentPattern == null || currentPattern.toString() != newPtrn) {
				currentPattern = Pattern.compile(newPtrn);
			}
		}
		else {
			currentPattern = fixedPattern;
		}
		
		Matcher blockMtchr = currentPattern.matcher(blockData);
		if (blockMtchr.find()) {
			if (isStateful()) {
				extractThreadPatterns(blockData);
			}
			return blockData;
		}
		else {
			return null;
		}

	}

	/**
	 * Checks is thread preserving is enabled.
	 * 
	 * @return true if it is
	 */
	private boolean isStateful()
	{
		return fixedPattern == null;
	}

	/**
	 * Flushes all state
	 * 
	 */
	@Override
	public void flush() {
		threadStartPatternList = new ArrayList<String>();
		currentPattern = null;
		patternBuilder = = new StringBuilder("(?ms)"); 
		patternParts = new ArrayList<String>();
		patternPartQualifierMap = new HashMap<String, Qualifier>();
		setFilterPattern(filterPattern); //keeping the initial one
    }

    @Override
	public boolean isStateful() {
		return isStateOptional;
	}

	/**
	 * Extracts thread patterns, and adds/keeps them if thread is not yet ended in the logs, or removes them is it has ended.
	 * 
	 * @param data String already matched by filter pattern.
	 */
	private void extractThreadPatterns(String data)
	{
		if (searchThreadEnds(data))
		{
			for (Map.Entry<String, String> extractedStart: extractThreadStarts(data).entrySet()) {
				removeThreadStart(extractedStart.getKey(), extractedStart.getValue());
			}
		}
		else
		{
			if (log.isTraceEnabled()) log.trace("Thread continues. Keeping starts");
			for (Map.Entry<String, String> extractedStart: extractThreadStarts(data).entrySet()) {
				addThreadStart(extractedStart.getKey(), extractedStart.getValue());
			}

		}
	}

	/**
	 * Iterates through <extractor> patterns matched by PRESERVE_THREAD tag. <br>
	 * For each tries to match supplied data, and if it matches passes matched string and qualifier to supplied method. 
	 * 
	 * @param data String already matched by filter pattern.
	 * @return collection of pairs <pattern,qualifier>
	 */
	private Map<String, String> extractThreadStarts(String data)
	{
		HashMap<String, String> extractedStarts = new HashMap<String, String>();
		for (String extractorPattern : threadStartExtractorList) {
			if (log.isTraceEnabled())
				log.trace(extractorPattern);
			Matcher extractorMatcher = Pattern.compile(extractorPattern).matcher(data);
			if (extractorMatcher.find())
			{
				String start = extractorMatcher.group();
				if (log.isTraceEnabled())
					log.trace("extracted; {}", start);
				extractedStarts.put(start, Qualifier.or.toString()); //adding extractor as or, since any could be a thread start
			}
		}
		return extractedStarts;
	}

	/**
	 * Applies <pattern> elements having threadEndPatternList collection as a parameter. <br>
	 * If any is matched current data is considered as end of current log thread if any.
	 * 
	 * @param data String already matched by filter pattern.
	 * @return true if thread end was found, false otherwise
	 */
	private boolean searchThreadEnds(String data)
	{
		if (!shouldBeSkipped(data))
		{
			boolean decision = false;
			Iterator<String> endIter = threadEndPatternList.iterator();
			while (!decision && endIter.hasNext()) {
				String thrend = endIter.next();
				log.trace("thrend ptrn: {}", thrend);
				decision = Pattern.compile(thrend).matcher(data).find();
			}
			return decision;
		}
		return false;
	}

	/**
	 * Applies <pattern> elements having threadSkipEndPatternList collection as a parameter. <br>
	 * If any is matched current data won't be considered as end, it simply won't be checked for end patterns.
	 * 
	 * @param data String already matched by filter pattern.
	 * @return true if data should skip thead end check, false otherwise
	 */
	private boolean shouldBeSkipped(String data)
	{
		boolean decision = false;
		Iterator<String> skipEndIter = threadSkipEndPatternList.iterator();
		while (!decision && skipEndIter.hasNext()) {
			String thrend = skipEndIter.next();
			log.trace("thrend ptrn: {}", thrend);
			decision = Pattern.compile(thrend).matcher(data).find();
		}
		return decision;
	}

	/**
	 * Adds supplied thread start to threadStartPatternList and to a filterPattern
	 * 
	 * @param start String representing thread start
	 * @param qlfr qualifier of this thread start
	 */
	private void addThreadStart(String start, String qlfr)
	{
		log.trace("adding thread start: {}", start);
		if (!threadStartPatternList.contains(start))
		{
			threadStartPatternList.add(start);
			addExtendedFilterPattern(start, qlfr);
		}
		else log.trace("Start exists");
	}

	/**
	 * Removes supplied thread start. Qualifier will be identified automatically.
	 *
	 * @param start String representing thread start
	 * @param qlfr not used. Needed for reflection call
	 */
	private void removeThreadStart(String start, String qlfr)
	{
		log.trace("removing thread start: {}", start);
		threadStartPatternList.remove(start);
		removeExtendedFilterPattern(start);
	}

	/**
	 * Appends to current pattern new part which is could be a thread coupling pattern or just a different thing to look up in the data.
	 * 
	 * @param val pattern to be added
	 * @param qualifier identifies how to conjunct it with previous patterns
	 */

	private void addExtendedFilterPattern(String val, String qualifier)
	{
		if (log.isTraceEnabled()) log.trace("adding complex pattern: val={} qual={}", val, qualifier);

		if (qualifier != null) patternBuilder = patternBuilder.append(Qualifier.valueOf(qualifier).getPattern());
		patternBuilder = patternBuilder.append(val);

		patternParts.add(val);
		patternPartQualifierMap.put(val, qualifier != null ? Qualifier.valueOf(qualifier) : null);

		if (log.isTraceEnabled()) {
			log.trace(patternParts.toString());
			log.trace(patternPartQualifierMap.toString());
		}
	}

	/**
	 * Removes supplied pattern with it's qualifier if any.
	 * 
	 * @param val pattern for removal
	 */
	private void removeExtendedFilterPattern(String val)
	{
		Qualifier qlfr = patternPartQualifierMap.get(val);
		String ptrn = (qlfr != null ? qlfr.getPattern() : "") + val;
		int ptrnIndex = patternBuilder.indexOf(ptrn);
		if (log.isTraceEnabled()) log.trace("to delete:/{}/ index:{}", ptrn, ptrnIndex);
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
	private void extractPatternParts(String val)
	{
		String qRegex = "";
		for (Qualifier it: Qualifier.values()) {
			qRegex += qRegex.length() > 0 ? "|%" + it + "%" : "%" + it + "%";
		}

		if (log.isTraceEnabled()) log.trace("Trying to match supplied pattern /{}/ if it contains /{}/", val, qRegex);
		Matcher qualifierMatcher = Pattern.compile(qRegex).matcher(val); //matching any qualifiers with % signs
		if (qualifierMatcher.find())
		{
			if (log.isTraceEnabled()) log.trace("Processing complex pattern");
			String[] tokens = val.split("%");
			String nextQualifier = null;
			if (tokens != null)
			{
				qRegex = qRegex.replaceAll("%", ""); //matching only qualifier names
				for (String grp : tokens)
				{
					if (log.isTraceEnabled()) log.trace("Next group in match: {}", grp);
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
			if (log.isTraceEnabled()) log.trace("No extended pattern supplied, might be a preserve thread");
			addExtendedFilterPattern(val, null);
		}
	}
}
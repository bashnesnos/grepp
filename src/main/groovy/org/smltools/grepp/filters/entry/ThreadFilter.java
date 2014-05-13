package org.smltools.grepp.filters.entry;

import java.util.*;
import java.util.regex.*;
import org.smltools.grepp.exceptions.ConfigNotExistsRuntimeException;
import org.smltools.grepp.exceptions.PropertiesNotFoundRuntimeException;
import org.smltools.grepp.filters.Refreshable;
import org.smltools.grepp.filters.Stateful;
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

public final class ThreadFilter extends SimpleFilter implements Stateful<String>, Refreshable {
	public final static String THREADS_CONFIG_KEY = "processThreads";
	public final static String THREAD_EXTRACTORS_KEY = "extractors";
	public final static String THREAD_SKIPENDS_KEY = "skipends";
	public final static String THREAD_ENDS_KEY = "ends";

	//Complex pattern processing and stuff
	private List<String> threadStartExtractorList;
	private List<String> threadStartPatternList;
	private List<String> threadSkipEndPatternList;
	private List<String> threadEndPatternList;

	private Map<?,?> state = new HashMap<Object, Object>();
	/**
	 * Creates non-refreshable and non-publicly modifiable, standalone and maybe stateless ThreadFilter
	 * @param filterPattern
	 *            pattern to filter data
	 */

	public ThreadFilter(String filterPattern, List<String> threadStartExtractorList, 
		List<String> threadSkipEndPatternList, List<String> threadEndPatternList)
	{
		super(ThreadFilter.class, filterPattern);
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

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("{}\n{}\n{}\n{}", threadStartExtractorList, threadStartPatternList, threadSkipEndPatternList, threadEndPatternList);
			}
		}
	}

	/**
	* Creates LogEntryFilter from config
	*
	*/
	public ThreadFilter(Map<?, ?> config, String configId) {
		super(ThreadFilter.class, config, configId);
	}

	@SuppressWarnings("unchecked")
	@Override
    protected boolean fillParamsByConfigIdInternal(String configId) {
    	if (!ThreadFilter.configIdExists(config, configId)) {
    		throw new ConfigNotExistsRuntimeException(configId);
    	}

    	boolean result = super.fillParamsByConfigIdInternal(configId);

    	Map<?, ?> configs = (Map<?,?>) config.get(THREADS_CONFIG_KEY);
    	Map<?, ?> customCfg = (Map<?,?>) configs.get(configId);

		if (customCfg.containsKey(THREAD_EXTRACTORS_KEY)) {
			threadStartExtractorList = (List<String>) customCfg.get(THREAD_EXTRACTORS_KEY);
			threadStartPatternList = new ArrayList<String>();
			threadSkipEndPatternList = (List<String>) GreppUtil.getNotNull((Map<String, Object> )customCfg, THREAD_SKIPENDS_KEY, new ArrayList<String>());
			if (customCfg.containsKey(THREAD_ENDS_KEY)) {
				threadEndPatternList =  (List<String>) customCfg.get(THREAD_ENDS_KEY);
			}
			else {
				throw new PropertiesNotFoundRuntimeException(THREADS_CONFIG_KEY + "." + THREAD_ENDS_KEY + " is not filled for config: " + configId);
			}

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("{}\n{}\n{}\n{}", threadStartExtractorList, threadStartPatternList, threadSkipEndPatternList, threadEndPatternList);
			}

			result |= true;
		}
		else {
			throw new PropertiesNotFoundRuntimeException(THREADS_CONFIG_KEY + "." + THREAD_EXTRACTORS_KEY + " is not filled for config: " + configId);
		}

		return result;
    }

    @SuppressWarnings("unchecked")
	public static boolean configIdExists(Map<?, ?> config, String configId) {
		Map<?, ?> threadConfigs = (Map<?,?>) config.get(THREADS_CONFIG_KEY);
		
		if (threadConfigs != null) {
			return threadConfigs.containsKey(configId);
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
		String passedData = super.filter(blockData);
		if (passedData != null) {
			extractThreadPatterns(passedData);
		}
		return passedData;
	}

	/**
	 * Flushes all state
	 * 
	 */
	@Override
	public void flush() {
		threadStartPatternList = new ArrayList<String>();
		currentPattern = null;
		patternBuilder = new StringBuilder("(?ms)"); 
		patternParts = new ArrayList<String>();
		patternPartQualifierMap = new HashMap<String, Qualifier>();
		setFilterPattern(filterPattern); //keeping the initial one
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
			if (LOGGER.isTraceEnabled()) LOGGER.trace("Thread continues. Keeping starts");
			for (Map.Entry<String, String> extractedStart: extractThreadStarts(data).entrySet()) {
				addThreadStart(extractedStart.getKey(), extractedStart.getValue());
			}

		}
		
		String newPtrn = patternBuilder.toString();
		if(LOGGER.isTraceEnabled()) LOGGER.trace("New pattern: {}", newPtrn);
	
		if (currentPattern == null || !currentPattern.toString().equals(newPtrn)) {
			currentPattern = Pattern.compile(newPtrn);
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
			if (LOGGER.isTraceEnabled())
				LOGGER.trace(extractorPattern);
			Matcher extractorMatcher = Pattern.compile(extractorPattern).matcher(data);
			if (extractorMatcher.find())
			{
				String start = extractorMatcher.group();
				if (LOGGER.isTraceEnabled())
					LOGGER.trace("extracted; {}", start);
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
				LOGGER.trace("thrend ptrn: {}", thrend);
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
			LOGGER.trace("thrend ptrn: {}", thrend);
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
		LOGGER.trace("adding thread start: {}", start);
		if (!threadStartPatternList.contains(start))
		{
			threadStartPatternList.add(start);
			addExtendedFilterPattern(start, qlfr);
		}
		else LOGGER.trace("Start exists");
	}

	/**
	 * Removes supplied thread start. Qualifier will be identified automatically.
	 *
	 * @param start String representing thread start
	 * @param qlfr not used. Needed for reflection call
	 */
	private void removeThreadStart(String start, String qlfr)
	{
		LOGGER.trace("removing thread start: {}", start);
		threadStartPatternList.remove(start);
		removeExtendedFilterPattern(start);
	}

	
    @Override
    public void setState(Map<?,?> state) {
    	this.state = state;
    }

    @Override
    public String processEvent(Event event) {
		if (event == null) {
			throw new IllegalArgumentException("Event shouldn't be null!");
		}
		else {
			return null;
		}
    }


    protected boolean isLocked = false;

    @Override
    public void lock() {
        isLocked = true;
    }

    @Override
    public boolean refreshByConfigId(String configId) {
        if (configId == null) {
            throw new IllegalArgumentException("configId shoudn't be null!");
        }

        if (isLocked) {
            LOGGER.debug("{} refresh is locked", this.getClass().getName());
            return false;
        }

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

}
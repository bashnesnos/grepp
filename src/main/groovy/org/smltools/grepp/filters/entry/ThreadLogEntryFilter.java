package org.smltools.grepp.filters.entry;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.smltools.grepp.config.ConfigHolder;
import org.smltools.grepp.exceptions.ConfigNotExistsRuntimeException;
import org.smltools.grepp.exceptions.PropertiesNotFoundRuntimeException;
import org.smltools.grepp.filters.FilterParams;
import org.smltools.grepp.filters.enums.*;
import org.smltools.grepp.util.GreppUtil;
import groovy.util.ConfigObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class which provide grouping lines into log entries. <br>
 * Each entry has some pattern which defines it's start, and generally each
 * entry's start is another one's end and vice-versa. <br>
 * This filter works on that principle.
 * 
 * @author Alexander Semelit
 */

@FilterParams(configIdPath = ThreadLogEntryFilter.THREADS_CONFIG_KEY, replaces = LogEntryFilter.class, order = 0)
public class ThreadLogEntryFilter extends LogEntryFilter {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadLogEntryFilter.class);
	public final static String THREADS_CONFIG_KEY = "processThreads";
	public final static String THREAD_EXTRACTORS_KEY = "extractors";
	public final static String THREAD_SKIPENDS_KEY = "skipends";
	public final static String THREAD_ENDS_KEY = "ends";

	//Complex pattern processing and stuff
	private List<String> threadStartExtractorList;
	private Map<String, StringBuilder> threadStartPatternBufferMap = new LinkedHashMap<String, StringBuilder>();
	private List<String> threadSkipEndPatternList = new ArrayList<String>();
	private List<String> threadEndPatternList;
	private Deque<String> readyThreadEntriesStack = new ArrayDeque<String>();
	private Deque<StringBuilder> threadBuffers = new ArrayDeque<StringBuilder>();

	public ThreadLogEntryFilter() {
		threadBuffers.add(new StringBuilder());
		threadBuffers.add(new StringBuilder());
		threadBuffers.add(new StringBuilder());
	}

	public void setThreadExtractorList(List<String> threadStartExtractorList) {
		GreppUtil.throwIllegalAEifNull(threadStartExtractorList, "Thread strat extractors shouldn't be null");
		this.threadStartExtractorList = threadStartExtractorList;
		flush();
	}

	public void setThreadSkipEndPatternList(List<String> threadSkipEndPatternList) {
		if (threadSkipEndPatternList != null) {
			this.threadSkipEndPatternList = threadSkipEndPatternList;
		}
	}

	public void setThreadEndPatternList(List<String> threadEndPatternList) {
		GreppUtil.throwIllegalAEifNull(threadEndPatternList, "Thread ends shouldn't be null");
		this.threadEndPatternList = threadEndPatternList;
	}

	@SuppressWarnings("unchecked")
	@Override
    public boolean fillParamsByConfigId(String configId) {
    	if (!configIdExists(configId)) {
    		throw new ConfigNotExistsRuntimeException(configId);
    	}
    	boolean result = super.fillParamsByConfigId(configId);

    	Map<?, ?> configs = (Map<?,?>) config.get(THREADS_CONFIG_KEY);
    	Map<?, ?> customCfg = (Map<?,?>) configs.get(configId);

		if (customCfg.containsKey(THREAD_EXTRACTORS_KEY)) {
			setThreadExtractorList((List<String>) customCfg.get(THREAD_EXTRACTORS_KEY));
			
			setThreadSkipEndPatternList((List<String>) (List<String>) customCfg.get(THREAD_SKIPENDS_KEY));

			if (customCfg.containsKey(THREAD_ENDS_KEY)) {
				setThreadEndPatternList((List<String>) customCfg.get(THREAD_ENDS_KEY));
			}
			else {
				throw new PropertiesNotFoundRuntimeException(THREADS_CONFIG_KEY + "." + THREAD_ENDS_KEY + " is not filled for config: " + configId);
			}

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("{}\n{}\n{}", threadStartExtractorList, threadSkipEndPatternList, threadEndPatternList);
			}

			result |= true;
		}
		else {
			throw new PropertiesNotFoundRuntimeException(THREADS_CONFIG_KEY + "." + THREAD_EXTRACTORS_KEY + " is not filled for config: " + configId);
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

        ConfigObject root = super.getAsConfig(configId);
    	ConfigObject processThreads = (ConfigObject) root.getProperty(THREADS_CONFIG_KEY);
    	ConfigObject config = (ConfigObject) processThreads.getProperty(configId);
    	config.put(THREAD_EXTRACTORS_KEY, threadStartExtractorList);
    	if (!threadSkipEndPatternList.isEmpty()) {
    		config.put(THREAD_SKIPENDS_KEY, threadSkipEndPatternList);
    	}
    	config.put(THREAD_ENDS_KEY, threadEndPatternList);
    	return root;
	}

	@Override
    public String filter(String blockData) {
    	GreppUtil.throwIllegalAEifNull("Thread start and thread end patterns should be supplied via configId or explicitly", threadStartExtractorList, threadEndPatternList);
    	return super.filter(blockData);
	}

	@Override
	protected String getNoMatchResult() {
		return getNextThreadEntry();
	}

	/**
	 * Implementation of appender to start accumulating new block and to clear
	 * out previous block data from buffer.
	 * 
	 * @param blockData
	 *            String to be appended. If null was supplied simply clears
	 *            previous data from buffer
	 */
	@Override
	protected String terminateBlock(String blockData) {
		return extractThreadPatterns(super.terminateBlock(blockData));
    }

	private void addThreadEntry(String threadEntry) {
		readyThreadEntriesStack.push(threadEntry);
	}

	private String getNextThreadEntry() {
		if (readyThreadEntriesStack.isEmpty()) {
			return null;
		}
		else {
			return readyThreadEntriesStack.pop();
		}
	}

	/**
	 * Extracts thread patterns, and adds/keeps them if thread is not yet ended in the logs, or removes them is it has ended.
	 * 
	 * @param data String already matched by filter pattern.
	 */
	private String extractThreadPatterns(String data)	{
		if (data == null) { //ignoring nulls
			return data;
		}
		
		if (searchThreadEnds(data))	{
			for (String extractedStart: extractThreadStarts(data)) {
				addThreadStart(extractedStart, data); //adding so the end is not lost
				addThreadEntry(removeThreadStart(extractedStart)); //removing, so it's aggregated with previous thread entry parts
			}
			return getNextThreadEntry();
		}
		else {
			if (LOGGER.isTraceEnabled()) LOGGER.trace("Thread continues. Keeping starts");
			for (String extractedStart: extractThreadStarts(data)) {
				addThreadStart(extractedStart, data);
			}
			return getNextThreadEntry();
		}
	}


	/**
	 * Iterates through <extractor> patterns matched by PRESERVE_THREAD tag. <br>
	 * For each tries to match supplied data, and if it matches passes matched string and qualifier to supplied method. 
	 * 
	 * @param data String already matched by filter pattern.
	 * @return collection of patterns
	 */
	private List<String> extractThreadStarts(String data) {
		List<String> extractedStarts = new ArrayList<String>();
		for (String extractorPattern : threadStartExtractorList) {
			if (LOGGER.isTraceEnabled())
				LOGGER.trace(extractorPattern);
			Matcher extractorMatcher = Pattern.compile(extractorPattern).matcher(data);
			if (extractorMatcher.find()) {
				String start = extractorMatcher.group();
				if (LOGGER.isTraceEnabled())
					LOGGER.trace("extracted; {}", start);
				extractedStarts.add(start);
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
	private boolean searchThreadEnds(String data) {
		if (!shouldBeSkipped(data))	{
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
	private boolean shouldBeSkipped(String data) {
		boolean decision = false;
		Iterator<String> skipEndIter = threadSkipEndPatternList.iterator();
		while (!decision && skipEndIter.hasNext()) {
			String thrend = skipEndIter.next();
			LOGGER.trace("thrend ptrn: {}", thrend);
			decision = Pattern.compile(thrend).matcher(data).find();
		}
		return decision;
	}

	private StringBuilder getStringBuilder() {
		if (!threadBuffers.isEmpty()) {
			return threadBuffers.pop();	
		}
		else {
			return new StringBuilder();
		}
		
	}

	private void pushStringBuilder(StringBuilder stringBuilder) {
		stringBuilder.setLength(0);
		threadBuffers.push(stringBuilder);
	}

	/**
	 * Adds supplied thread start to threadStartPatternBufferMap and to a filterPattern
	 * 
	 * @param start String representing thread start
	 * @param qlfr qualifier of this thread start
	 */
	private void addThreadStart(String start, String value) {
		LOGGER.trace("adding thread start: {}", start);
		if (!threadStartPatternBufferMap.containsKey(start)) {
			threadStartPatternBufferMap.put(start, getStringBuilder().append(value));
		}
		else { //just appending to this thread
			StringBuilder threadEntryBuilder = threadStartPatternBufferMap.get(start);
			if (!value.startsWith("\n")) {
				threadEntryBuilder.append("\n"); //ensuring there is a new line
			}
			threadEntryBuilder.append(value);
		}
	}

	/**
	 * Removes supplied thread start. Qualifier will be identified automatically.
	 *
	 * @param start String representing thread start
	 * @param qlfr not used. Needed for reflection call
	 */
	private String removeThreadStart(String start) {
		LOGGER.trace("removing thread start: {}", start);
		StringBuilder threadEntry = threadStartPatternBufferMap.remove(start);
		if (threadEntry != null) {
			String result = threadEntry.toString();
			pushStringBuilder(threadEntry);
			return result;
		}
		else { //no entry was there before, so it's a new start
			return null;
		}
	}    

	/**
	 * Flushes all state
	 * 
	 */
	@Override
	public void flush() {
        super.flush();
        threadStartPatternBufferMap.clear();
        for (StringBuilder buffer : threadBuffers) {
        	buffer.setLength(0);
        }
        readyThreadEntriesStack.clear();
    }

	/**
	 * 
	 * Listens for CHUNK_ENDED event to return current accumulated block
	 */

	@Override
	protected List<String> processEventInternal(Event event) {
        switch (event)
        {
            case CHUNK_ENDED: {
            	//all pending blocks gathering
            	terminateBlock(null); //just processing what we've got
            	
            	for (StringBuilder threadEntry : threadStartPatternBufferMap.values()) {
            		addThreadEntry(threadEntry.toString());
            		pushStringBuilder(threadEntry);
            	}
            	List<String> passingVal = new ArrayList<String>(readyThreadEntriesStack);
        		flush();
  				return passingVal;
            }
            default: {
            	return null;
            }
        }
    }

}
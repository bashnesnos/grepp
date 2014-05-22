package org.smltools.grepp.filters.entry;

import java.util.Map;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Pattern;
import org.smltools.grepp.config.ConfigHolder;
import org.smltools.grepp.exceptions.ConfigNotExistsRuntimeException;
import org.smltools.grepp.exceptions.PropertiesNotFoundRuntimeException;
import org.smltools.grepp.filters.Stateful;
import org.smltools.grepp.filters.RefreshableFilterBase;
import org.smltools.grepp.filters.FilterParams;
import org.smltools.grepp.filters.enums.Event;
import groovy.util.ConfigObject;
import org.smltools.grepp.util.GreppUtil;
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

@FilterParams(configIdPath = ConfigHolder.SAVED_CONFIG_KEY, order = 0)
public class LogEntryFilter extends RefreshableFilterBase<String> implements Stateful<List<String>> {
	private static final Logger LOGGER = LoggerFactory.getLogger(LogEntryFilter.class);
	private boolean isBlockMatched = false;
	private StringBuilder curBlock = new StringBuilder();
	private Pattern logEntryPtrn = null;
	private Pattern logEntryTerminatorPtrn = null;
	private String starter = null;
	private String dateRegex = null;
    protected Map<?,?> state = new HashMap<Object, Object>();
    
    @Override
    public void setState(Map<?,?> state) {
    	this.state = state;
    	//currently thinking should we really store all state outside, or have it just for something global
    }

	public void setStarter(String starter) {
		if (starter == null) {
			throw new IllegalArgumentException("Is null by default; just don't set it");
		}
		this.starter = starter;
		setLogEntryPattern();
	}


	public void setDateRegex(String dateRegex) {
		if (dateRegex == null) {
			throw new IllegalArgumentException("Is null by default; just don't set it");
		}
		
		if (logEntryTerminatorPtrn != null) { //not adding date regex if the logEntryTerminator is set
			return;
		}

		this.dateRegex = dateRegex;
		setLogEntryPattern();
	}

	public void setLogEntryTerminatorPattern(String logEntryTerminatorPtrn) {
		GreppUtil.throwIllegalAEifNull(starter, "LogEntryTerminator shouldn't be set only after starter is set");
		this.logEntryTerminatorPtrn = Pattern.compile(logEntryTerminatorPtrn);
		LOGGER.debug("Entry terminator pattern :/{}/", logEntryTerminatorPtrn);
	}

	private void setLogEntryPattern() {
		this.logEntryPtrn = Pattern.compile((starter != null ? starter : "") + (dateRegex != null ? dateRegex : "" ));
		LOGGER.debug("Entry pattern :/{}/", this.logEntryPtrn.pattern());
        this.setState(null);
        flush();
	}



	@SuppressWarnings("unchecked")
	@Override
    public boolean fillParamsByConfigId(String configId) {
    	if (!configIdExists(configId)) {
    		throw new ConfigNotExistsRuntimeException(configId);
    	}
    	this.configId = configId;

    	Map<?, ?> configs = (Map<?,?>) config.get(ConfigHolder.SAVED_CONFIG_KEY);
    	Map<?, ?> customCfg = (Map<?,?>) configs.get(configId);

    	//invalidating any previous
		logEntryPtrn = null;
		logEntryTerminatorPtrn = null;
		starter = null;
		dateRegex = null;		


		if (customCfg.containsKey(ConfigHolder.SAVED_CONFIG_STARTER_KEY)) {
			setStarter((String) customCfg.get(ConfigHolder.SAVED_CONFIG_STARTER_KEY));
		}

		if (customCfg.containsKey(ConfigHolder.SAVED_CONFIG_TERMINATOR_KEY)) {
			setLogEntryTerminatorPattern((String) customCfg.get(ConfigHolder.SAVED_CONFIG_TERMINATOR_KEY));
		}

		if (customCfg.containsKey(ConfigHolder.SAVED_CONFIG_DATE_FORMAT_KEY)) {
			setDateRegex((String) ((Map<?, ?>) customCfg.get(ConfigHolder.SAVED_CONFIG_DATE_FORMAT_KEY)).get(ConfigHolder.SAVED_CONFIG_DATE_FORMAT_REGEX_KEY));
		}
		
		if (starter != null || dateRegex != null) {
			logEntryPtrn = Pattern.compile((starter != null ? starter : "") + (dateRegex != null ? dateRegex : "" ));
			LOGGER.debug("Entry start pattern :/{}/", logEntryPtrn.pattern());
		}
		else {
			throw new PropertiesNotFoundRuntimeException("Either " + ConfigHolder.SAVED_CONFIG_STARTER_KEY + " or " + ConfigHolder.SAVED_CONFIG_DATE_FORMAT_KEY + "." + ConfigHolder.SAVED_CONFIG_DATE_FORMAT_REGEX_KEY + " should be filled for config: " + configId);
		}

		return true;
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
        if (starter != null) {
	    	ConfigObject savedConfigs = (ConfigObject) root.getProperty(ConfigHolder.SAVED_CONFIG_KEY);
    		((ConfigObject) savedConfigs.getProperty(configId)).put(ConfigHolder.SAVED_CONFIG_STARTER_KEY, starter);
   		}

   		if (logEntryTerminatorPtrn != null) {
	    	ConfigObject savedConfigs = (ConfigObject) root.getProperty(ConfigHolder.SAVED_CONFIG_KEY);
    		((ConfigObject) savedConfigs.getProperty(configId)).put(ConfigHolder.SAVED_CONFIG_TERMINATOR_KEY, logEntryTerminatorPtrn.pattern());
   		}

    	return root;
	}

	/**
	 * Overrided check method. Once log entry start pattern was matched it
	 * accumulates data coming through until another entry start would be found. <br>
	 * Once it was found, it passes all the data which was accumulated to the
	 * next filter. <br>
	 * 
	 * @param blockData
	 *            A String to be checked.
	 */

	@Override
    public String filter(String blockData) {
  		if ( logEntryPtrn.matcher(blockData).find() ) { //finding match of current blockData
  			if (!isBlockMatched) {
  				isBlockMatched = true;
  				appendCurBlock(blockData);
  			}
  			else if (logEntryTerminatorPtrn == null) {
  		       	if (LOGGER.isTraceEnabled()) {
    	    		LOGGER.trace("Terminating by entry start");
	        	}  				
	        	isBlockMatched = true;
        		return terminateBlock(blockData);
  			}
  			else { //start matched while the previous match is not terminated 
  		       	if (LOGGER.isTraceEnabled()) {
    	    		LOGGER.trace("Start of next block inside the first; if it's a thread, use ThreadLogEntryFilter instead");
	        	}
  				appendCurBlock(blockData);
  			}
  		}
  		else if (isBlockMatched) {
			if (logEntryTerminatorPtrn != null && logEntryTerminatorPtrn.matcher(blockData).find()) {
				appendCurBlock(blockData);
				isBlockMatched = false;
				return terminateBlock(null);
			}
  			appendCurBlock(blockData);
  		}

       	if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Block continues? {};", isBlockMatched);
    	}

  		return getNoMatchResult();

    }

    protected String getNoMatchResult() {
    	return null;
    }

	/**
	 * Implementation of appender to accumulate lines
	 * 
	 * @param line
	 *            String to be appended
	 */

	private void appendCurBlock(String line) {
    	if (LOGGER.isTraceEnabled()) {
    		LOGGER.trace("appending");
    	}
        if (curBlock.length() != 0) {
        	curBlock.append('\n');
        }
        curBlock.append(line);
    }

	/**
	 * Implementation of appender to start accumulating new block and to clear
	 * out previous block data from buffer.
	 * 
	 * @param blockData
	 *            String to be appended. If null was supplied simply clears
	 *            previous data from buffer
	 */

	protected String terminateBlock(String blockData) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("returning block");
		}

		String passingVal = curBlock.toString();
        resetBuffer();
        if (blockData != null) {
            appendCurBlock(blockData);
        }

        return passingVal;
    }

    protected void resetBuffer() {
    	curBlock.setLength(0);
    }

	/**
	 * Flushes all state
	 * 
	 */
	@Override
	public void flush() {
    	isBlockMatched = false;		
        resetBuffer();
    }

    @Override
    public List<String> processEvent(Event event) {
		if (event == null) {
			throw new IllegalArgumentException("Event shouldn't be null!");
		}
		else {
			return processEventInternal(event);
		}
    }

	/**
	 * 
	 * Listens for CHUNK_ENDED event to return current accumulated block
	 */
	protected List<String> processEventInternal(Event event) {
        switch (event) {
            case CHUNK_ENDED: {
				String passingVal = curBlock.toString();
        		flush();
  				return Collections.singletonList(passingVal);
            }
            default: {
            	return null;
            }
        }
    }

}
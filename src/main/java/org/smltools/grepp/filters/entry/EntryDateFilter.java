package org.smltools.grepp.filters.entry;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.smltools.grepp.exceptions.TimeToIsOverduedException;
import org.smltools.grepp.filters.FilterBase;
import org.smltools.grepp.filters.enums.Event;

/**
 * Class provides entry date filtering for supplied FROM and TO dates.
 * 
 * @author Alexander Semelit
 * 
 */

final class EntryDateFilter extends StatefulFilterBase<String> implements OptionallyStateful<String> {
	public final static String SAVED_CONFIG_KEY = "savedConfigs";
	public final static String DATE_FORMAT_KEY = "dateFormat";
	public final static String DATE_FORMAT_REGEX_KEY = "regex";
	public final static String DATE_FORMAT_VALUE_KEY = "value";

	private final boolean isStateOptional;
	private Date from;
	private Date to;
	private boolean isDateFromPassed = false;
	private Pattern logDatePtrn = null;
	private SimpleDateFormat logDateFormat;

	/**
	 * Creates non-refreshable and non-publicly modifiable, standalone and stateless EntryDateFilter
	 * @param logDatePtrn
	 *            pattern to slice data for entries
	 */
	public EntryDateFilter(String logDatePtrn, String logDateFormat, Date from, Date to) {
		super(EntryDateFilter.class, null);
		
		if (logDatePtrn != null) {
			this.logDatePtrn = Pattern.compile(logDatePtrn);	
		}
		else {
			throw new IllegalArgumentException("logDatePtrn was not supplied");
		}
		
		if (logDateFormat != null) {
			this.logDateFormat = new SimpleDateFormat(logDateFormat);
		}	
		else {
			throw new IllegalArgumentException("logDateFormat was not supplied");
		}

		if (from != null || to != null) {
			this.from = from;
			this.to = to;
		}
		else {
			throw new IllegalArgumentException("Either 'from' or 'to' date should be supplied");	
		}
		isStateOptional = true;
	}

	/**
	* Creates LogEntryFilter from config
	*
	*/
	public EntryDateFilter(Map<?, ?> config, String configId) {
		if (config == null || configId == null) {
			throw new IllegalArgumentException("All the constructor params shouldn't be null! " + (config != null) + ";" + (configId != null));
		}

		super(EntryDateFilter.class, config);
		fillParamsByConfigIdInternal(configId);
	}

	@SuppressWarnings("unchecked")
	@Override
    protected boolean fillParamsByConfigIdInternal(String configId) {
    	if (!EntryDateFilter.configIdExists(config, configId)) {
    		throw new ConfigNotExistsRuntimeException(configId);
    	}

    	Map<?, ?> configs = (Map<?,?>) config.get(SAVED_CONFIG_KEY);
    	Map<?, ?> customCfg = (Map<?,?>) configs.get(configId);

		if (customCfg.containsKey(DATE_FORMAT_KEY))	{
			Map<?,?> dateFormatProps = (Map<?, ?>) customCfg.get(DATE_FORMAT_KEY);
			if (dateFormatProps.containsKey(DATE_FORMAT_REGEX_KEY)) {
				logDatePtrn = Pattern.compile((String) dateFormatProps.get(DATE_FORMAT_REGEX_KEY));
			}
			else {
				throw new PropertiesNotFoundRuntimeException(DATE_FORMAT_KEY + "." + DATE_FORMAT_REGEX_KEY + " is not filled for config: " + configId);
			}

			if (dateFormatProps.containsKey(DATE_FORMAT_VALUE_KEY)) {
				logDateFormat = new SimpleDateFormat((String) dateFormatProps.get(DATE_FORMAT_VALUE_KEY));
			}
			else {
				throw new PropertiesNotFoundRuntimeException(DATE_FORMAT_KEY + "." + DATE_FORMAT_REGEX_KEY + " is not filled for config: " + configId);
			}
			return true;
		}
		else {
			throw new PropertiesNotFoundRuntimeException(DATE_FORMAT_KEY + " is not filled for config: " + configId);
		}
    }

    @SuppressWarnings("unchecked")
	public static boolean configIdExists(Map<?, ?> config, String configId) {
		Map<?, ?> configs = (Map<?,?>) config.get(SAVED_CONFIG_KEY);
		if (configs != null) {
			return configs.containsKey(configId);
		}
		else {
			return false;
		}
	}

    public void setFrom(Date from) {
    	this.from = from;
    }

    public void setTo(Date to) {
    	this.to = to;
    }

	/**
	 * Checks if supplied entry suits desired from and to date and time.
	 * 
	 * @param entry
	 *            A String to be checked
	 * @throws IllegalArgumentException
	 *             if supplied blockData is not String
	 * @throws TimeToIsOverduedException
	 *             if to was passed
	 */

	@Override
	public String filter(String blockData) throws TimeToIsOverduedException {
		if (from == null && to == null) {
			throw new IllegalStateException("Either 'from' or 'to' should be supplied to the filter");
		}

		if (blockData != null && logDatePtrn != null && logDateFormat != null) {

			Date entryDate = null;

			if (!isDateFromPassed || to != null) {
				String timeString = null;

				if (LOGGER.isTraceEnabled())
					LOGGER.trace("Checking log entry {} for log date pattern |{}| and formatting to |{}|"
						, blockData, logDatePtrn, logDateFormat.toPattern());

				Matcher entryDateMatcher = logDatePtrn.matcher(blockData);
				if (entryDateMatcher.find()) {
					timeString = entryDateMatcher.group(1);
				} 
				else {
					if (LOGGER.isTraceEnabled())
						LOGGER.trace("No signs of time in here");
					return null;
				}

				try {
					entryDate = logDateFormat.parse(timeString);
				} 
				catch (ParseException e) {
					throw new RuntimeException(e); //re-throwing as unchecked exception, as it will mean that date time config is invalid 
				}
				
			} 
			else {
				if (LOGGER.isTraceEnabled())
					LOGGER.trace("Date check was skipped, dateFromPassed={}, to={}", isDateFromPassed, to);
				return isDateFromPassed;
			}

			if (entryDate != null && (from == null || !entryDate.before(from))) {
				
				if (isStateful()) {
					isDateFromPassed = true;
				}

				if (to != null) {
					if (!entryDate.after(to)) {
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("Passed to");
						}
						return blockData;
					} 
					else {
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("Not passed");
						}
						throw new TimeToIsOverduedException(logDateFormat.format(to));
					}
				}
				if (LOGGER.isTraceEnabled())
					LOGGER.trace("Passed from only");
				return blockData;
			} 
			else {
				if (LOGGER.isTraceEnabled()) LOGGER.trace("Not passed");
				return null;
			}
		}
		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Date check was totally skipped, logDatePtrn={}", logDatePtrn);
		return blockData;
	}

	/**
	 * Flushes all state
	 * 
	 */
	@Override
	public void flush() {
        isDateFromPassed = false;
    }

    @Override
	public boolean isStateful() {
		return isStateOptional;
	}

	/**
	 * Listens for CHUNK_ENDED event. Cleans isDateFromPassed in that case.
	 * 
	 */
	@Override
	protected String processEventInternal(Event event) {
		switch (event) {
			case CHUNK_ENDED:
				flush();
			default:
				return null;
		}
	}

}
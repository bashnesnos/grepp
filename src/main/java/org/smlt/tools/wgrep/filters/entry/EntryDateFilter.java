package org.smlt.tools.wgrep.filters.entry;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.smlt.tools.wgrep.exceptions.TimeToIsOverduedException;
import org.smlt.tools.wgrep.filters.enums.Event;
import org.smlt.tools.wgrep.filters.FilterBase;

/**
 * Class provides entry date filtering for supplied FROM and TO dates.
 * 
 * @author Alexander Semelit
 * 
 */

final class EntryDateFilter extends FilterBase<String> {

	private SimpleDateFormat DATE_FORMAT;
	private Date FROM_DATE;
	private Date TO_DATE;
	private boolean isDateFromPassed = false;
	private Pattern logDatePtrn = null;

	/**
	 * Creates new EntryDateFilter on top of the supplied filter chain. <br>
	 * Fills appropriate params from supplied WgrepConfig instance
	 * 
	 * @param nextFilter_
	 *            FilterBase next filter
	 * @param config
	 *            WgrepConfig instance
	 */
	public EntryDateFilter(FilterBase<String> nextFilter_, String logDatePtrn_, String logDateFormat_,
			Date from, Date to) {
		super(nextFilter_, EntryDateFilter.class);
		
		if (logDatePtrn_ != null) {
			logDatePtrn = Pattern.compile(logDatePtrn_);	
		}
		else {
			log.warn("logDatePtrn_ is not supplied");
		}
		
		if (logDateFormat_ != null) {
			DATE_FORMAT = new SimpleDateFormat(logDateFormat_);
		}	
		else {
			log.warn("logDateFormat_ is not supplied");
		}

		FROM_DATE = from;
		TO_DATE = to;
	}

	/**
	 * Checks if supplied entry suits desired from and to date and time.
	 * 
	 * @param entry
	 *            A String to be checked
	 * @throws IllegalArgumentException
	 *             if supplied blockData is not String or Matcher instance
	 * @throws TimeToIsOverduedException
	 *             if TO_DATE was passed
	 */

	@Override
	public boolean check(String blockData) throws TimeToIsOverduedException {
		if (blockData != null && logDatePtrn != null && DATE_FORMAT != null) {

			Date entryDate = null;

			if (!isDateFromPassed || TO_DATE != null) {
				String timeString = null;

				if (log.isTraceEnabled())
					log.trace("Checking log entry {} for log date pattern |{}| and formatting to |{}|"
						, blockData, logDatePtrn, DATE_FORMAT.toPattern());

				Matcher entryDateMatcher = logDatePtrn.matcher(blockData);
				if (entryDateMatcher.find()) {
					timeString = entryDateMatcher.group(1);
				} 
				else {
					if (log.isTraceEnabled())
						log.trace("No signs of time in here");
					return false;
				}

				try {
					entryDate = DATE_FORMAT.parse(timeString);
				} 
				catch (ParseException e) {
					throw new RuntimeException(e); //re-throwing as unchecked exception, as it will mean that date time config is invalid 
				}
				
			} 
			else {
				if (log.isTraceEnabled())
					log.trace("Date check was skipped, dateFromPassed={}, TO_DATE={}", isDateFromPassed, TO_DATE);
				return isDateFromPassed;
			}

			if (entryDate != null
					&& (FROM_DATE == null || FROM_DATE.compareTo(entryDate) <= 0)) {
				isDateFromPassed = true;
				if (TO_DATE != null) {
					if (TO_DATE.compareTo(entryDate) >= 0) {
						if (log.isTraceEnabled())
							log.trace("Passed TO_DATE");
						return true;
					} else {
						if (log.isTraceEnabled())
							log.trace("Not passed");
						throw new TimeToIsOverduedException(
								DATE_FORMAT.format(TO_DATE));
					}
				}
				if (log.isTraceEnabled())
					log.trace("Passed FROM_DATE only");
				return true;
			} else {
				if (log.isTraceEnabled())
					log.trace("Not passed");
				return false;
			}
		}
		if (log.isTraceEnabled())
			log.trace("Date check was totally skipped, filterPtrn={}", logDatePtrn);
		return true;
	}

	/**
	 * Listens for FILE_ENDED event. Cleans isDateFromPassed in that case.
	 * @throws TimeToIsOverduedException 
	 * @throws ParseException 
	 * 
	 */
	@Override
	protected StringBuilder gatherPrintableState(Event event, StringBuilder agg) {
		switch (event) {
		case FILE_ENDED:
			isDateFromPassed = false;
			break;
		default:
			break;
		}
		return super.gatherPrintableState(event, agg);
	}

}
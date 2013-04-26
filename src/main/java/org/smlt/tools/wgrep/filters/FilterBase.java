package org.smlt.tools.wgrep.filters;

import java.text.ParseException;

import org.smlt.tools.wgrep.exceptions.FilteringIsInterruptedException;
import org.smlt.tools.wgrep.filters.enums.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Super class for all filters. Provides filtering process template with hooking
 * methods.<br>
 * 
 * @author Alexander Semelit
 */

public abstract class FilterBase<E> {
	protected FilterBase<E> nextFilter;
	protected E passingVal;
	protected final Logger log;

	/**
	 * Constructor version for simple filters, which do not require a config.
	 * 
	 * @param nextFilter_
	 *            next filter to pass results to
	 */

	public FilterBase(FilterBase<E> nextFilter_,
			@SuppressWarnings("rawtypes") Class subclazz) {
		nextFilter = nextFilter_;
		log = LoggerFactory.getLogger(subclazz);
		if (log.isTraceEnabled()) {
			if (nextFilter_ != null)
			{
				log.trace("Added on top of " + nextFilter.getClass().getCanonicalName());	
			}
		}
	}

	/**
	 * Main filtering method. Sequence is the following: <li>1. {@link
	 * this.check()} is called</li> <li>2a. If check returned true, {@link
	 * this.beforePassing()} is called</li> <li>3a. {@link this.passNext()} is
	 * called</li> <li>2b. If check returned false, <code>null</code> is
	 * returned</li>
	 * 
	 * @param blockData
	 *            which is going to be checked
	 * @return result of {@link this.passNext} method if check passed, null
	 *         otherwise
	 * @throws TimeToIsOverduedException
	 * @throws ParseException
	 */

	public E filter(E blockData) throws FilteringIsInterruptedException {
		passingVal = null; // invalidating passingVal
		if (blockData != null && check(blockData)) {
			beforePassing(blockData);
			return passNext(blockData);
		} else {
			if (log.isTraceEnabled())
				log.trace("not passed");
			return null;
		}
	}

	/**
	 * Hook for data checking. Always returns true in base method.
	 * 
	 * @param blockData
	 *            which is going to be checked
	 * @return true if data is ok to be passed, false otherwise
	 * @throws TimeToIsOverduedException
	 * @throws ParseException
	 */

	public boolean check(Object blockData) throws FilteringIsInterruptedException {
		return true;
	}

	/**
	 * 
	 * Hook to do some actions before passing data further. Could be used to
	 * save state, or to extract something from needed data.
	 * 
	 * @param blockData
	 *            which is ok to be passed further
	 */

	public void beforePassing(Object blockData) {

	}

	/**
	 * 
	 * Passes data further in chain. If there is no nextFilter, just returns
	 * blockData.
	 * 
	 * @param blockData
	 *            which is ok to be passed further
	 * @return blockData if <code>this</code> is last in chain, result of
	 *         <code>nextFilter.filter(blockData)</code> otherwise.
	 * @throws TimeToIsOverduedException
	 * @throws ParseException
	 */

	public E passNext(E blockData) throws FilteringIsInterruptedException {
		if (log.isTraceEnabled())
			log.trace("attempting to pass to next filter");
		if (nextFilter != null) {
			if (log.isTraceEnabled())
				log.trace("nextFilter " + nextFilter.getClass());
			return nextFilter.filter(passingVal != null ? passingVal
					: blockData);
		} else {
			if (log.isTraceEnabled())
				log.trace("is last in chain");
			return passingVal != null ? passingVal : blockData;
		}
	}

	/**
	 * 
	 * Getter of next filter (which has it's own next filter etc.)
	 * 
	 * @return nextFilter value
	 */

	public FilterBase<E> getTail() {
		return nextFilter;
	}

	/**
	 * Base method for event processing. Simply passes it to the next filter and
	 * returns true if there is no next filter.
	 * 
	 * @param event
	 * @return result of <code>nextFilter.gatherPrintableState</code> and true if it
	 *         doesn't have next filter (i.e. all filters in chain has processed
	 *         that event).
	 */

	public Object processEvent(Event event) {
		if (log.isTraceEnabled())
			log.trace("Passing and gathering printable state for event: " + event);
		StringBuilder result = gatherPrintableState(event, new StringBuilder(""));
		return result.length() > 0 ? result : null;
	}
	
	/**
	 * Iterates through chain and gathers states from each filter. 
	 * 
	 * @param event
	 * @param agg StringBuiler to gather printable data
	 * @return aggregated StringBuilder
	 */
	protected StringBuilder gatherPrintableState(Event event, StringBuilder agg) {
		if (log.isTraceEnabled())
			log.trace("Gathering state");

		if (nextFilter != null) {
			return nextFilter.gatherPrintableState(event, agg);
		}
		else {
			return agg;
		}
	}
	
	protected StringBuilder appendNotNull(StringBuilder agg, Object data) {
		if (data != null)
			return agg.append(data);
		else
			return agg;
	}
}
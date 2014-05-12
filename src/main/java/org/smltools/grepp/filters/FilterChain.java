package org.smltools.grepp.filters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smltools.grepp.exceptions.FilteringIsInterruptedException;
import org.smltools.grepp.filters.enums.Event;

public class FilterChain<T> {
        private static final Logger LOGGER = LoggerFactory.getLogger(FilterChain.class);
    
	private final List<Filter<T>> filters = new ArrayList<Filter<T>>();
	private final Aggregator<T> aggregator;
	private Map<?, ?> state = new HashMap();

	public FilterChain(Aggregator<T> aggregator) {
		this.aggregator = aggregator;
	}

	public void add(Filter<T> filter) {
		if (filter != null) {
			filters.add(filter);
		}
		else {
			throw new IllegalArgumentException("Filter can't be null!");
		}
	}

	/**
	 * Main filtering method. Sequence is the following: <li>1. {@link
	 * this.check()} is called</li> <li>2a. If check returned true, {@link
	 * this.beforePassing()} is called</li> <li>3a. {@link this.passNext()} is
	 * called</li> <li>2b. If check returned false, <code>null</code> is
	 * returned</li>
	 * 
	 * @param data
	 *            which is going to be filtered
	 * @return result of {@link this.passNext} method if check passed, null
	 *         otherwise
	 * @throws FilteringIsInterruptedException
	 */

	public T filter(T data) throws FilteringIsInterruptedException {
		return filterAfter(null, data);
	}

	@SuppressWarnings("unchecked")
	public void flush() {
		state = new HashMap();
		for (Filter<?> filter: filters) {
			if (filter instanceof Stateful<?>) {
				Stateful<?> statefulFilter = (Stateful<?>) filter;
				statefulFilter.setState(state);
				statefulFilter.flush();
			}
		}
	}

	/**
	 * Base method for event processing. Simply passes it to the next filter and
	 * returns true if there is no next filter.
	 * 
	 * @param event
	 * @return result of <code>nextFilter.gatherPrintableState</code> and true if it
	 *         doesn't have next filter (i.e. all filters in chain has processed
	 *         that event).
        * @throws org.smltools.grepp.exceptions.FilteringIsInterruptedException
	 */

	public T processEvent(Event event) throws FilteringIsInterruptedException {
		if (event == null) {
			throw new IllegalArgumentException("Event can't be null!");
		}

		LOGGER.trace("Processing event: {}", event);

		T flushedData;
		Iterator<Filter<T>> filterIterator = filters.iterator();
		do {
			Filter<T> curFilter = filterIterator.next();
                        if (curFilter instanceof Stateful<?>) {
				flushedData = ((Stateful<T>) curFilter).processEvent(event);
				if (flushedData != null) {
					LOGGER.trace("Filtering flushed data from {}", curFilter.getClass());
					flushedData = filterAfter(curFilter, flushedData);
					aggregator.add(flushedData);
				}
			}
		}
		while (filterIterator.hasNext());
		return aggregator.aggregate();
	}

	public boolean refreshByConfigId(String configId) {
		boolean hasChanged = false;
		for (Filter<?> filter: filters) {
                        if (filter instanceof Configurable) {
                            hasChanged |= ((Configurable) filter).refreshByConfigId(configId);
                        }
		}
		return hasChanged;
	}

	private T filterAfter(Filter<T> filter, T data) throws FilteringIsInterruptedException {
		if (filters.isEmpty()) {
			throw new IllegalStateException("No filters in the filter chain!");
		}

		if (data != null) {
			T filteredData = data;
			
			Iterator<Filter<T>> filterIterator;
			if (filter == null) {
				filterIterator = filters.iterator();
			}
			else {
				filterIterator = filters.subList(filters.indexOf(filter) + 1, filters.size()).iterator();
			}

			while (filteredData != null && filterIterator.hasNext()) {
				filteredData = filterIterator.next().filter(filteredData);
			}
			return filteredData;
		} 
		else {
			return null;
		}
	}
}
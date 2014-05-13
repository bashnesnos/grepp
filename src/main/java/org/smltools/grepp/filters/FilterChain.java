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

public class FilterChain<T> implements Filter<T>, Stateful<T>, Refreshable {
    private static final Logger LOGGER = LoggerFactory.getLogger(FilterChain.class);
    private static final List<Class<? extends Filter>> FILTER_ORDER = new ArrayList<Class<? extends Filter>>();
    private static final Comparator<Filter<?>> BY_ORDER_FIELD = new Comparator<Filter<?>>() {
    	public int compareTo(Filter<?> a, Filter<?> b) {
    		return FILTER_ORDER.indexOf(a.getClass()).compareTo(FILTER_ORDER.indexOf(b.getClass()));
    	}
    }

    static { //filter order and other meta-data init here
    	FILTER_ORDER.add(FileDateFilter.class);
    	FILTER_ORDER.add(FileSortFilter.class);
    	FILTER_ORDER.add(LogEntryFilter.class);
    	FILTER_ORDER.add(PropertiesFilter.class);
    	FILTER_ORDER.add(ThreadFilter.class);
    	FILTER_ORDER.add(SimpleFilter.class);
    	FILTER_ORDER.add(EntryDateFilter.class);
    	FILTER_ORDER.add(PostFilter.class);
    }

	public static <V extends FilterBase> V createFilterFromConfigByConfigId(Class<V> filterClass, Map<?, ?> config, String configId) {
		if (filterClass == null || config == null || configId == null) {
			throw new IllegalArgumentException("All the method params shouldn't be null! " + (filterClass != null) + ";" + (config != null) + ";" + (configId != null));
		}

		try {
			Constructor<V> constructByConfigId = filterClass.getDeclaredConstructor(Map.class, String.class);
			V newFilter = constructByConfigId.newInstance(config, configId);
			newFilter.configId = configId;
			return newFilter;
		} catch(InvocationTargetException ite) {
			throw new RuntimeException(ite);
		} catch (NoSuchMethodException nsme) {
			throw new UnsupportedOperationException("A particular Filter implementation should have a constructor by Map and configId", nsme);
		} catch (InstantiationException ie) {
			throw new RuntimeException(ie);
		} catch (IllegalAccessException iace) {
			throw new RuntimeException(iace);
		} 
	}

	private final List<Filter<T>> filters = new ArrayList<Filter<T>>();
	private final Aggregator<T> aggregator;
	private Map<?, ?> state = new HashMap();

	private Set<Class<? extends Filter>> disabledFilters = new HashSet<Class<? extends Filter>>;

	public FilterChain(Aggregator<T> aggregator) {
		this.aggregator = aggregator;
	}

    @Override
    public void setState(Map<?,?> state) {
    	this.state = state;
    	//currently thinking should we really store all state outside, or have it just for something global
    }

	public void add(Filter<T> filter) {
		if (filter != null) {
			filters.add(filter);
			Collection.sort(filter, BY_ORDER_FIELD);
		}
		else {
			throw new IllegalArgumentException("Filter can't be null!");
		}
	}

	public void addByConfigAndConfigId(Map<?, ?> config, String configId) {
		for (Class<? extends Filter> filterClass: FILTER_ORDER) {
			if (!disabledFilters.contains(filterClass)) {
				if (filterClass.configIdExists(config, configId)) {
					add(createFilterFromConfigByConfigId(filterClass, config, configId));
				}
			}
		}
	}

	public void disableFilter(Class<? extends Filter> filterClass) {
		disabledFilters.add(filterClass);
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

	@Override
	public T filter(T data) throws FilteringIsInterruptedException {
		return filterAfter(null, data);
	}

	@SuppressWarnings("unchecked")
	@Override
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
	@Override
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

	@Override
	public boolean refreshByConfigId(String configId) {
		boolean hasChanged = false;
		for (Filter<?> filter: filters) {
                        if (filter instanceof Refreshable) {
                            hasChanged |= ((Refreshable) filter).refreshByConfigId(configId);
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
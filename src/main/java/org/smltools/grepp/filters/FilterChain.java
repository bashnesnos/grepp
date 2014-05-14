package org.smltools.grepp.filters;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smltools.grepp.exceptions.FilteringIsInterruptedException;
import org.smltools.grepp.filters.enums.Event;

public class FilterChain<T> implements Filter<T>, Stateful<T>, Refreshable {
    private static final Logger LOGGER = LoggerFactory.getLogger(FilterChain.class);
    private List<Class<? extends Filter>> filterOrderList = new ArrayList<Class<? extends Filter>>();
    private Comparator<Class<? extends Filter>> NATURAL_BY_ORDER_FIELD = new Comparator<Class<? extends Filter>>() {
        @Override
        public int compare(Class<? extends Filter> c1, Class<? extends Filter> c2) {
        	FilterParams params1 = c1.getAnnotation(FilterParams.class);
        	FilterParams params2 = c2.getAnnotation(FilterParams.class);
        	if (params1 != null) {
        		if (params2 == null) {
        			return 1;
        		}
        		else {
        			int diff = params1.order() - params2.order();
        			return diff > 0 ? -1 : diff < 0 ? 1 : 0;
        		}
        	}
        	else if (params2 != null) {
        		return -1;
        	}
        	else {
        		return 0;
        	}
        }
    };

    private Comparator<Filter<?>> naturalByOrderedList = new Comparator<Filter<?>>() {
        @Override
        public int compare(Filter<?> o1, Filter<?> o2) {
            int diff = filterOrderList.indexOf(o1.getClass()) - filterOrderList.indexOf(o2.getClass());
            return diff > 0 ? -1 : diff < 0 ? 1 : 0;
        }
    };

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
	private boolean isLocked = false;

	public FilterChain(Aggregator<T> aggregator) {
		this.aggregator = aggregator;
	}

    @Override
    public void setState(Map<?,?> state) {
    	this.state = state;
    	//currently thinking should we really store all state outside, or have it just for something global
    }

	public void add(Filter<T> filter) {
		if (isLocked) return;

		if (filter != null) {
			enableFilter(filter.getClass());
			filters.add(filter);
			Collections.sort(filters, naturalByOrderedList);
		}
		else {
			throw new IllegalArgumentException("Filter can't be null!");
		}
	}

	@SuppressWarnings("unchecked")
	public boolean addByConfigAndConfigId(Map<?, ?> config, String configId) {
			if (isLocked) return false;

            boolean wasAdded = false;
            for (Class<? extends Filter> filterClass: filterOrderList) {
				if (FilterBase.class.isAssignableFrom(filterClass)) {
                    try {
                        Method configIdExistsMethod = filterClass.getMethod("configIdExists", Map.class, String.class);
                        if ((Boolean) configIdExistsMethod.invoke(null, config, configId)) {
                            add(createFilterFromConfigByConfigId((Class<? extends FilterBase>) filterClass, config, configId));
                            wasAdded = true;
                        }
                    } catch (NoSuchMethodException nsme) {
                        throw new RuntimeException(nsme);
                    } catch (SecurityException se) {
                        throw new RuntimeException(se);
                    } catch (IllegalAccessException iae) {
                        throw new RuntimeException(iae);
                    } catch (IllegalArgumentException iare) {
                        throw new RuntimeException(iare);
                    } catch (InvocationTargetException ite) {
                        throw new RuntimeException(ite);
                    }
    	        }
			}
            return wasAdded;
	}

    @Deprecated
    public void enableFilter(Class<? extends Filter> filterClass) {
    	if (filterClass != null && !filterOrderList.contains(filterClass)) {
        	filterOrderList.add(filterClass);
        	FilterParams params = filterClass.getAnnotation(FilterParams.class);
        	if (params != null) {
        		Class<? extends Filter> replaceClass = params.replaces();
        		if (replaceClass != null && replaceClass != NoOpFilter.class) {
        			filterOrderList.remove(replaceClass);
        		}
        	}
        	Collections.sort(filterOrderList, NATURAL_BY_ORDER_FIELD);
        }
        else {
        	throw new IllegalArgumentException("Filter class shouldn't be null!");
        }
    }
        
	public void disableFilter(Class<? extends Filter> filterClass) {
		if (filterClass != null) {
			filterOrderList.remove(filterClass);
		}
        else {
        	throw new IllegalArgumentException("Filter class shouldn't be null!");
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
	@SuppressWarnings("unchecked")
	@Override
	public T processEvent(Event event) {
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
					try {
						flushedData = filterAfter(curFilter, flushedData);
						aggregator.add(flushedData);
					}
					catch (FilteringIsInterruptedException fiie) {
						LOGGER.debug("Filtering interrupted during event " + event + " processing", fiie);
					}
				}
			}
		}
		while (filterIterator.hasNext());
		return aggregator.aggregate();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean refreshByConfigId(String configId) {
		if (isLocked) return false;

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

    @Override
    public void lock() {
        isLocked = true;
    }
}
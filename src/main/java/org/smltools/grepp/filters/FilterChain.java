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
import groovy.util.ConfigObject;
import org.smltools.grepp.util.GreppUtil;
import java.io.File;
import static org.smltools.grepp.Constants.*;

public class FilterChain<T> implements Filter<T>, Stateful<T>, Refreshable, Configurable {
    private static final Logger LOGGER = LoggerFactory.getLogger(FilterChain.class);
    private static final List<Class<? extends Filter>> REGISTERED_FILTERS_LIST = new ArrayList<Class<? extends Filter>>();
	public static final String GREPP_FILTER_PLUGIN_DIR = "/plugin/filters";

    @SuppressWarnings("unchecked")
    public static void addFilterByName(String name) throws ClassNotFoundException {
    	Class<?> clazz = Class.forName(name);
    	if (Filter.class.isAssignableFrom(clazz)) {
    		REGISTERED_FILTERS_LIST.add((Class<? extends Filter>) clazz);
    	}
    	else {
    		throw new IllegalArgumentException(name + " is not an implementation of Filter");
    	}
    }
    
    @SuppressWarnings("unchecked")
    private static void addFilterByClass(Class<?> clazz) throws ClassNotFoundException {
    	if (Filter.class.isAssignableFrom(clazz)) {
    		REGISTERED_FILTERS_LIST.add((Class<? extends Filter>) clazz);
    	}
    	else {
    		LOGGER.debug("{} is not an implementation of Filter; ignoring", clazz);
    	}
    }


    static {
    	try {
	    	addFilterByName("org.smltools.grepp.filters.logfile.FileDateFilter");
	    	addFilterByName("org.smltools.grepp.filters.logfile.FileSortFilter");
	    	addFilterByName("org.smltools.grepp.filters.entry.EntryDateFilter");
	    	addFilterByName("org.smltools.grepp.filters.entry.LogEntryFilter");
	    	addFilterByName("org.smltools.grepp.filters.entry.SimpleFilter");
	    	addFilterByName("org.smltools.grepp.filters.entry.ThreadFilter");
	    	addFilterByName("org.smltools.grepp.filters.entry.ReportFilter");
	    	addFilterByName("org.smltools.grepp.filters.entry.PropertiesFilter");
	    	
	    	if (System.getProperty(GREPP_HOME_SYSTEM_OPTION) != null) {
	    		File pluginDir = new File(System.getProperty(GREPP_HOME_SYSTEM_OPTION), GREPP_FILTER_PLUGIN_DIR);
	    		if (pluginDir.exists() && pluginDir.isDirectory()) {
	    			LOGGER.trace("Plugin dir {} exists; plugging in Filters enabled", GREPP_FILTER_PLUGIN_DIR);
	    			for (File pluginFile: pluginDir.listFiles()) {
	    				LOGGER.trace("Found file: {}", pluginFile.getName());
	    				Class<?> pluginClass = GreppUtil.loadGroovyClass(pluginFile);
	    				if (pluginClass != null) {
	    					addFilterByClass(pluginClass);
	    				}
	    				else {
	    					LOGGER.error("{} was ignored class: {}", pluginFile.getName(), pluginClass);
	    				}
	    			}
	    		}
	    		else {
	    			LOGGER.trace("Plugin dir {} doesn't exist; i.e. disabled", GREPP_FILTER_PLUGIN_DIR);
	    		}
	    	}
    	}
    	catch (ClassNotFoundException cnfe) {
    		throw new RuntimeException(cnfe);
    	}
    }


    private static final Comparator<Class<? extends Filter>> NATURAL_BY_ORDER_FIELD = new Comparator<Class<? extends Filter>>() {
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

    @SuppressWarnings("unchecked")
	public static <V extends FilterBase> V createFilterFromConfigByConfigId(Class<V> filterClass, Map<?, ?> config, String configId) {
		if (filterClass == null || config == null || configId == null) {
			throw new IllegalArgumentException("All the method params shouldn't be null! " + (filterClass != null) + ";" + (config != null) + ";" + (configId != null));
		}

		try {
			V newFilter = filterClass.newInstance();
			newFilter.setConfig(config);
			if (newFilter.fillParamsByConfigId(configId)) {
				newFilter.configId = configId;
			}
			else {
				throw new RuntimeException(filterClass + " can't be instantiated from configId: " + configId);
			}
			return newFilter;
		} catch (InstantiationException ie) {
			throw new RuntimeException(ie);
		} catch (IllegalAccessException iace) {
			throw new RuntimeException(iace);
		} 
	}

    private List<Class<? extends Filter>> filterOrderList = new ArrayList<Class<? extends Filter>>();
    private Map<Class<? extends Filter>, Class<? extends Filter>> replacedFiltersMap = new HashMap<Class<? extends Filter>, Class<? extends Filter>>();
    private Comparator<Filter<?>> naturalByOrderedList = new Comparator<Filter<?>>() {
        @Override
        public int compare(Filter<?> o1, Filter<?> o2) {
            int diff = filterOrderList.indexOf(o1.getClass()) - filterOrderList.indexOf(o2.getClass());
            return diff > 0 ? -1 : diff < 0 ? 1 : 0;
        }
    };

	private final List<Filter<T>> filters = new ArrayList<Filter<T>>();
	private final Aggregator<T> aggregator;
	private Map<?, ?> config;
	private Map<?, ?> state = new HashMap();
	private boolean isLocked = false;

	public FilterChain(Map<?, ?> config, Aggregator<T> aggregator, Class<T> type) {
		if (config == null || aggregator == null || type == null) {
			throw new IllegalArgumentException("All constructor params shouldn't be null: " + (config != null) + ";" + (aggregator != null) + ";" + (type != null));
		}

		this.config = config;
		this.aggregator = aggregator;

		//filter class pick-up here
		for (Class<? extends Filter> filterClass: REGISTERED_FILTERS_LIST) {
			Class<?> parameter = GreppUtil.findParameterClass(filterClass);
			if (parameter != null && parameter.isAssignableFrom(type)) {
				if (!replacedFiltersMap.containsKey(filterClass)) {
					enableFilter(filterClass);
				}
				else {
					LOGGER.debug("<init> Ignoring attempt to enable replaced class: {}; replaced by: {}", filterClass.getName(), replacedFiltersMap.get(filterClass));					
				}
			}
		}
	}

	@Override
	public void setConfig(Map<?, ?> config) {
		this.config = config;
	}

    @Override
    public void setState(Map<?,?> state) {
    	this.state = state;
    	//currently thinking should we really store all state outside, or have it just for something global
    }

    public boolean has(Class<? extends Filter> filterClass) {
    	for (Filter<T> filter: filters) {
    		if (filter.getClass().equals(filterClass)) {
    			return true;
    		}
    	}
    	return false;
    }

    @SuppressWarnings("unchecked")
    public <E extends Filter<T>> E get(Class<E> filterClass) {
    	for (Filter<T> filter: filters) {
    		if (filter.getClass().equals(filterClass)) {
    			return (E) filter;
    		}
    	}
    	return null;
    }

    @SuppressWarnings("unchecked")
    public <E extends Filter<T>> E getInstance(Class<E> filterClass) {
    	if (filterClass != null) {
    		enableFilter(filterClass);
    	}
    	else {
    		throw new IllegalArgumentException("Filter class shouldn't be null!");
    	}

    	try {
	    	if (filterOrderList.contains(filterClass)) {
	    		return (E) filterClass.newInstance();
	    	}
	    	else if (replacedFiltersMap.containsKey(filterClass)) {
	    		return (E) replacedFiltersMap.get(filterClass).newInstance(); //assumed replacing one extends replaced by
	    	}
	    }
	    catch (InstantiationException ie) {
	    	throw new RuntimeException("A Filter implementation supposed to have a no-argument constructor!", ie);
	    }
	    catch (IllegalAccessException iae) {
			throw new RuntimeException("A Filter implementation supposed to have a public no-argument constructor!", iae);	    	
	    }
    	return null;
    }

    public void enableFilter(Class<? extends Filter> filterClass) {
    	if (filterClass != null) {
    		if (replacedFiltersMap.containsKey(filterClass)) {
    			LOGGER.debug("Replaced class: " + filterClass.getName() + "; replaced by: "+ replacedFiltersMap.get(filterClass) + "; enabling replacing class");
    			enableFilter(replacedFiltersMap.get(filterClass));
    			return;
    		}

    		if (!filterOrderList.contains(filterClass)) {
	        	filterOrderList.add(filterClass);
	        	FilterParams params = filterClass.getAnnotation(FilterParams.class);
	        	if (params != null) {
	        		Class<? extends Filter> classToReplace = params.replaces();
	        		if (classToReplace != null && classToReplace != NoOpFilter.class) {
	        			if (!classToReplace.isAssignableFrom(filterClass)) {
	        				throw new IllegalArgumentException("Replacing class " + filterClass + " should extend replacable " + classToReplace + "!");
	        			}

	        			if (!replacedFiltersMap.containsKey(classToReplace)) {
	        				filterOrderList.remove(classToReplace);
	        				replacedFiltersMap.put(classToReplace, filterClass);
	        			}
	        			else {
	        				Class<? extends Filter> alreadyReplacedByClass = replacedFiltersMap.get(classToReplace);
	        				if (!filterClass.equals(alreadyReplacedByClass)) {
	        					throw new IllegalArgumentException(classToReplace.getName() + " is already replaced by " + alreadyReplacedByClass.getName() + "; attempt to replace it with " + filterClass.getName() + " is illegal without disabling previous first");
	        				}
	        			}
	        		}
	        	}
	        	Collections.sort(filterOrderList, NATURAL_BY_ORDER_FIELD);
        	}
        }
        else {
        	throw new IllegalArgumentException("Filter class shouldn't be null!");
        }
    }
        
	public void disableFilter(Class<? extends Filter> filterClass) {
		if (filterClass != null) {
			filterOrderList.remove(filterClass);
			FilterParams params = filterClass.getAnnotation(FilterParams.class);
			if (params != null) {
				replacedFiltersMap.remove(params.replaces());
			}
		}
        else {
        	throw new IllegalArgumentException("Filter class shouldn't be null!");
        }
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
	public boolean addByConfigId(String configId) {
			if (isLocked) return false;

            boolean wasAdded = false;
            for (Class<? extends Filter> filterClass: filterOrderList) {
				if (!has(filterClass) && FilterBase.class.isAssignableFrom(filterClass)) {
                    if (configIdExists(filterClass, configId)) {
                        add(createFilterFromConfigByConfigId((Class<? extends FilterBase>) filterClass, config, configId));
                        wasAdded = true;
                    }
    	        }
			}
            return wasAdded;
	}

	@SuppressWarnings("unchecked")
	private boolean configIdExists(Class<? extends Filter> filterClass, String configId) {
		if (FilterBase.class.isAssignableFrom(filterClass)) {
            try {
                Method configIdExistsMethod = filterClass.getMethod("configIdExists", Map.class, String.class);
                return (Boolean) configIdExistsMethod.invoke(null, config, configId);
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
        else {
        	return false;
        }
	}

    @SuppressWarnings("unchecked")
	public boolean configIdExists(String configId) {
		boolean exists = false;
        for (Class<? extends Filter> filterClass: filterOrderList) {
        	exists |= configIdExists(filterClass, configId);
        }
        return exists;
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
    public ConfigObject getAsConfig(String configId) {
        ConfigObject root = new ConfigObject();

        for (Filter<?> filter: filters) {
        	if (filter instanceof Configurable) {
        		root.merge(((Configurable) filter).getAsConfig(configId));
        	}
        }

        return root;
    }

    @Override
    public boolean fillParamsByConfigId(String configId) {
    	return refreshByConfigId(configId);
    }

	@SuppressWarnings("unchecked")
	@Override
	public boolean refreshByConfigId(String configId) {
		if (isLocked) return false;

		boolean hasChanged = false;
		for (Filter<?> filter: filters) {
            if (filter instanceof Refreshable) {
            	if (filter instanceof Configurable) {
            		((Configurable) filter).setConfig(config);
            	}
            	
                hasChanged |= ((Refreshable) filter).refreshByConfigId(configId);
            }
		}

		hasChanged |= addByConfigId(configId);

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
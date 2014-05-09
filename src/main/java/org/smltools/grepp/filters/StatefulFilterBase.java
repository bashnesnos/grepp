package org.smltools.grepp.filters;

import java.text.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smltools.grepp.exceptions.FilteringIsInterruptedException;
import org.smltools.grepp.filters.FilterBase;
import org.smltools.grepp.filters.enums.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Super class for all filters. Provides filtering process template with hooking
 * methods.<br>
 * 
 * @author Alexander Semelit
 */
public abstract class StatefulFilterBase<T> extends FilterBase<T> implements Stateful<T> {
	protected Map<?,?> state = new HashMap<Object, Object>();

    @Override
    public void setState(Map<?,?> state) {
    	this.state = state;
    	//currently thinking should we really store all state outside, or have it just for something global
    }

    @Override
    public T processEvent(Event event) {
		if (event == null) {
			throw new IllegalArgumentException("Event shouldn't be null!");
		}
		else {
			return processEventInternal(event);
		}
    }

   	protected abstract String processEventInternal(Event event);

}
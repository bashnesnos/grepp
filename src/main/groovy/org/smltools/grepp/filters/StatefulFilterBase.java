package org.smltools.grepp.filters;

import java.util.HashMap;
import java.util.Map;
import org.smltools.grepp.filters.enums.*;

/**
 * Super class for all filters. Provides filtering process template with hooking
 * methods.<br>
 * 
 * @author Alexander Semelit
 * @param <T>
 */
public abstract class StatefulFilterBase<T> extends RefreshableFilterBase<T> implements Stateful<T> {
	
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

   	protected abstract T processEventInternal(Event event);

}
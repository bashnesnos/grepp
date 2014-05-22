package org.smltools.grepp.filters;

import java.util.Map;
import org.smltools.grepp.filters.enums.Event;

public interface Stateful<T> {
	void setState(Map<?,?> state);

	void flush();

	/**
	 * Base method for event processing. Simply passes it to the next filter and
	 * returns true if there is no next filter.
	 * 
	 * @param event
	 * @return whatever filter has to return as reaction to that event
	 */

	public T processEvent(Event event);
}
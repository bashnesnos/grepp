package org.smlt.tools.wgrep.output;

import org.smlt.tools.wgrep.config.ModuleBase
import groovy.util.logging.Slf4j
import org.smlt.tools.wgrep.filters.enums.Event
import org.smlt.tools.wgrep.filters.FilterChainFactory
import org.smlt.tools.wgrep.filters.FilterBase


/**
 * 
 * Simple output class.
 * 
 * @author Alexander Semelit
 *
 */

@Slf4j
public class SimpleOutput implements WgrepOutput {
	
	protected PrintWriter printer;
	protected FilterChainFactory filterFactory;
	protected ingoreClose = false;
	protected FilterBase filterChain;
	
	SimpleOutput(FilterChainFactory filterFactory_, PrintWriter printer_, boolean ignoreClose) {
		filterFactory = filterFactory_
		printer = printer_
		filterChain = filterFactory.createFilterChain()

	}
	
	@Override
	public void printToOutput(Object data) {
		printNotFiltered(filterChain.filter(data))
	}

	@Override
	public void closeOutput() {
		if (!ingoreClose) {
			printer.close();
		}
	}

	@Override
	public void refreshFilters(Object criteria) {
		if (criteria instanceof String) {
			try {
				if (filterFactory.refreshConfigByFile((String) criteria))
				{
					filterChain = filterFactory.createFilterChain()
				}
			}
			catch(IllegalArgumentException e) {
				e.printStackTrace(printer)
			}
		}
	}
	
	@Override
	public void printEventToOutput(Event event) {
		printNotFiltered(filterChain.processEvent(event))
	}

	protected void printNotFiltered(Object data) {
		if ( data != null)
		{
			printer.println(data)
		}
		else
		{
			log.trace("data is null, not printing it")
		}
	}
	
}

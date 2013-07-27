package org.smlt.tools.wgrep.output;

import groovy.util.logging.Slf4j
import org.smlt.tools.wgrep.config.ParamsHolder
import org.smlt.tools.wgrep.config.ParamsHolderFactory
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
public class SimpleOutput implements WgrepOutput<Object, String> {
	
	protected PrintWriter printer;
	protected ParamsHolder params;
	protected FilterBase filterChain;
	
	SimpleOutput(ParamsHolder params_, PrintWriter printer_) {
		printer = printer_
		params = params_
		filterChain = FilterChainFactory.createFilterChain(params)
	}
	
	@Override
	public void printToOutput(Object data) {
		printNotFiltered(filterChain.filter(data))
	}

	@Override
	public void closeOutput() {
		if (printer != null) {
			printer.close();
		}
	}

	@Override
	public void refreshFilters(String criteria) {
			try {
				if (params.refresh(criteria))
				{
					filterChain = FilterChainFactory.createFilterChain(params)
				}
			}
			catch(IllegalArgumentException e) {
				log.debug(e)
			}
	}
	
	@Override
	public void processEvent(Event event) {
		printNotFiltered(filterChain.processEvent(event))
	}

	protected void printNotFiltered(Object data) {
		if (data != null)
		{
			printer.println(data)
		}
		else
		{
			log.trace("data is null, not printing it")
		}
	}
	
}

package org.smltools.grepp.output;

import groovy.util.logging.Slf4j

import org.smltools.grepp.config.ParamHolder
import org.smltools.grepp.config.ParamHolderFactory
import org.smltools.grepp.filters.enums.Event
import org.smltools.grepp.filters.FilterBase
import org.smltools.grepp.output.GreppOutput;
import org.smltools.grepp.filters.FilterChainFactory;


/**
 * 
 * Simple output class.
 * 
 * @author Alexander Semelit
 *
 */

@Slf4j
public class SimpleOutput implements GreppOutput<Object, String> {
	
	protected PrintWriter printer;
	protected ParamHolder params;
	protected FilterBase filterChain;
	
	SimpleOutput(ParamHolder params_) {
		this(params_, null)
	}
	
	SimpleOutput(ParamHolder params_, PrintWriter printer_) {
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
		if (params.refresh(criteria))
		{
			filterChain = FilterChainFactory.createFilterChain(params)
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

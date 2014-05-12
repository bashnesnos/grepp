package org.smltools.grepp.output;

import groovy.util.logging.Slf4j
import org.smltools.grepp.config.ConfigHolder

import org.smltools.grepp.config.ParamHolder
import org.smltools.grepp.config.ParamHolderFactory
import org.smltools.grepp.filters.enums.Event
import org.smltools.grepp.filters.FilterChain
import org.smltools.grepp.output.GreppOutput;
import org.smltools.grepp.output.Refreshable;
import org.smltools.grepp.filters.FilterChainFactory;


/**
 * 
 * Simple output class.
 * 
 * @author Alexander Semelit
 *
 */

@Slf4j
public class SimpleOutput<T> implements GreppOutput<T>, Refreshable<String> {
	
	protected PrintWriter printer;
        protected ConfigHolder config;
	protected FilterChain<T> filterChain;
	
	SimpleOutput(ConfigHolder config, FilterChain<T> filterChain) {
		this(config, filterChain, null)
	}
	
	SimpleOutput(ConfigHolder config, FilterChain<T> filterChain, PrintWriter printer) {
		this.printer = printer
		this.config = config
		this.filterChain = filterChain
	}
	
        @Override
        public void flush() {
            filterChain.flush()
        }
    
	@Override
	public void printToOutput(T data) {
		printNotFiltered(filterChain.filter(data))
	}

	@Override
	public void closeOutput() {
		if (printer != null) {
			printer.close();
		}
	}

	@Override
	public void refreshFilters(String fileName) {
                String configId = GreppUtil.findConfigIdByFileName(config, fileName)
		if (configId != null) {
			filterChain.refreshByConfigId(configId)
		}
	}
	
	@Override
	public void processEvent(Event event) {
		printNotFiltered(filterChain.processEvent(event))
	}

	protected void printNotFiltered(T data) {
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

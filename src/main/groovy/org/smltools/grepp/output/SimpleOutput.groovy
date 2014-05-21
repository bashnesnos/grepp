package org.smltools.grepp.output;

import groovy.util.logging.Slf4j
import org.smltools.grepp.config.ConfigHolder

import org.smltools.grepp.filters.enums.Event
import org.smltools.grepp.filters.FilterChain
import org.smltools.grepp.output.GreppOutput;
import org.smltools.grepp.output.RefreshableOutput;


/**
 * 
 * Simple output class.
 * 
 * @author Alexander Semelit
 *
 */

@Slf4j("LOGGER")
public class SimpleOutput<T> implements GreppOutput<T>, RefreshableOutput<String> {
	
	protected PrintWriter printer;
    protected ConfigHolder config;
	protected FilterChain<T> filterChain;
	
	public SimpleOutput(ConfigHolder config, FilterChain<T> filterChain) {
		this(config, filterChain, null)
	}
	
	public SimpleOutput(ConfigHolder config, FilterChain<T> filterChain, PrintWriter printer) {
		this.printer = printer
		this.config = config
		this.filterChain = filterChain
	}
	
    @Override
    public void flush() {
    	if (filterChain != null) {
        	filterChain.flush()
        }
    }
    
	@Override
	public void print(T data) {
		printNotFiltered(filterChain != null ? filterChain.filter(data) : data)
	}

	@Override
	public void close() {
		flush()
		if (printer != null) {
			printer.close();
		}
	}

	@Override
	public void refreshFilters(String fileName) {
		if (filterChain != null) {
	        String configId = ConfigHolder.findConfigIdByFileName(config, fileName)
			if (configId != null) {
				filterChain.refreshByConfigId(configId)
			}
		}
	}
	
	@Override
	public void processEvent(Event event) {
		if (filterChain != null) {
			printNotFiltered(filterChain.processEvent(event))
		}
	}

	protected void printNotFiltered(T data) {
		if (data != null) {
			printer.println(data)
		}
		else {
			LOGGER.trace("Nothing to print")
		}
	}
	
}

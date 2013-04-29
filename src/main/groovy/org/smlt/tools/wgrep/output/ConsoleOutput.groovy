package org.smlt.tools.wgrep.output;

import groovy.util.logging.Slf4j;

/**
 * 
 * Simple console output class.
 * 
 * @author Alexander Semelit
 *
 */

@Slf4j
public class ConsoleOutput implements WgrepOutput {
	
	private PrintWriter printer;
	
	ConsoleOutput() {
		def console = System.console()
		if (console != null) {
			printer = console.writer()	
		}
		else {
			log.debug("There is no associated console to use with this output! Defaulting to System.out.");
			printer = new PrintWriter(System.out, true)
		}
	}
	
	@Override
	public void printToOutput(Object data) {
		if (data != null)
		{
			printer.println(data)
		}
		else 
		{
			log.trace("data is null, not printing it")
		}
	}

	@Override
	public void closeOutput() {
		//ignoring close since it will close whole console or STDOUT	
	}

}

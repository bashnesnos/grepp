package org.smltools.grepp.output;

import org.smltools.grepp.config.*
import org.smltools.grepp.output.GreppOutput;
import org.smltools.grepp.filters.FilterChainFactory;
import groovy.util.logging.Slf4j;

@Slf4j
class OutputFactory {
			
	public static GreppOutput<?> getOutputInstance(ConfigHolder configHolder, boolean parse, boolean spool) {
		PrintWriter printer = null;
		if (parse) {
			log.info("Creating config output")
			return new ConfigOutput(configHolder)
		}
		else if (spool) {
			log.info("Creating file output")
			printer = getFilePrinter(configHolder)
			return new SimpleOutput(configHolder, printer)
		}
		else
		{
			log.info("Creating console output")
			printer = getConsolePrinter()
			return new SimpleOutput(configHolder, printer)
		}
	}
	
	private static PrintWriter getConsolePrinter() {
		def console = System.console()
		if (console != null) {
			return console.writer()
		}
		else {
			log.debug("There is no associated console to use with this output! Defaulting to System.out.");
			return new PrintWriter(System.out, true)
		}
	}
	
	private static PrintWriter getFilePrinter(ConfigHolder configHolder) {
		def outputDir = new File(new File(paramsHolder.get(Param.HOME_DIR)), paramsHolder.get(Param.RESULTS_DIR))
		if (!outputDir.exists()) outputDir.mkdir()
		def out_file = new File(outputDir, paramsHolder.getSpoolFileName())
		log.trace("Creating new file: {}", out_file.getCanonicalPath())
		out_file.createNewFile()
		return new PrintWriter(new FileWriter(out_file), true) //autoflushing PrintWriter
	}
}
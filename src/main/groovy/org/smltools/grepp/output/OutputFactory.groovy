package org.smltools.grepp.output;

import org.smltools.grepp.config.*
import org.smltools.grepp.output.WgrepOutput;
import org.smltools.grepp.filters.FilterChainFactory;
import groovy.util.logging.Slf4j;

@Slf4j
class OutputFactory {
			
	public static WgrepOutput<?,?> getOutputInstance(ParamsHolder paramsHolder) {
		PrintWriter printer = null;
		if (!paramsHolder.checkParamIsEmpty(Param.PARSE_PROPERTIES)) {
			log.info("Creating config output")
			return new ConfigOutput(paramsHolder)
		}
		if (!paramsHolder.checkParamIsEmpty(Param.SPOOLING)) {
			log.info("Creating file output")
			printer = getFilePrinter(paramsHolder)
			return new SimpleOutput(paramsHolder, printer)
		}
		else
		{
			log.info("Creating console output")
			printer = getConsolePrinter()
			return new SimpleOutput(paramsHolder, printer)
		}
	}
	
	private static getConsolePrinter() {
		def console = System.console()
		if (console != null) {
			return console.writer()
		}
		else {
			log.debug("There is no associated console to use with this output! Defaulting to System.out.");
			return new PrintWriter(System.out, true)
		}
	}
	
	private static PrintWriter getFilePrinter(ParamsHolder paramsHolder) {
		def outputDir = new File(new File(paramsHolder.get(Param.HOME_DIR)), paramsHolder.get(Param.RESULTS_DIR))
		if (!outputDir.exists()) outputDir.mkdir()
		def out_file = new File(outputDir, paramsHolder.get(Param.FILTER_PATTERN).replaceAll("[^\\p{L}\\p{N}]", {""}) + paramsHolder.get(Param.SPOOLING_EXT))
		log.trace("Creating new file: {}", out_file.getCanonicalPath())
		out_file.createNewFile()
		return new PrintWriter(new FileWriter(out_file), true) //autoflushing PrintWriter
	}
}
package org.smlt.tools.wgrep.output;

import org.smlt.tools.wgrep.config.*
import org.smlt.tools.wgrep.filters.FilterChainFactory;
import org.springframework.beans.factory.annotation.Autowired;
import groovy.util.logging.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

@Slf4j
class OutputFactory extends ModuleBase {
	
	private FilterChainFactory filterFactory

	public FilterChainFactory getFilterFactory()
	{
		return filterFactory
	}

	public void setFilterFactory(FilterChainFactory filterFactory_)
	{
		filterFactory = filterFactory_
	}
		
	public WgrepOutput<?,?> getOutputInstance() {
		PrintWriter printer = null;
		if (getParam('PARSE_PROPERTIES') != null) {
			return new ConfigOutput(filterFactory, getParam("configFilePath"))
		}
		if (getParam('SPOOLING') != null) {
			printer = getFilePrinter()
			return new SimpleOutput(filterFactory, printer)
		}
		else
		{
			printer = getConsolePrinter()
			return new SimpleOutput(filterFactory, printer)
		}
	}
	
	private getConsolePrinter() {
		def console = System.console()
		if (console != null) {
			return console.writer()
		}
		else {
			log.debug("There is no associated console to use with this output! Defaulting to System.out.");
			return new PrintWriter(System.out, true)
		}
	}
	
	private PrintWriter getFilePrinter() {
		def outputDir = new File(new File(getParam('HOME_DIR')), getParam('RESULTS_DIR'))
		if (!outputDir.exists()) outputDir.mkdir()
		def out_file = new File(outputDir, getParam('FILTER_PATTERN').replaceAll("[^\\p{L}\\p{N}]", {""}) + getParam('SPOOLING_EXT'))
		log.trace("Creating new file: {}", out_file.getCanonicalPath())
		out_file.createNewFile()
		return new PrintWriter(new FileWriter(out_file), true) //autoflushing PrintWriter
	}
}
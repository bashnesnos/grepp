package org.smlt.tools.wgrep.output;

import groovy.util.logging.Slf4j;

import java.io.PrintWriter;

/**
 * Class for spooling output into a file.
 * 
 * @author Alexander Semelit
 *
 */
@Slf4j
public class FileOutput implements WgrepOutput{
	
	private PrintWriter printer;
	
	FileOutput(def outputDirPath, def fileName) {
		def outputDir = new File(outputDirPath) //configInstance.getParam('HOME_DIR') + configInstance.getParam('FOLDER_SEPARATOR') + configInstance.getParam('RESULTS_DIR')
		if (!outputDir.exists()) outputDir.mkdir()
		def out_file = new File(outputDir, fileName) //configInstance.getParam('FOLDER_SEPARATOR') + configInstance.getParam('FILTER_PATTERN').replaceAll("[^\\p{L}\\p{N}]", {""}) + getParam('SPOOLING_EXT'))
		log.trace("Creating new file: " + out_file.getCanonicalPath())
		out_file.createNewFile()
		printer = new PrintWriter(new FileWriter(out_file), true) //autoflushing PrintWriter
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
		printer.close()
	}

}

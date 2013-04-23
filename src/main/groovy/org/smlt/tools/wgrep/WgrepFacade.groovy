package org.smlt.tools.wgrep

import groovy.util.logging.Slf4j
import org.smlt.tools.wgrep.config.WgrepConfig
import org.smlt.tools.wgrep.output.ConsoleOutput
import org.smlt.tools.wgrep.output.FileOutput
import org.smlt.tools.wgrep.output.WgrepOutput
import org.smlt.tools.wgrep.processors.DataProcessor
import org.smlt.tools.wgrep.processors.FileProcessor
import org.smlt.tools.wgrep.config.ModuleBase;

/**
 * A Facade linking config and modules. Provides facade methods to do the processing.
 *
 * @author Alexander Semelit 
 */
@Slf4j
class WgrepFacade extends ModuleBase {
	private Date startTime = new Date()

	private DataProcessor dataProcessor = null
	private ConsoleOutput consoleOutput = new ConsoleOutput()
	private WgrepOutput output

	/**
	 *
	 * Constructor. Simply associates facade instance with a config instance.
	 *
	 */

	WgrepFacade(WgrepConfig config) {
		super(config)
	}

	//Getters

	/**
	 * Returns <code>WgrepConfig</code> instance associated with this facade.
	 * @return <code>WgrepConfig</code>
	 */

	WgrepConfig getConfig()
	{
		return configInstance
	}

	/**
	 * Returns <code>FileProcessor</code> instance associated with this facade.
	 * @return <code>FileProcessor</code>
	 */

	FileProcessor getFileProcessor()
	{
		return dataProcessor
	}

	/**
	 * Method checks if all the main vars are fulfilled.
	 * @return <code>true</code> if check is passed. <code>false</code> otherwise.
	 */

	boolean check()
	{

		if (configInstance.getParam('FILES').isEmpty())
		{
			println "No file to wgrep"
			return false
		}

		if (configInstance.getParam('LOG_ENTRY_PATTERN') == null)
		{
			log.warn("No log entry pattern.")
		}

		if (configInstance.getParam('FILTER_PATTERN') == null)
		{
			log.warn("No filter pattern.")
		}

		return true
	}

	//General

	/**
	 * Method for module initialization.
	 * Does what is needed to initialize all the used modules appropriately
	 *
	 */

	private void moduleInit()
	{
		if (configInstance.getParam('SPOOLING') != null) {
			output = new FileOutput(new File(new File(configInstance.getParam('HOME_DIR')), configInstance.getParam('RESULTS_DIR')).getCanonicalPath(), configInstance.getParam('FILTER_PATTERN').replaceAll("[^\\p{L}\\p{N}]", {""}) + getParam('SPOOLING_EXT'))
		}
		else
		{
			output = new ConsoleOutput()
		}
		dataProcessor = FileProcessor.getInstance(configInstance, output)
	}

	/**
	 * Method to trigger processing of supplied files.
	 * Sequence is the following:
	 * <li>1. Passes supplied arguments to the <code>configInstance</code></li>
	 * <li>2. Calls <code>moduleInit</code></li>
	 * <li>3. Performs validation via <code>check</code> method</li>
	 * <li>4. Calls processing method of initialized FileProcessor</li>
	 * @param args Command-line style arguments
	 */

	void doProcessing(def args)
	{
		try {
			configInstance.processInVars(args)
			moduleInit()
			if (!check()) {
				return
			}
			dataProcessor.process()
			log.info("Processing time = " + ((new Date().getTime() - startTime.getTime())/1000)) + " sec"
		}
		catch(Exception e)
		{
			log.error("An unexpected exception occured", e)
			consoleOutput.printToOutput("Abnormal termination due to " + e.message + ". Check details in the log")
		}
	}


}
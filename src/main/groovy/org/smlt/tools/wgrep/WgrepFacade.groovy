package org.smlt.tools.wgrep

import groovy.util.logging.Slf4j;

import org.smlt.tools.wgrep.varparsers.*

/**
 * A Facade linking config and modules. Provides facade methods to do the processing.
 */
@Slf4j
class WgrepFacade {
	private Date startTime = new Date()

	private WgrepConfig configInstance = null

	private FileProcessor fProcessor = null

	/**
	*
	* Constructor. Simply associates facade instance with a config instance.
	*
	*/

	WgrepFacade(WgrepConfig config) {
		configInstance = config
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
		return fProcessor
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
		fProcessor = FileProcessor.getInstance(configInstance)
	}

	/**
	 * Initializes spooling, i.e. redirects System.out to a file. 
	 * <p>
	 * File is created in the <code>HOME_DIR</code> folder with name compiled from <code>FILTER_PATTERN</code> and extension as <code>SPOOLING_EXT</code>
	 */

	void spool()
	{
		if (configInstance.getParam('SPOOLING') != null)
		{
			def resultsDir = new File(configInstance.getParam('HOME_DIR') + configInstance.getParam('FOLDER_SEPARATOR') + configInstance.getParam('RESULTS_DIR'))
			if (!resultsDir.exists()) resultsDir.mkdir()
			def out_file = new File(resultsDir.getAbsolutePath() + configInstance.getParam('FOLDER_SEPARATOR') + configInstance.getParam('FILTER_PATTERN').replaceAll("[^\\p{L}\\p{N}]", {""}) + getParam('SPOOLING_EXT'))
			log.trace("Creating new file: " + out_file.getAbsolutePath())
			out_file.createNewFile()
			System.setOut(new PrintStream(new FileOutputStream(out_file)))
		}
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
			if (!check()) return
			spool()
			fProcessor.processAll()
			log.info("Processing time = " + ((new Date().getTime() - startTime.getTime())/1000)) + " sec"
		}
		catch(Exception e)
		{
			log.error("An exception occured. Check the log", e)
			println "Check the log for errors"
		}
	}
	
	
}
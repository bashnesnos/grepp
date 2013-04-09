package org.smlt.tools.wgrep

import groovy.util.logging.Slf4j;

import org.smlt.tools.wgrep.varparsers.*

/**
 * Facade-Singleton containing all the configuration and is linked with main modules.
 * <p>
 * As well is carrying out config parsing, incoming variable parsing, modules initialization.
 * Pretty much holds most of the program params.
 */
@Slf4j
class WgrepFacade {
	private Date startTime = new Date()


	private WgrepConfig configInstance = null

	private FileProcessor fProcessor


	WgrepFacade(WgrepConfig config) {
		configInstance = config
	}


	WgrepConfig getConfig()
	{
		return configInstance
	}

	PatternAutomationHelper getPatternHelper()
	{
		return configInstance.getParam('paHelper')
	}

	FileProcessor getFileProcessor()
	{
		return fProcessor
	}

	/**
	 * Method checks if all the main vars are not nulls.
	 * @return <code>1</code> if check is passed. <code>null</code> otherwise.
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
			log.trace("No log entry pattern.")
		}

		if (configInstance.getParam('FILTER_PATTERN') == null)
		{
			log.trace("No filter pattern.")
		}

		return true
	}

	//General

	void moduleInit()
	{
		fProcessor = FileProcessor.getInstance(configInstance)
	}

	/**
	 * Initializes spooling, i.e. redirects System.out to a file. 
	 * <p>
	 * File is created in the {@link this.HOME_DIR} folder with name compiled from {@link this.FILTER_PATTERN} and extension as {@link this.SPOOLING_EXT}
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
	 * Method to trigger processing of supplied files. Contains hook to start spooling.
	 */

	void startProcessing(def args)
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
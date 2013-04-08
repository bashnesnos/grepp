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

	private static WgrepFacade facadeInstance

	/**
	 * Creates the only facade instance.
	 * @param args Params needed for the facade initialization. Currently only path to the config.xml is expected.
	 * @return <code>facadeInstance</code>
	 */

	static WgrepFacade getInstance() {
		if (facadeInstance == null) {
			facadeInstance = new WgrepFacade()
		}
		return facadeInstance
	}

	/**
	 * Nullifies facadeInstance. It allows to recreate the Facade. Used for test purposes.
	 * @param args Params needed for the facade initialization. Currently only path to the config.xml is expected.
	 * @return <code>facadeInstance</code>
	 */

	static void reset() {
		facadeInstance = null
	}

	private WgrepConfig configInstance = null

	private FileProcessor fProcessor


	def initConfig(def configFile) {
		configInstance = new WgrepConfig(configFile)
	}



	// Getters

	/**
	 * Getter for parsed <code>documentElement</code> of the parsed config.xml
	 * @return Value of <code>root</code>
	 */

	def getRoot()
	{
		return getParam('root')
	}

	/**
	 * Getter to extract CDATA element value from a node which is expected to be text.
	 * @return <code>node.text()</code> if the node has text. Value of CDATA element i.e. <code>node.getFirstChild().getNodeValue()</code> otherwise.
	 */

	def getCDATA(def node)
	{
		return configInstance.getCDATA(node)
	}

	/**
	 * Gets value of the {@link this.extraParams} by key.
	 * @param field Key for <code>extraParams</code> which is needed to be get.
	 * @return Value set to the key <code>field</code>
	 */

	def getParam(def field)
	{
		log.trace("Accessing param: " + field)
		return configInstance.getParam(field)
	}

	def getConfig()
	{
		return configInstance
	}

	def getPatternHelper()
	{
		return getParam('paHelper')
	}

	def getFileProcessor()
	{
		return fProcessor
	}

	// Setters

	/**
	 * Sets value of any field which exists in <code>WgrepFacade</code> via reflection. Is used to propagate value directly from config.xml
	 * @param field Name of field of <code>WgrepFacade</code> which is needed to be set.
	 * @param val Value to be set
	 */

	def setParam(def field, def val)
	{
		log.trace("Setting param: " + field + " val: " + val)
		configInstance.setParam(field, val)
	}

	/**
	 * Sets value of <code>LOG_ENTRY_PATTERN</code> field which exists in <code>WgrepFacade</code> in a classical setter way.
	 * @param val <code>String</code> value to be set
	 */

	def setLogEntryPattern(def val)
	{
		setParam('LOG_ENTRY_PATTERN', val)
	}

	/**
	 * Sets value of <code>FILTER_PATTERN</code> field which exists in <code>WgrepFacade</code> in a classical setter way. If extended pattern processing is enabled it will pre-processed to extract the left-most pattern first.
	 * @param val <code>String</code> value to be set
	 */

	def setFilterPattern(def val)
	{
		setParam('FILTER_PATTERN', val)
	}

	/**
	 * Sets value of <code>SPOOLING_EXT</code> field which exists in <code>WgrepFacade</code> in a classical setter way.
	 * @param val <code>String</code> value to be set
	 */

	def setSpoolingExt(def val)
	{
		setParam('SPOOLING_EXT', val)
	}

	/**
	 * Sets value of <code>FILES</code> field which exists in <code>WgrepFacade</code> in a classical setter way. Initializes {@link FileProcessor} at the same time.
	 * @param val <code>String</code> value to be set
	 */

	def addFileName(def val)
	{
		if (val != null) setParam('FILES', val)
	}



	def subscribeParser(DefaultVarParser parser)
	{
		configInstance.subscribeVarParsers(parser)
	}

	def unsubscribeParser(DefaultVarParser parser)
	{
		configInstance.unsubscribeVarParsers(parser)
	}

	/**
	 * Method checks if all the main vars are not nulls.
	 * @return <code>1</code> if check is passed. <code>null</code> otherwise.
	 */

	def check()
	{

		if (getParam('FILES').isEmpty())
		{
			println "No file to wgrep"
			return false
		}

		if (getParam('LOG_ENTRY_PATTERN') == null)
		{
			log.trace("No log entry pattern.")
		}

		if (getParam('FILTER_PATTERN') == null)
		{
			log.trace("No filter pattern.")
		}

		return true
	}

	//General

	def moduleInit()
	{
		fProcessor = FileProcessor.getInstance()
	}

	/**
	 * Initializes spooling, i.e. redirects System.out to a file. 
	 * <p>
	 * File is created in the {@link this.HOME_DIR} folder with name compiled from {@link this.FILTER_PATTERN} and extension as {@link this.SPOOLING_EXT}
	 */

	def spool()
	{
		if (getParam('SPOOLING') != null)
		{
			def resultsDir = new File(getParam('HOME_DIR') + getParam('FOLDER_SEPARATOR') + getParam('RESULTS_DIR'))
			if (!resultsDir.exists()) resultsDir.mkdir()
			def out_file = new File(resultsDir.getAbsolutePath() + getParam('FOLDER_SEPARATOR') + getParam('FILTER_PATTERN').replaceAll("[^\\p{L}\\p{N}]", {""}) + getParam('SPOOLING_EXT'))
			log.trace("Creating new file: " + out_file.getAbsolutePath())
			out_file.createNewFile()
			System.setOut(new PrintStream(new FileOutputStream(out_file)))
		}
	}

	/**
	 * Method to trigger processing of supplied files. Contains hook to start spooling.
	 */

	def startProcessing(def args)
	{
		configInstance.processInVars(args)
		moduleInit()
		if (!check()) return
			spool()
		fProcessor.processAll()
		log.info("Processing time = " + ((new Date().getTime() - startTime.getTime())/1000)) + " sec"
	}

	boolean refreshConfigByFileName(def fileName)
	{
		PatternAutomationHelper paHelper = getParam('paHelper')
		if ( paHelper != null)
		{
			return paHelper.applySequenceByFileName(fileName)
		}
		return false
	}
	
	
}
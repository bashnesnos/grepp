package org.smlt.tools.wgrep.config

import groovy.util.logging.Slf4j;
import org.w3c.dom.Element
/**
 * Base class for wgrep modules. Provides alias methods for used params, shortcut to WgrepConfig to ease initialization and parameter access. <br>
 * Also delegates some basic WgrepConfig methods like param getting and setting. 
 * 
 * @author Alexander Semelit 
 *
 */
@Slf4j
class ModuleBase {

	protected WgrepConfig configInstance
	
	/**
	 * Basic constructor for simple modules, which do not require WgrepConfig instance.
	 */
	
	ModuleBase() {
		log.trace("Creating without config")
	}
	
	/**
	 * Constructor initializing configInstance and containing a validation hook method.
	 * 
	 * @param config_ WgrepConfig instance
	 */
	ModuleBase(WgrepConfig config_) {
		configInstance = config_
		if (!isConfigValid())
		{
			throw new RuntimeException("Supplied params/config is not valid")
		}
	}

	// Getters
	
	/**
	 * Getter for parsed <code>documentElement</code> of the parsed config.xml
	 * @return Value of <code>root</code>
	 */

	Element getRoot()
	{
		return getParam('root')
	}

	/**
	 * Delegates to {@linkplain WgrepConfig.getCDATA}
	 * 
	 * @return String value of element 
	 */

	String getCDATA(Element node)
	{
		return configInstance.getCDATA(node)
	}

	/**
	 * Gets value of the {@link this.extraParams} by key.
	 * @param field Key for <code>extraParams</code> which is needed to be get.
	 * @return Value set to the key <code>field</code>
	 */

	def getParam(String field)
	{
		log.trace("Accessing param: " + field)
		return configInstance.getParam(field)
	}

	// Setters

	/**
	 * Sets value of any field which exists in <code>WgrepFacade</code> via reflection. Is used to propagate value directly from config.xml
	 * @param field Name of field of <code>WgrepFacade</code> which is needed to be set.
	 * @param val Value to be set
	 */

	void setParam(String field, def val)
	{
		log.trace("Setting param: " + field + " val: " + val)
		configInstance.setParam(field, val)
	}

	/**
	 * Sets value of <code>LOG_ENTRY_PATTERN</code> field which exists in <code>WgrepFacade</code> in a classical setter way.
	 * @param val <code>String</code> value to be set
	 */

	void setLogEntryPattern(String val)
	{
		setParam('LOG_ENTRY_PATTERN', val)
	}

	/**
	 * Sets value of <code>FILTER_PATTERN</code> field which exists in <code>WgrepFacade</code> in a classical setter way.
	 * @param val <code>String</code> value to be set
	 */

	void setFilterPattern(String val)
	{
		setParam('FILTER_PATTERN', val)
	}
	
	/**
	 * Gets value of <code>FILTER_PATTERN</code> field which exists in <code>WgrepConfig</code> in a classical getter way.
	 * @param val <code>String</code> value to be set
	 */
	
	String getFilterPattern()
	{
		getParam('FILTER_PATTERN')
	}

	/**
	 * Sets value of <code>SPOOLING_EXT</code> field which exists in <code>WgrepConfig</code> in a classical setter way.
	 * @param val <code>String</code> value to be set
	 */

	void setSpoolingExt(String val)
	{
		setParam('SPOOLING_EXT', val)
	}

	/**
	 * Sets value of <code>FILES</code> field which exists in <code>WgrepConfig</code> in a classical setter way.
	 * @param fileName <code>String</code> filename to be added
	 */
	
	void addFileName(String fileName)
	{
		setParam('FILES', fileName)
	}
	
	/**
	 * Delegates fileName to {@link WgrepConfig.refreshConfigByFileName}
	 * 
	 * @param fileName String representing file path or just name
	 * @return true if config was changed, false otherwise
	 */

	boolean refreshConfigByFileName(String fileName)
	{
		return configInstance.refreshConfigByFileName(fileName)
	}
	
	/**
	 * Basic validation.
	 * 
	 * @return true if configInstance is not null
	 */

	boolean isConfigValid()
	{
		return configInstance != null
	}

}
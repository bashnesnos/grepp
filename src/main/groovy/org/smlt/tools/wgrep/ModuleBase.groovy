package org.smlt.tools.wgrep

import groovy.util.logging.Slf4j;
import org.w3c.dom.Element

@Slf4j
class ModuleBase {

	protected WgrepConfig configInstance

	ModuleBase(WgrepConfig config_) {
		configInstance = config_
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
	 * Getter to extract CDATA element value from a node which is expected to be text.
	 * @return <code>node.text()</code> if the node has text. Value of CDATA element i.e. <code>node.getFirstChild().getNodeValue()</code> otherwise.
	 */

	String getCDATA(Node node)
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
	 * Sets value of <code>FILTER_PATTERN</code> field which exists in <code>WgrepFacade</code> in a classical setter way. If extended pattern processing is enabled it will pre-processed to extract the left-most pattern first.
	 * @param val <code>String</code> value to be set
	 */

	void setFilterPattern(String val)
	{
		setParam('FILTER_PATTERN', val)
	}
	
	String getFilterPattern()
	{
		getParam('FILTER_PATTERN')
	}

	/**
	 * Sets value of <code>SPOOLING_EXT</code> field which exists in <code>WgrepFacade</code> in a classical setter way.
	 * @param val <code>String</code> value to be set
	 */

	void setSpoolingExt(String val)
	{
		setParam('SPOOLING_EXT', val)
	}

	void addFileName(String fileName)
	{
		setParam('FILES', fileName)
	}

	boolean refreshConfigByFileName(String fileName)
	{
		return configInstance.refreshConfigByFileName(fileName)
	}

}
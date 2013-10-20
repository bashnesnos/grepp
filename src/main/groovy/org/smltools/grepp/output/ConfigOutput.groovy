package org.smltools.grepp.output;

import groovy.util.logging.Slf4j

import org.smltools.grepp.config.ParamsHolder
import org.smltools.grepp.config.Param
import org.smltools.grepp.filters.enums.Event
import org.smltools.grepp.filters.FilterBase
import org.smltools.grepp.filters.FilterChainFactory;
import org.w3c.dom.Element
import groovy.xml.DOMBuilder
import groovy.xml.dom.DOMCategory
import groovy.xml.XmlUtil

/**
 * 
 * Output writing directly to config.xml
 * 
 * @author Alexander Semelit
 *
 */

@Slf4j
final class ConfigOutput extends SimpleOutput {
	
	private String configFilePath;
	private def cfgDoc
	private def root
	private DOMBuilder domBuilder
	
	public ConfigOutput(ParamsHolder paramsHolder) 
	{
    	super(paramsHolder, null)
    	domBuilder = DOMBuilder.newInstance(false, true) 
    	configFilePath = paramsHolder.get(Param.CONFIG_FILE_PATH)
    	if (configFilePath != null) {
    		cfgDoc = DOMBuilder.parse(new FileReader(configFilePath))
        	root = cfgDoc.documentElement
    	}
    	else throw new IllegalArgumentException("Config file is null")

   	}


	@Override
	protected void printNotFiltered(Object data) {
		if (data instanceof StringBuilder || data instanceof String)
		{
			def customConfig = domBuilder.parseText(data.toString()).documentElement
			def configId = ""
	        use(DOMCategory) {
	        	configId = customConfig.'@id'
	        	def existing = root.custom.config.find { it.'@id' == configId }
	        	if (existing == null) {
	        		def importedNode = cfgDoc.importNode(customConfig, true)
	        		if (root.custom[0] == null) { //creating custom element
	        			def custom = cfgDoc.createElement("custom")
	        			root.appendChild(custom)
	        		}
					root.custom[0].insertBefore(importedNode, root.custom.config[0])
					cfgDoc.normalizeDocument()
					XmlUtil.serialize(root, new FileWriter(configFilePath))
	        	}
	        	else {
	        		log.debug("Config exists")
	        	}
	       	}
			log.debug("Config {} parsed", configId)			
		}
		else {
			log.debug("No custom config found")
		}
	}

}
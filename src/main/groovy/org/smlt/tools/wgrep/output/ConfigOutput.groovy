package org.smlt.tools.wgrep.output;

import groovy.util.logging.Slf4j
import org.smlt.tools.wgrep.filters.enums.Event
import org.smlt.tools.wgrep.filters.FilterChainFactory
import org.smlt.tools.wgrep.filters.FilterBase
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.w3c.dom.Element


/**
 * 
 * Output writing directly to config.xml
 * 
 * @author Alexander Semelit
 *
 */

@Slf4j
final class ConfigOutput implements WgrepOutput<Element, Object>, BeanFactoryAware  {
	
	private BeanFactory beanFactory;
	private File configFile;
	private def cfgDoc
	private def root;
	
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException 
	{
    	this.beanFactory = beanFactory;
    	configFile = beanFactory.getBean("configFactory").getConfigFile().getFile()
    	cfgDoc = DOMBuilder.parse(new FileReader(configFile))
        root = cfgDoc.documentElement
   	}
	/**
	 * Refreshes filters/filtering params by some criteria.
	 * 
	 * @param criteria Something that can be used for config refreshing. Filename for example
	 */
	void refreshFilters(Object criteria) {
		throw new UnsupportedOperationException()
	}
	
	/**
	 * Ensures that event is correctly printed to output. Nothing more or less.
	 * 
	 * @param event Event to be printed
	 */
	void processEvent(Event event) {
		throw new UnsupportedOperationException()
	}
	
	/**
	 * Ensures that data is printed to output. Nothing more or less.
	 * 
	 * @param data Data to be printed
	 */
	void printToOutput(Element data) {
	        use(DOMCategory) {
	        	root.custom.appendChild(data)
        	}
			cfgDoc.normalizeDocument()
			XmlUtil.serialize(data, new FileWriter(configFile))			
	}
	
	/**
	 * 
	 * Releases output resources
	 * 
	 */
	void closeOutput() {

	}
	
}
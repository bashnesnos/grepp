package org.smltools.grepp.config

import groovy.xml.dom.DOMCategory;
import groovy.util.logging.Slf4j

import org.smltools.grepp.util.GreppUtil;
import org.w3c.dom.Document
import org.w3c.dom.Element

import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory
import javax.xml.validation.Validator

import groovy.xml.DOMBuilder

@Slf4j
public class ConfigHolder {

	//internal
	private Validator configValidator;
	private String configFilePath;
	private String configXSDPath;
	private Document cfgDoc;
	private Element root;

	/**
	 * Constructor <br>
	 * Initializes the instance. Parses config.xml and loads defaults from there.
	 *
	 * @param configFilePath String which can be recognized by a <code>FileReader</code> and is a valid path to an config.xml file
	 */
	ConfigHolder(String pConfigFilePath)
	{
		this(pConfigFilePath, null)
	}
	
	
	ConfigHolder(String pConfigFilePath, String pConfigXSDPath)
	{
		loadConfigInternal(pConfigFilePath, pConfigXSDPath)
	}

	public Validator getCofigValidator() {
		return configValidator;
	}
	
	public String getConfigFilePath() {
		return configFilePath;
	}
	
	public String getConfigXSDPath() {
		return configXSDPath;
	}

	public Document getCfgDoc() {
		return cfgDoc;
	}
	
	public Element getRoot() {
		return root;
	}
	
	public Object withRoot(Closure closure) {
		use(DOMCategory) {
			return closure.call(root);
		}
	}

	public Class<?> getParamsHolderFactoryClass() {
		withRoot { root ->
			String className = root.global.params_factory.text()
			if (className != null) {
				return GreppUtil.getClassByName(className);
			}
			else {
				throw new AssertionError("Should be validated by xsd")
			}
		}
	}
			
	private void loadConfigInternal(String pConfigFilePath, String pConfigXSDPath) {
		if (pConfigXSDPath == null || validateConfigFile(pConfigFilePath, pConfigXSDPath)) {
			initConfig(pConfigFilePath)
		}
		else {
			throw new RuntimeException("config.xml is invalid")
		}
	}
	
	private boolean validateConfigFile(String pCconfigFilePath, String pConfigXSDPath) {
		log.trace("Loading validator")
		def factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
		def schema = factory.newSchema(new StreamSource(new FileReader(pConfigXSDPath)))
		configXSDPath = pConfigXSDPath
		configValidator = schema.newValidator()
		log.trace("Validating the config")
		configValidator.validate(new StreamSource(new FileReader(pCconfigFilePath)))
		return true
	}
	
	private void initConfig(String pConfigFilePath) {
		configFilePath = pConfigFilePath
		cfgDoc = DOMBuilder.parse(new FileReader(configFilePath))
		root = cfgDoc.documentElement
	}
	
}

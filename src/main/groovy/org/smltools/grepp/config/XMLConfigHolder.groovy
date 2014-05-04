package org.smltools.grepp.config

import groovy.xml.dom.DOMCategory
import groovy.util.logging.Slf4j
import org.w3c.dom.Document
import org.w3c.dom.Element
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory
import javax.xml.validation.Validator
import groovy.xml.DOMBuilder
import groovy.util.ConfigObject

@Slf4j
public class XMLConfigHolder extends ConfigHolder {

    //internal
    private String configFilePath;
    private Document cfgDoc;
    private DOMBuilder domBuilder = DOMBuilder.newInstance(false, true) 

    /**
     * Constructor <br>
     * Initializes the instance. Parses config.xml and loads defaults from there.
     *
     * @param configFilePath String which can be recognized by a <code>FileReader</code> and is a valid path to an config.xml file
     */
    public XMLConfigHolder(String pConfigFilePath)
    {
        this(pConfigFilePath, null)
    }
    
    
    public XMLConfigHolder(String pConfigFilePath, String pConfigXSDPath)
    {
        loadConfigInternal(pConfigFilePath, pConfigXSDPath)
    }

    @Override
    public void addAndSave(ConfigObject newSubConfig) {
        this.merge(newSubConfig)        
        serializeConfig()
    }

    private void serializeConfig() {
        //serialize the whole this to proper XML here

//        def writer = new StringWriter()
//        def xml = new MarkupBuilder(writer)
//        
        //if (idVal == null || idVal == "") //pointless to create if config doesn't have an identifier
//            return null
//
//        xml.config("id":idVal, 'xmlns':"http://www.smltools.org/config") {
//            if (dateFormatVal != null)
//                date_format(dateFormatVal)
//            if (dateVal != null)
//                date("(" + dateVal + ")")
//            if (starterVal != null && starterVal != dateVal)
//                starter(starterVal + ".*")
//            if (logThresholdVal != 0)
//                log_threshold(logThresholdVal)
//            if (patternVal != null)
//                pattern(patternVal)
//        }
//        def data = writer.toString()                        
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
        def configValidator = schema.newValidator()
        log.trace("Validating the config")
        configValidator.validate(new StreamSource(new FileReader(pCconfigFilePath)))
        return true
    }
    
    private void initConfig(String configFilePath) {
        this.configFilePath = configFilePath
        this.cfgDoc = DOMBuilder.parse(new FileReader(configFilePath))
        def root = this.cfgDoc.documentElement

        parseDefaults(root)        
        parseLogDateFormats(root)
        parseCustomConfigs(root)
        parseFilterAliases(root)
        parsePostFilterParams(root)
    }

    private void parseDefaults(Element root) {
        use(DOMCategory) {
            this.defaults.spoolFileExtension = root.defaults.spoolFileExtension[0].text()
            this.defaults.resultsDir = root.defaults.resultsDir[0].text()
        }
    }

    private void parseLogDateFormats(Element root) {
        def logDateFormats = this.logDateFormats
        use(DOMCategory) {
            root.logDateFormats.dateFormat.each { logDateFormat ->
                def formatId = logDateFormat.'@id'
                //log.trace('Parsing logDateFormat {}', formatId)                
                def logDateFormatPropMap = logDateFormats."$formatId"
                logDateFormatPropMap.value = logDateFormat.value[0].text()
                logDateFormatPropMap.regex = logDateFormat.regex[0].text()
            }
        }
    }

    /**
     * Parses <config> sections. Depending on which elements are supplied it fills: <br>
     * LOG_ENTRY_PATTERN <br>
     * LOG_DATE_PATTERN <br>
     * LOG_DATE_FORMAT <br>
     * LOG_FILE_THRESHOLD <br>
     * If custom config was found and params were filled, toggles isAmmended flag.
     * 
     */
    void parseCustomConfigs(Element root) {
        def savedConfigs = this.savedConfigs
        use(DOMCategory) { 
            root.config.each { customCfg ->
                def configId = customCfg.'@id'
                def newCustomConfig = this.savedConfigs."$configId"
                
                //log.trace("Parsing entry config for {}", configId)

                def starter = customCfg.starter[0]
                if (starter != null) {
                    newCustomConfig.starter = starter.text()
                }
                def date = customCfg.logDateFormat[0]
                if (date != null) {
                    def dateFormatId = date.text()
                    if (this.logDateFormats.containsKey(dateFormatId)) {
                        newCustomConfig.dateFormat = this.logDateFormats."$dateFormatId"//get logDateFormat by id    
                    }
                    else {
                        newCustomConfig.dateFormat.value = date.value[0].text()
                        newCustomConfig.dateFormat.regex = date.regex[0].text()
                    }
                    
                }
                def thrshld = customCfg.logThreshold.text()
                if (thrshld != null && thrshld != "") {
                    newCustomConfig.logThreshold = Integer.valueOf(thrshld)
                }
            }
        }
    }

    /**
     * Looks for <filter> element with "tags" parameter containing supplied tag. Method fills: <br>
     * FILTER_PATTERN
     * 
     * @param tag One of "tags", which could be found in <filter> element.
     */

    void parseFilterAliases(Element root) {
        def filterAliases = this.filterAliases
        use(DOMCategory) {
            root.filterAliases.filter.each { filter ->
                def tagList = filter.'@tags'.tokenize(",")
                def firstTag = tagList.remove(0)
                filterAliases."$firstTag" = filter.text()
                tagList.each { anotherTag ->
                    filterAliases."$anotherTag" = filterAliases."$firstTag"
                }
            }    
        }
    }

    /**
     * Parses postProcessColumns configuration from config.xml 
     * 
     *
     */
    void parsePostFilterParams(Element root) {

        def postProcessColumns = this.postProcessColumns
        def postProcessSeparators = this.postProcessSeparators

        use(DOMCategory) {
            root.postProcessColumns.splitter.each { splitter ->
                def tagList = splitter.'@tags'.tokenize(",")
                def firstTag = tagList.remove(0)
                def postProcessPropMap = postProcessColumns."$firstTag"
                def postProcessTypePropMap = postProcessPropMap."${splitter.'@type'}"
                postProcessTypePropMap.order = Integer.valueOf(splitter.'@order')
                postProcessTypePropMap.colName = splitter.'@col_name'
                postProcessTypePropMap.value = splitter.text()
                tagList.each { anotherTag ->
                    postProcessColumns."$anotherTag" = postProcessPropMap
                }
            }

            root.postProcessSeparators.separator.each { separator ->
                def postProcessSeparatorPropMap = postProcessSeparators."${separator.'@id'}"
                postProcessSeparatorPropMap.value = separator.value[0].text()
                postProcessSeparatorPropMap.spoolFileExtension = separator.spoolFileExtension[0].text()
            }

        }
    }

    /**
     * 
     * Parses appropriate ComplexFilter params from config.xml
     * 
     */
    void parseComplexFilterParams(Element root) {
        def processThreads = this.processThreads
        use(DOMCategory) {
            root.processThreads.extractors.each { extractor ->
                def tagList = extractor.'@tags'.tokenize(",")
                def firstTag = tagList.remove(0)
                def processThreadsPropMap = processThreads."$firstTag"
                if (processThreadsPropMap.extractors instanceof List<?>) {
                    processThreadsPropMap.extractors.add(extractor.text())
                }
                else {
                    processThreadsPropMap.extractors = [extractor.text()]
                }
                tagList.each { anotherTag ->
                    processThreads."$anotherTag".extractors = processThreadsPropMap.extractors
                }
            }
            
            root.processThreads.skipends.each { skipend ->
                def tagList = skipend.'@tags'.tokenize(",")
                def firstTag = tagList.remove(0)
                def processThreadsPropMap = processThreads."$firstTag"
                if (processThreadsPropMap.skipends instanceof List<?>) {
                    processThreadsPropMap.skipends.add(skipend.text())
                }
                else {
                    processThreadsPropMap.skipends = [skipend.text()]
                }
                tagList.each { anotherTag ->
                    processThreads."$anotherTag".skipends = processThreadsPropMap.skipends
                }
            }

            root.processThreads.ends.each { end ->
                def tagList = end.'@tags'.tokenize(",")
                def firstTag = tagList.remove(0)
                def processThreadsPropMap = processThreads."$firstTag"
                if (processThreadsPropMap.ends instanceof List<?>) {
                    processThreadsPropMap.ends.add(end.text())
                }
                else {
                    processThreadsPropMap.ends = [end.text()]
                }
                tagList.each { anotherTag ->
                    processThreads."$anotherTag".ends = processThreadsPropMap.ends
                }
            }
        }
    }

}
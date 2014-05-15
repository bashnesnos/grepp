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
import groovy.xml.MarkupBuilder
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
        if (pConfigFilePath == null) throw new IllegalArgumentException("configFilePath shouldn't be null")
        loadConfigInternal(pConfigFilePath, pConfigXSDPath)
    }

    @Override
    void backupConfigFile() {
        new File(configFilePath).renameTo(new File(configFilePath.replace("xml", "bak${String.format('%tY%<tm%<td', new Date())}")))
    }

    @Override
    void writeToConfigFile() {
        //serialize the whole this to proper XML here
        def writer = new File(configFilePath).newWriter()
        def xml = new MarkupBuilder(writer)
        def extractorsSet = this.processThreads.collect { it.value.extractors }.flatten().toSet()
        def skipendsSet = this.processThreads.findAll { it.value.skipends instanceof List }.collect { it.value.skipends }.flatten().toSet() //since skipends are optional
        def endsSet = this.processThreads.collect { it.value.ends }.flatten().toSet()
        def filterSet = this.filterAliases.collect { it.value }.flatten().toSet()
        xml.mkp.xmlDeclaration(version:'1.0', encoding:'UTF-8')
        xml.root('xmlns':"http://www.smltools.org/config",
            'xmlns:xsi':"http://www.w3.org/2001/XMLSchema-instance",
            'xsi:schemaLocation':"http://www.smltools.org/config this.xsd") {
            defaults {
                spoolFileExtension(this.defaults.spoolFileExtension)
                resultsDir(this.defaults.resultsDir)
                postProcessSeparator(this.postProcessSeparators.findResult { id, props -> if (this.defaults.postProcessSeparator.value.equals(props.value)) return id})
            }
            postProcessSeparators {
                this.postProcessSeparators.each { id, props ->
                    separator('id':id, ) {
                        spoolFileExtension(props.spoolFileExtension)
                        value(props.value)
                    }
                }
            }
            logDateFormats {
                this.logDateFormats.each { id, props ->
                    dateFormat('id':id, ) {
                        value(props.value)
                        regex(props.regex)
                    }
                }
            }
            this.savedConfigs.each {id, props ->
                config('id':id, ) {
                    if (props.containsKey('dateFormat')) {
                        logDateFormat(this.logDateFormats.findResult { ldId, ldProps -> if (props.dateFormat.regex.equals(ldProps.regex)) return ldId})
                    }

                    if (props.containsKey('starter')) {
                        starter(props.starter)
                    }

                    if (props.containsKey('logThreshold')) {
                        logThreshold(props.logThreshold)
                    }

                    if (props.containsKey('pattern')) {
                        pattern(props.pattern)
                    }
                }
            }
            processThreads {
                extractors {
                    extractorsSet.each { extractor ->
                        pattern('tags':this.processThreads.findResults { if (it.value.extractors.contains(extractor)) return it.key }.join(','), extractor)
                    }
                }
                skipends {
                    skipendsSet.each { skipend ->
                        pattern('tags':this.processThreads.findResults { if (it.value.containsKey('skipends') && it.value.skipends instanceof List && it.value.skipends.contains(skipend)) return it.key }.join(','), skipend)
                    }
                }
                ends {
                    endsSet.each { end ->
                        pattern('tags':this.processThreads.findResults { if (it.value.ends.contains(end)) return it.key }.join(','), end)
                    }
                }
            }
            postProcessColumns {
                this.postProcessColumns.each {id, types ->
                    types.each { type, props ->
                        if (!'postProcessSeparator'.equals(id)) {
                            splitter('col_name':props.colName, 'order':props.order, 'tags':id, 'type':type, props.value)
                        }
                    }
                }
            }
            filterAliases {
                filterSet.each { filterVal ->
                    filter('tags':this.filterAliases.findResults { if (it.value.equals(filterVal)) return it.key }.join(','), filterVal)
                }
            }
        }
        writer.close()       
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

        parseLogDateFormats(root)
        parseCustomConfigs(root)
        parseComplexFilterParams(root)
        parseFilterAliases(root)
        parsePostFilterParams(root)
        parseDefaults(root)
    }

    private void parseDefaults(Element root) {
        use(DOMCategory) {
            def defaultProp = root.defaults.spoolFileExtension[0]
            if (defaultProp != null) {
                this.defaults.spoolFileExtension = defaultProp.text()
            }

            defaultProp = root.defaults.resultsDir[0]
            if (defaultProp != null) {
                this.defaults.resultsDir = defaultProp.text()
            }
            
            defaultProp = root.defaults.spoolFileExtension[0]
            if (defaultProp != null) {
                def postProcessSeparatorId = defaultProp.text()
                if (this.postProcessSeparators.containsKey(postProcessSeparatorId)) {
                    this.defaults.postProcessSeparator = this.postProcessSeparators."$postProcessSeparatorId"
                }
                else {
                    log.warn("Given default postProcessSeparator {} doesn't exist", postProcessSeparatorId)
                }
            }
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

                def pattern = customCfg.pattern[0]
                if (pattern != null) {
                    newCustomConfig.pattern = pattern.text()
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
            root.processThreads.extractors.pattern.each { extractor ->
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
            
            root.processThreads.skipends.pattern.each { skipend ->
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

            root.processThreads.ends.pattern.each { end ->
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
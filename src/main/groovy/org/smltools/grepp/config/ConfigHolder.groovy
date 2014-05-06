package org.smltools.grepp.config;

import groovy.util.ConfigObject
import java.io.IOException
import java.io.Writer
import java.net.URL
import java.net.URI
import java.util.Collection
import java.util.Map
import java.util.Properties
import java.util.Set
import groovy.util.ConfigSlurper
import java.util.regex.Pattern
import org.smltools.grepp.util.GreppUtil

/** 
 * 
 * As much immutable version of ConfigObject as possible to represent Grepp config file.
 * Default representation is a .groovy file
 *
 * @author asemelit
 */

public class ConfigHolder extends ConfigObject {
    private URL configFilePath

    public void mergeAndSave(ConfigObject newSubConfig) {
    	backupConfigFile()
    	this.merge(newSubConfig)
    	newSubConfig.each {id, props ->
	    	this.savedConfigs."$id" = props
	    	if (props.containsKey('dateFormat')) {
	    		this.logDateFormats."$id" = props.dateFormat
	    	}
    	}
    	writeToConfigFile()
    }

    public ConfigHolder() {
    	loadDefaults()
    }

    public ConfigHolder(URL configFilePath) {
        if (configFilePath == null) throw new IllegalArgumentException("configFilePath shouldn't be null")
        
        this.configFilePath = configFilePath
        this.merge(new ConfigSlurper().parse(configFilePath))
    }

    void loadDefaults() {
        this.defaults.spoolFileExtension = '.log'
        this.defaults.resultsDir = 'results'
        this.defaults.postProcessSeparator.value = ','
        this.defaults.postProcessSeparator.spoolFileExtension = '.csv'
    }

    void backupConfigFile() {
        if (configFilePath != null) {
            new File(configFilePath.toURI()).renameTo(new File(new URI(configFilePath.toString().replace("groovy", "bak${String.format('%tY%<tm%<td', new Date())}"))))
        }
    }

    void writeToConfigFile() {
        if (configFilePath != null) {
            def configFile = new File(configFilePath.toURI())
            def writer = new StringWriter()
            this.writeTo(writer)
            configFile.write(GreppUtil.escapeRegexes(writer.toString()))
        }
    }
}

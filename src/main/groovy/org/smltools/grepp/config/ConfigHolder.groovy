package org.smltools.grepp.config;

import groovy.util.ConfigObject;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/** 
 * 
 * As much immutable version of ConfigObject as possible to represent Grepp config file
 * 
 * @author asemelit
 */

public abstract class ConfigHolder extends ConfigObject {

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

    void loadDefaults() {
        this.defaults.spoolFileExtension = '.log'
        this.defaults.resultsDir = 'results'
        this.defaults.postProcessSeparator.value = ','
        this.defaults.postProcessSeparator.spoolFileExtension = '.csv'
    }

    abstract void backupConfigFile();
    abstract void writeToConfigFile();
}

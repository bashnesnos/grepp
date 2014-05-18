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
    public final static String SAVED_CONFIG_KEY = "savedConfigs";
    public final static String SAVED_CONFIG_FILENAME_PATTERN_KEY = "pattern";
    public final static String SAVED_CONFIG_STARTER_KEY = "starter";
    public final static String SAVED_CONFIG_DATE_FORMAT_KEY = "dateFormat";
    public final static String SAVED_CONFIG_DATE_FORMAT_REGEX_KEY = "regex";
    public final static String SAVED_CONFIG_DATE_FORMAT_VALUE_KEY = "value";
    public final static String SAVED_CONFIG_LOG_THRESHOLD_KEY = "logThreshold";

    /**
     * Finds config id by specified String. Method looks up for <config> element containing matching <pattern> with "alevel" parameter equal to level.
     * 
     * @param config
     * @param fileName String which would be matched to 'pattern' property of a savedConfig
     * @return
     */
    @SuppressWarnings("unchecked")
    public static String findConfigIdByFileName(ConfigHolder config, String fileName) {
            if (config == null || fileName == null) {
                    throw new IllegalArgumentException("Both config and fileName shouldn't be null");
            }

            return config.savedConfigs.findResult { configId, props ->
                    if (props.containsKey(SAVED_CONFIG_FILENAME_PATTERN_KEY)) {
                            String currentConfigPtrn = props.pattern
                            if (fileName =~ currentConfigPtrn) {
                                return configId;
                            }
                    }
            }
    } 
    
    private URL configFilePath

    public void save() {
    	backupConfigFile()
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
        this.defaults.spoolFileExtension = 'txt'
        this.defaults.resultsDir = 'results'
        this.defaults.reportSeparator.value = ','
        this.defaults.reportSeparator.spoolFileExtension = 'csv'
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

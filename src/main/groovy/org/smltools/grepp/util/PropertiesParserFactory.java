package org.smltools.grepp.util;

import static org.smltools.grepp.Constants.*;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PropertiesParserFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesParserFactory.class);

	public static final String GREPP_PROPERTIES_PARSER_PLUGIN_DIR = "/plugin/propertiesParsers";

    private static final Map<String, Class<? extends PropertiesParser>> ID_TO_PARSER_CLASS_MAP = new HashMap<String, Class<? extends PropertiesParser>>();

    static {
    	try {
    		addParserClassByName("org.smltools.grepp.filters.entry.PropertiesFilter");
        }
        catch (ClassNotFoundException cnfe) {
    		throw new RuntimeException(cnfe);
    	}

        if (System.getProperty(GREPP_HOME_SYSTEM_OPTION) != null) {
            File pluginDir = new File(System.getProperty(GREPP_HOME_SYSTEM_OPTION), GREPP_PROPERTIES_PARSER_PLUGIN_DIR);
            if (pluginDir.exists() && pluginDir.isDirectory()) {
                LOGGER.trace("Plugin dir {} exists; plugging in PropertiesParsers enabled", GREPP_PROPERTIES_PARSER_PLUGIN_DIR);
                for (File pluginFile: pluginDir.listFiles()) {
                    LOGGER.trace("Found file: {}", pluginFile.getName());
                    Class<?> pluginClass = GreppUtil.loadGroovyClass(pluginFile);
                    if (pluginClass != null && PropertiesParser.class.isAssignableFrom(pluginClass)) {
						@SuppressWarnings("unchecked")
                    	Class<? extends PropertiesParser> parserClass = (Class<? extends PropertiesParser>) pluginClass;
                        addIdToParserClassMapping(null, parserClass);
                    }
                    else {
                        LOGGER.error("{} was ignored class: {}", pluginFile.getName(), pluginClass);
                    }
                }
            }
            else {
                LOGGER.trace("Plugin dir {} doesn't exist; i.e. disabled", GREPP_PROPERTIES_PARSER_PLUGIN_DIR);
            }
        }    
    }

    @SuppressWarnings("unchecked")
    public static void addParserClassByName(String name) throws ClassNotFoundException {
    	Class<?> clazz = Class.forName(name);
    	if (PropertiesParser.class.isAssignableFrom(clazz)) {
    		addIdToParserClassMapping(null, (Class<? extends PropertiesParser>) clazz);
    	}
    	else {
    		throw new IllegalArgumentException(name + " is not an implementation of Filter");
    	}

    }

    private static void addIdToParserClassMapping(String parserId, Class<? extends PropertiesParser> parserClass) {
        if (parserId == null) {
            PropertiesParserParams parserParams = parserClass.getAnnotation(PropertiesParserParams.class);
            if (parserParams != null) {
                parserId = parserParams.id();
            }
            else {
                throw new IllegalArgumentException("Either parserId shouldn't be null, or " + parserClass.getName() + " should be annotated with PropertiesParserParams");
            }
        }
        if (!ID_TO_PARSER_CLASS_MAP.containsKey(parserId)) {
            ID_TO_PARSER_CLASS_MAP.put(parserId, parserClass);
        }
        else {
            throw new IllegalArgumentException("Parser id " + parserId + " already registered!");
        }
    }

	public static PropertiesParser getParserInstanceById(String parserId) {
		Class<? extends PropertiesParser> parserClass = ID_TO_PARSER_CLASS_MAP.get(parserId);
		if (parserClass != null) {
			try {
				return parserClass.newInstance();
			}
		    catch (InstantiationException ie) {
		    	throw new RuntimeException("A PropertiesParser implementation supposed to have a no-argument constructor!", ie);
		    }
		    catch (IllegalAccessException iae) {
				throw new RuntimeException("A PropertiesParser implementation supposed to have a public no-argument constructor!", iae);	    	
		    }
		}
		else {
			throw new IllegalArgumentException("Unknown parser id: " + parserId);
		}
	}

	private PropertiesParserFactory() {
		throw new AssertionError("Why would you instantiate this?");
	}

}
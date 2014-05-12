package org.smltools.grepp.util;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smltools.grepp.config.ConfigHolder;

/**
 * 
 * Some common methods.
 * 
 * @author Alexander Semelit
 *
 */
public final class GreppUtil {
        private static final Logger LOGGER = LoggerFactory.getLogger(GreppUtil.class);
        
	private GreppUtil() { throw new AssertionError(); } //please don't instantiate the class

	/**
	 * Finds config id by specified String. Method looks up for <config> element containing matching <pattern> with "alevel" parameter equal to level.
	 * 
         * @param config
	 * @param fileName String which would be matched to 'pattern' property of a savedConfig
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static String findConfigIdByFileName(Map<?, ?> config, String fileName) {
		if (config == null || fileName == null) {
			throw new IllegalArgumentException("Both config and fileName shouldn't be null");
		}

		for (Map.Entry<String, Map<String,?>> savedConfig: ((Map<String, Map<String,?>>) config.get(ConfigHolder.SAVED_CONFIG_KEY)).entrySet()) { 
			String newConfigId = savedConfig.getKey();
                        Map<String, ?> props = savedConfig.getValue();
                        LOGGER.trace("id: {}; props: {}", newConfigId, props);
                        if (props.containsKey(ConfigHolder.SAVED_CONFIG_FILENAME_PATTERN_KEY)) {
                                String currentConfigPtrn = (String) props.get(ConfigHolder.SAVED_CONFIG_FILENAME_PATTERN_KEY);
                                LOGGER.trace("ptrn=/{}/ fileName='{}'", currentConfigPtrn, fileName);
                                if (Pattern.compile(currentConfigPtrn).matcher(fileName).find()) {
                                        return newConfigId;
                                }
                        }
		}

		return null;
	}

	/**
	 * Kind-of a default value method for a Map.get()
	 * 
	 * @param params a Map which will be the source
	 * @param key a Key which will be used to get an object from the params
	 * @param returnIfNull an Object which will be returned if there is nothing in params corresponding to the key
	 * @return
	 */
	public static <K,V> V getNotNull(Map<K, V> params, K key, V returnIfNull)
	{
		V result = params.get(key);
		return result != null ? result : returnIfNull;
	}

	/**
	 * Simply throws IllegalArgumentException with a given message if a given Object is null
	 * 
	 * @param o Object to be checked for null
	 * @param message which will be passed to an exception
	 */
	public static void throwIllegalAEifNull(Object o, String message)
	{
		if (o == null) throw new IllegalArgumentException(message);
	}

	/**
	 * Looks in the class-path for a resource by name
	 * 
	 * @param name Resource name
	 * @return null or String representing path to a resource
	 */
	public static URL getResourceOrNull(String name) {
		URL resourceURL = GreppUtil.class.getClassLoader().getResource(name);
		if (resourceURL != null)
		{
			return resourceURL;
		}
		else {
			return null;
		}
	}
	
	public static Class<?> getClassByName(String className) throws ClassNotFoundException {
		if (className != null) {
			return GreppUtil.class.getClassLoader().loadClass(className);
		}
		else {
			throw new IllegalArgumentException("Class name shouldn't be null");
		}
	}

	/**
	 * Checks if a given Class has a given field as it's own member or has inherited it from a super class
	 * 
	 * @param clazz Class object
	 * @param field name of a field to check
	 * @return true if it has such a field or inherited it
	 */
	public static boolean hasField(Class<?> clazz, String field) {
		if (!hasField(clazz, field, false)) {
			return hasField(clazz.getSuperclass(), field, true);
		}
		else {
			return true;
		}
	}

	private static boolean hasField(Class<?> clazz, String field, boolean isSuper) {
		try {
			if (!isSuper) {
				clazz.getDeclaredField(field);	
				return true;
			}
			else {
				int modifiers = clazz.getDeclaredField(field).getModifiers();
				return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
			}
		}
		catch (NoSuchFieldException e) {
			return false;
		}
	}

	/**
	 * Reloads current wgrep looging config as of logback.xml with a given String
	 * 
	 * @param logbackConfig XML with logback configuration
	 */
	public static void resetLogging(String logbackConfig) {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

		try {
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(context);
			// Call context.reset() to clear any previous configuration, e.g. default
			// configuration. For multi-step configuration, omit calling context.reset().
			context.reset();
			configurator.doConfigure(new ByteArrayInputStream(logbackConfig.getBytes()));
		} catch (JoranException je) {
			// StatusPrinter will handle this
		}
		StatusPrinter.printInCaseOfErrorsOrWarnings(context);
	}

	public static String escapeRegexes(String stringWithRegex) {
		return stringWithRegex.replaceAll("(\\\\)(?!['])", "$1$1");
	}

}

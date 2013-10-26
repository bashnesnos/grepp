package org.smltools.grepp.util;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.net.URL;
import org.slf4j.LoggerFactory;
import org.smltools.grepp.util.GreppUtil;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import java.lang.reflect.Modifier;

/**
 * 
 * Some common methods.
 * 
 * @author Alexander Semelit
 *
 */
public final class GreppUtil {

	private GreppUtil() { throw new AssertionError(); } //please don't instantiate the class


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
	public static String getResourcePathOrNull(String name) {
		URL resourceURL = GreppUtil.class.getClassLoader().getResource(name);
		if (resourceURL != null)
		{
			return resourceURL.getPath();
		}
		else {
			return null;
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

}

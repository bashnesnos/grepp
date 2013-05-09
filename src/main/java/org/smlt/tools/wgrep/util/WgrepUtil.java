package org.smlt.tools.wgrep.util;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.net.URL;
import org.w3c.dom.Element;
import org.slf4j.LoggerFactory;
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
public class WgrepUtil {
	
	public static String getCDATA(Element node)
	{
		if (node == null) return null;
		String txt = node.getTextContent();
		return (txt != null) ? txt : node.getFirstChild().getNodeValue();
	}

	public static <K,V> V getNotNull(Map<K, V> params, K key, V returnIfNull)
	{
		V result = params.get(key);
		return result != null ? result : returnIfNull;
	}

	public static void throwIllegalAEifNull(Object o, String message)
	{
		if (o == null) throw new IllegalArgumentException(message);
	}

	public static String getResourcePathOrNull(String name) {
		URL resourceURL = WgrepUtil.class.getClassLoader().getResource(name);
		if (resourceURL != null)
		{
			return resourceURL.getPath();
		}
		else {
			return null;
		}
	}

	public static boolean hasField(Class<?> clazz, String field) {
		if (!hasField(clazz, field, false)) {
			return hasField(clazz.getSuperclass(), field, true);
		}
		else {
			return true;
		}
	}

	public static boolean hasField(Class<?> clazz, String field, boolean isSuper) {
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

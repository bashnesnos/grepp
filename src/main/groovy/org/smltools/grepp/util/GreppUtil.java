package org.smltools.grepp.util;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.ParameterizedType;
import groovy.lang.GroovyClassLoader;
import java.io.File;
import java.io.IOException;
import org.codehaus.groovy.control.CompilationFailedException;


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

	@SuppressWarnings("unchecked")
	public static Class<?> loadGroovyClass(File groovyFile) {
		try {
			GroovyClassLoader gcl = new GroovyClassLoader();
			return gcl.parseClass(groovyFile);
		}
		catch (IOException ioe) {
			LOGGER.error("Can't open groovy file;\n", ioe);
		}
		catch (CompilationFailedException cfe) {
			LOGGER.error("Can't compile groovy plugin;\n", cfe);	
		}
		
		return null;
	}

	@SuppressWarnings("unchecked")
	public static Class<?> findConcreteParameterClass(ParameterizedType type) {
		for (Type typeArgument: type.getActualTypeArguments()) {
			if (typeArgument instanceof Class<?> && !Object.class.equals(typeArgument)) {
				return (Class<?>) typeArgument;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static Class<?> findSuperclassParameter(Class<?> clazz) {
		Type superclazz = clazz.getGenericSuperclass();
		if (superclazz != null) {
			if (superclazz instanceof ParameterizedType) {
				LOGGER.debug("findSuperclassParameter: {}", superclazz);
				return findConcreteParameterClass((ParameterizedType) superclazz);
			}
			else {
				return findSuperclassParameter((Class<?>) superclazz);
			}
		}
		else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static Class<?> findParameterClass(Class<?> clazz) {
		for (TypeVariable typeVar: clazz.getTypeParameters()) {
			for (Type type: typeVar.getBounds()) {
				if (type instanceof Class<?> && !Object.class.equals(type)) {
					LOGGER.debug("{} findParameterClass: itself", clazz.getName());
					return (Class<?>) type;
				}
			}
		}

		ParameterizedType parametrizedInterface = null;
		for (Type interfase: clazz.getGenericInterfaces()) {
			if (interfase instanceof ParameterizedType) {
				Class<?> concreteClass = findConcreteParameterClass((ParameterizedType) interfase);
				if (concreteClass != null) {
					LOGGER.debug("{} findParameterClass: interface {}", clazz.getName(), interfase);
					return concreteClass;
				}
			}
		}

		LOGGER.debug("findParameterClass: super");
		return findSuperclassParameter(clazz);
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
	public static void throwIllegalAEifNull(String msg, Object... objects) {
		StringBuilder message = new StringBuilder(msg).append(" ");

		boolean isAllNotNull = true;
		for (Object obj: objects) {
			isAllNotNull &= obj != null;
			message.append(isAllNotNull).append(";");
			if (!isAllNotNull) throw new IllegalArgumentException(message.toString());
		}
	}

	public static void throwIllegalAEifNull(Object o, String message) {
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
		return stringWithRegex.replaceAll("(\\\\)(?!['])", "$1$1"); //escapes unescaped, since ConfigObject doesn't hadnle those cases pretty well...
	}

}

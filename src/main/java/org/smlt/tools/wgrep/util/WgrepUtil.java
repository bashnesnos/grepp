package org.smlt.tools.wgrep.util;

import java.util.Map;

import org.w3c.dom.Element;

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
}

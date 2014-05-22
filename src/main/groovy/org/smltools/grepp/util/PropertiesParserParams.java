package org.smltools.grepp.util;
import java.lang.annotation.*;

@Documented
@Inherited
@Retention(value=RetentionPolicy.RUNTIME)
@Target(value=ElementType.TYPE)
public @interface PropertiesParserParams {
	String id();
}
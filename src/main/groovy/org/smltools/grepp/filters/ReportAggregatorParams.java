package org.smltools.grepp.filters;
import java.lang.annotation.*;

@Documented
@Inherited
@Retention(value=RetentionPolicy.RUNTIME)
@Target(value=ElementType.TYPE)
public @interface ReportAggregatorParams {
	String id();
	String spoolFileExtension();
}
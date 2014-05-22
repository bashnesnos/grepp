package org.smltools.grepp.util;

public abstract class PropertiesParserBase implements PropertiesParser {
	
	@Override
	public String getId() {
        if (this.getClass().isAnnotationPresent(PropertiesParserParams.class)) {
            PropertiesParserParams parserParams = this.getClass().getAnnotation(PropertiesParserParams.class);
            return parserParams.id();
        }
        else {
            throw new IllegalArgumentException(this.getClass() + " should have PropertiesParserParams annotation or override getId method!");
        }        
    }

}

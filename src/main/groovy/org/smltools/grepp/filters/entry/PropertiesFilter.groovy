package org.smltools.grepp.filters.entry

import java.util.regex.*
import org.smltools.grepp.filters.Filter
import org.smltools.grepp.util.GreppUtil
import org.smltools.grepp.util.SavedConfigBuilder
import org.smltools.grepp.util.PropertiesParserBase
import org.smltools.grepp.util.PropertiesParserParams
import groovy.util.logging.Slf4j
import org.smltools.grepp.filters.FilterParams
import static org.smltools.grepp.util.Log4jLogbackPropUtil.*

@Slf4j("LOGGER")
@FilterParams(order = 20)
@PropertiesParserParams(id = "log4j")
public class PropertiesFilter extends PropertiesParserBase implements Filter<String> {

	@Override
    public String filter(String configString) {
    	Matcher fileMatcher = APPENDER_FILE_STRING_PTRN.matcher(configString)
		if (fileMatcher.find()) {
			String fileNameString = fileMatcher.group(1)
			LOGGER.info("File name string found = {}", fileNameString)
			String[] configIdAndfilePattern = findIdAndFileNamePattern(fileNameString)
			
			if (configIdAndfilePattern != null) {
				SavedConfigBuilder builder = new SavedConfigBuilder()
				builder.setId(configIdAndfilePattern[0])				
				builder.setPattern(configIdAndfilePattern[1])
				Matcher conversionMatcher = APPENDER_CONVERSION_STRING_PTRN.matcher(configString)
				if (conversionMatcher.find()) {
					String layoutString = conversionMatcher.group(1)
					LOGGER.info("Layout string found = {}", layoutString)
					builder.setStarter(findStarter(layoutString))
					String[] dateRegexAndDateFormat = findDateRegexAndDateFormat(layoutString)
					if (dateRegexAndDateFormat != null) {
						builder.setDateRegex(dateRegexAndDateFormat[0])
						builder.setDateFormat(dateRegexAndDateFormat[1])
					}
					else { // no log entry pattern
						LOGGER.info("dateRegex was not identified")
					}
					return builder.buildSavedConfig()
				}
				else { //no log entry pattern
					LOGGER.info("Layout string wasn't found by {}", APPENDER_CONVERSION_STRING_PTRN.toString())
				}
			}
			else { //no config id / file pattern
				LOGGER.info("File string wasn't found")
			}
		}
		else {
			LOGGER.info("File not found")
		}
		return null
    }
}




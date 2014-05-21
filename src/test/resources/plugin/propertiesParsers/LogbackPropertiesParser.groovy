
import groovy.util.logging.Slf4j
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.smltools.grepp.util.PropertiesParserProcessorBase
import org.smltools.grepp.util.PropertiesParserParams
import groovy.xml.dom.DOMCategory
import groovy.xml.DOMBuilder
import org.smltools.grepp.util.SavedConfigBuilder
import static org.smltools.grepp.filters.entry.PropertiesFilter.*

@Slf4j("LOGGER")
@PropertiesParserParams(id = "logback")
public class LogbackPropertiesParser extends PropertiesParserProcessorBase {
	SavedConfigBuilder builder
	/**
	 * Method which does processing of one File, which contains lines. <br>
	 * 
	 * @param data a File which needs to be processed
	 */
    @Override
    protected void processSingleFile(File data) {
		def cfgDoc = DOMBuilder.parse(new FileReader(data))
		def root = cfgDoc.documentElement
		use(DOMCategory) {
			root.appender.each { appender ->
				if (appender.'@class' =~ /FileAppender/) {
					builder = new SavedConfigBuilder()
					String filePattern = parseFileConfig(appender.file.text())
					if (filePattern != null) {
						builder.setPattern(filePattern)
						String starterPtrn = parseConfigLayout(appender.encoder.pattern.text())
						if (starterPtrn != null) {
							builder.setStarter(starterPtrn)
						}
						output.print(builder.buildSavedConfig())
					}
					else {
						LOGGER.info("File pattern not parsed: {}", appender.file[0])
					}
				}
			}
		}		
    }

	private String[] groupSplit(String input, Pattern splitter) {
		List<String> result = []
		Matcher mtchr = splitter.matcher(input)
		int regionStart = 0
		while (mtchr.find()) {
			int start = mtchr.start(1)
			int end = mtchr.end(1)
			LOGGER.debug("Found: {},{}", start, end)
			result.add(input.substring(regionStart, start))
			regionStart = end
		}
		result.add(input.substring(regionStart))
		return result.toArray(new String[0])
	}

	private String escapeCharForRegex(String input) {
		Matcher charReplacer = REGEX_CHAR_PTRN.matcher(input)
		StringBuffer sb = new StringBuffer()
		while (charReplacer.find()) {
			String replaceString = "\\\\" + charReplacer.group(1)
			charReplacer.appendReplacement(sb, replaceString)
		}
		charReplacer.appendTail(sb)
		return sb.toString()
	}

	private String applyDateConversionOption(String regex, String conversionOption) {
		def predefinedConversion = DateConversionOptionToRegex.keySet().find { it.contains(conversionOption)}
		String datePtrn = null
		if (predefinedConversion != null) {
			LOGGER.info("Predefined date format conversion")
			predefinedConversion = DateConversionOptionToRegex[predefinedConversion]
			datePtrn = predefinedConversion[0]
			builder.setDateFormat(predefinedConversion[1])
			
		}
		else {
			LOGGER.info("SimpleDateFormat conversion")
			datePtrn = conversionOption.replaceAll(SIMPLE_DATE_TIME_DIGITS, SIMPLE_DATE_TIME_DIGIT_REGEX)
										.replaceAll(SIMPLE_DATE_TIME_CHARS, SIMPLE_DATE_TIME_CHAR_REGEX)
			builder.setDateFormat(conversionOption)
		}
		builder.setDateRegex(datePtrn)
		return datePtrn

	}

	private String applyDefaultConversionOption(String regex, String conversionOption) {
		LOGGER.info("By default conversion option is not used: {}; {}", regex, conversionOption)
		return regex
	}

	private String applyFormatModifier(String regex, String formatModifier) {
		if (formatModifier != null && formatModifier != "") {
			//println "/" + formatModifier + "/"
			def parts = formatModifier.split("\\.")
			String leftPart = parts[0]
			String rightPart = null
			if (parts.length > 1) {
				rightPart = parts[1]
			}
			
			LOGGER.info("Format modifier parts: {};{}", leftPart, rightPart)
			if (leftPart.contains("-")) {
				return regex + PADDING_REGEX
			}
			else if (rightPart != null && rightPart.contains("-")) {
				return regex + PADDING_REGEX
			}
			else {
				return PADDING_REGEX + regex
			}
		}
		else return regex
	}

	private String parseLogEntryPtrn(String conversionPattern) {
		Matcher conversionGroups = CONVERSION_GROUPS_PTRN.matcher(conversionPattern)
		if (conversionGroups.find()) {
			def parts = conversionPattern.split(CONVERSION_GROUPS_PTRN.toString())
			//println "/" + parts + "/"
			String left = null
			String right = null
			if (parts != null && parts.length > 0) {
				left = escapeCharForRegex(parts[0])
				if (parts.length > 1) {
					right = escapeCharForRegex(parts[1])
				}
			}
			String formatModifier = conversionGroups.group(1)
			String conversionWord = conversionGroups.group(2)
			String conversionOption = conversionGroups.group(3) 
			if (conversionOption != null) {
				conversionOption = conversionOption.replaceAll("[{}]", "")
			}
			LOGGER.info("Matched! {} = {}; {}; {};", conversionGroups.group(0), formatModifier , conversionWord,  conversionOption)
			Set<String> conversionFamily = ConversionLayoutToRegex.keySet().find { it.contains(conversionWord)}
			LOGGER.info("Conversion belongs to: {}", conversionFamily)
			if (conversionFamily != null) {
				String initialRegex = ConversionLayoutToRegex[conversionFamily]
				String conversionOptionMethod = ConversionOptionSpecialConverters[conversionFamily]
				switch(conversionOptionMethod) {
					case "applyDateConversionOption" :
						this."$conversionOptionMethod"(applyFormatModifier(initialRegex, formatModifier), conversionOption)
						return null //not setting starter if it's date
					default:
						if (conversionOptionMethod == null)	{
							conversionOptionMethod = "applyDefaultConversionOption"
						}
						def resolvedOptionPattern = this."$conversionOptionMethod"(applyFormatModifier(initialRegex, formatModifier), conversionOption)
						return resolvedOptionPattern != null ?(left != null ? left : "") + resolvedOptionPattern + (right != null ? right : "") : resolvedOptionPattern
				}
			}
			else {
				LOGGER.info("Unknown conversion word: {}", conversionWord)
				return null
			}
		}
		else {
			LOGGER.info("{} not a valid conversion for /{}/", conversionPattern, CONVERSION_GROUPS_PTRN.toString())
			return null
		}
	}

	private String parseConfigLayout(String layoutString) {
		String starterPtrn
		groupSplit(layoutString, LAYOUT_SEPARATOR_PTRN).each { //need to parse all to see if there is any date ptrn
			String ptrn = parseLogEntryPtrn(it)
			if (starterPtrn == null && ptrn != null) {
				LOGGER.trace("Parsed log entry: {}", ptrn)
				starterPtrn = ptrn
			}
		}
		return starterPtrn
	}

	private String parseFileConfig(String fileNameString) {
		Matcher fileGroups = APPENDER_FILE_PTRN.matcher(fileNameString)
		if (fileGroups.matches()) {
			String fileNamePtrn = fileGroups.group(1)
			String macroPtrn = fileGroups.group(2)
			String fileExtPtrn = fileGroups.group(3)
			LOGGER.info("Matched! {} = {}; {}; {};", fileGroups.group(0), fileNamePtrn, macroPtrn, fileExtPtrn)
			builder.setId(fileNamePtrn)
			return fileNamePtrn + ((macroPtrn != null) ? ".*" : "") + ((fileExtPtrn != null) ? ("\\." + fileExtPtrn) : "")
		}
		else {
			LOGGER.info("{} not a valid filename", fileNameString)
			return null
		}

	}    

}
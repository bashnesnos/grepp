package org.smltools.grepp.filters.entry

import java.util.regex.*
import org.smltools.grepp.filters.Filter
import org.smltools.grepp.util.GreppUtil
import org.smltools.grepp.util.PropertiesParserBase
import org.smltools.grepp.util.PropertiesParserParams
import groovy.util.ConfigObject
import groovy.util.logging.Slf4j
import org.smltools.grepp.filters.FilterParams

@Slf4j("LOGGER")
@FilterParams(order = 20)
@PropertiesParserParams(id = "log4j")
public class PropertiesFilter extends PropertiesParserBase implements Filter<String> {
	//CW stands for Conversion Word
	private static String CW_STARTER = "%"
	private static String CW_OPTION_START = "\\{"
	private static String CW_OPTION_END = "\\}"
	private static String CW_FORMAT_MODIFER = "-?\\d*\\.?-?\\d*"
	private static String CONVERSION_WORD = "\\p{Alpha}*"
	private static String CONVERSION_OPTION = "(?:\\p{Graph}| )*"
	private static Pattern CONVERSION_GROUPS_PTRN = Pattern.compile(CW_STARTER + "(" + CW_FORMAT_MODIFER + ")?" + "(" + CONVERSION_WORD + ")" + "(" + CW_OPTION_START  + CONVERSION_OPTION  + CW_OPTION_END + ")?")
	private static Pattern APPENDER_FILE_PTRN = Pattern.compile(".*?[/\\\\]?(\\w+)(\\\$\\p{Graph}+\\})?\\.(\\w+)")
	private static Pattern APPENDER_FILE_STRING_PTRN = Pattern.compile("\\.[fF]ile=(.*)")
	private static Pattern APPENDER_CONVERSION_STRING_PTRN = Pattern.compile("\\.[cC]onversion[pP]attern=(.*)")
	private static Pattern LAYOUT_SEPARATOR_PTRN = Pattern.compile("(?:[\\{].*? .*?[\\}])?( )")

	private static Pattern REGEX_CHAR_PTRN = Pattern.compile("([\\[\\]\\\$^|+?:(){}])")

	private static String CLASSNAME_REGEX = "(\\\\w+\\\\.)*\\\\w*"
	private static String PADDING_REGEX = " *"
	private static String LOG_LEVEL_REGEX = "[TRACEDBUGINFLOWSV]*"
	private static String NOT_VALUABLE_REGEX = null //i.e. the results of the conversion are immaterial/belong more to message body rather than can be used for log entry start judgement
	private static String FILENAME_REGEX = "\\\\w*\\\\.(java|groovy)"
	private static String NUMBER_REGEX = "\\\\d*"

	//CW stands for Conversion Word
	private static Set<String> LOG_LEVEL_CW = ["c", "p", "le", "level"]
	private static Set<String> CLASSNAME_CW = ["C", "logger"]
	private static Set<String> FILENAME_CW = ["F", "file"]
	private static Set<String> CALLER_CW = ["l", "L", "M", "caller", "line", "method"]
	private static Set<String> DATE_CW = ["d", "date"]
	private static Set<String> NUMBER_CW = ["r", "relative"]
	private static Set<String> LINE_SEPARATOR_CW = ["n"]
	//Not valuable in terms of log entry header distinction, conversion from this group are rather related to the log message body rather than to separate one log entry from another
	private static Set<String> NOT_VALUABLE_CW = ["t", "thread", "x", "X", "contextName", "cn", "mdc", "nopex", "nopexception", "marker"
												, "property", "replace"
													]
	private static Set<String> MESSAGE_BODY_CW = ["m", "msg", "message", "ex"
												,"exception", "throwable", "xEx", "xException", "xThrowable"
												,"rEx", "rootException"
										 		]
	
	static {
		//adding those to not valuable as they won't help to identify log entry's start
		NOT_VALUABLE_CW.addAll(CALLER_CW)
		NOT_VALUABLE_CW.addAll(LINE_SEPARATOR_CW)
		NOT_VALUABLE_CW.addAll(MESSAGE_BODY_CW)
	}										 		

	private static Map<Set<String>, List<String>> ConversionLayoutToRegex = [
		(PropertiesFilter.LOG_LEVEL_CW):LOG_LEVEL_REGEX,
		(PropertiesFilter.CLASSNAME_CW):CLASSNAME_REGEX,
		(PropertiesFilter.FILENAME_CW):FILENAME_REGEX,
		(PropertiesFilter.DATE_CW):NOT_VALUABLE_REGEX,
		(PropertiesFilter.NUMBER_CW):NUMBER_REGEX,
		(PropertiesFilter.NOT_VALUABLE_CW):NOT_VALUABLE_REGEX
	]

	private static Map<Set<String>, List<String>> ConversionOptionSpecialConverters = [
		(PropertiesFilter.DATE_CW):"applyDateConversionOption"
	]

	private static String ABSOLUTE_DATE_OPTION = "ABSOLUTE"
	private static String ISO_DATE_OPTION = "ISO8601"
	private static String DATE_DATE_OPTION = "DATE"
	private static String ABSOLUTE_DATE_REGEX = "\\d{2}:\\d{2}:\\d{2},\\d{3}"
	private static String ISO_DATE_REGEX = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3}"
	private static String DATE_DATE_REGEX = "\\d{2} \\p{Alpha}{3} \\d{4} \\d{2}:\\d{2}:\\d{2},\\d{3}"
	private static String ABSOLUTE_DATE_SIMPLE = "HH:mm:ss,SSS"
	private static String ISO_DATE_SIMPLE = "yyyy-MM-dd HH:mm:ss,SSS"
	private static String DATE_DATE_SIMPLE = "dd MMM yyyy HH:mm:ss,SSS"

	private static String SIMPLE_DATE_TIME_DIGITS = "[ywWDdFHkKhmsSZ]"
	private static String SIMPLE_DATE_TIME_DIGIT_REGEX = "\\\\d"
	private static String SIMPLE_DATE_TIME_CHARS = "[GMEaz]"
	private static String SIMPLE_DATE_TIME_CHAR_REGEX = "\\\\p{Alpha}"


	private static Map<Set<String>, List<String>> DateConversionOptionToRegex = [
		[ABSOLUTE_DATE_OPTION]:[ABSOLUTE_DATE_REGEX, ABSOLUTE_DATE_SIMPLE],
		[ISO_DATE_OPTION]:[ISO_DATE_REGEX, ISO_DATE_SIMPLE],
		[DATE_DATE_OPTION]:[DATE_DATE_REGEX, DATE_DATE_SIMPLE]
	]


	private SavedConfigBuilder builder
	private List<String> configs = []
	private def result = null

	public String[] groupSplit(String input, Pattern splitter) {
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

	public String escapeCharForRegex(String input) {
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
			builder.setDateFormatVal(predefinedConversion[1])
			
		}
		else {
			LOGGER.info("SimpleDateFormat conversion")
			datePtrn = conversionOption.replaceAll(SIMPLE_DATE_TIME_DIGITS, SIMPLE_DATE_TIME_DIGIT_REGEX)
										.replaceAll(SIMPLE_DATE_TIME_CHARS, SIMPLE_DATE_TIME_CHAR_REGEX)
			builder.setDateFormatVal(conversionOption)
		}
		builder.setDateVal(datePtrn)
		return datePtrn

	}

	private String applyDefaultConversionOption(String regex, String conversionOption) {
		LOGGER.info("By default conversion option is not used")
		return regex
	}

	private String applyFormatModifier(String regex, String formatModifier) {
		if (formatModifier != null && formatModifier != "") {
			//println "/" + formatModifier + "/"
			def parts = formatModifier.split("\\.")
			String leftPart = parts[0]
			String rightPart = null
			if (parts.length > 1)
				rightPart = parts[1]
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
				if (parts.length > 1)
					right = escapeCharForRegex(parts[1])
			}
			String formatModifier = conversionGroups.group(1)
			String conversionWord = conversionGroups.group(2)
			String conversionOption = conversionGroups.group(3) 
			if (conversionOption != null) 
				conversionOption = conversionOption.replaceAll("[{}]", "")
			LOGGER.info("Matched! {} = {}; {}; {};", conversionGroups.group(0), formatModifier , conversionWord,  conversionOption)
			Set<String> conversionFamily = ConversionLayoutToRegex.keySet().find { it.contains(conversionWord)}
			LOGGER.info("Conversion belongs to: {}", conversionFamily)
			if (conversionFamily != null) {
				String initialRegex = ConversionLayoutToRegex[conversionFamily]
				String conversionOptionMethod = ConversionOptionSpecialConverters[conversionFamily]
				if (conversionOptionMethod == null) conversionOptionMethod = "applyDefaultConversionOption"
				return (left != null ? left : "") + this."$conversionOptionMethod"(applyFormatModifier(initialRegex, formatModifier), conversionOption) + (right != null ? right : "")
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
			builder.setIdVal(fileNamePtrn)
			return fileNamePtrn + ((macroPtrn != null) ? ".*" : "") + ((fileExtPtrn != null) ? ("\\." + fileExtPtrn) : "")
		}
		else {
			LOGGER.info("{} not a valid filename", fileNameString)
			return null
		}

	}

	public String parseConfig(String configString) {
		Matcher fileMatcher = APPENDER_FILE_STRING_PTRN.matcher(configString)
		if (fileMatcher.find()) {
			builder = new SavedConfigBuilder()
			String fileNameString = fileMatcher.group(1)
			LOGGER.info("File name string found = {}", fileNameString)
			String filePattern = parseFileConfig(fileNameString)
			
			if (filePattern != null) {
				builder.setPatternVal(filePattern)
				Matcher conversionMatcher = APPENDER_CONVERSION_STRING_PTRN.matcher(configString)
				if (conversionMatcher.find()) {
					String layoutString = conversionMatcher.group(1)
					LOGGER.info("Layout string found = {}", layoutString)
					String starterPtrn = parseConfigLayout(layoutString)
					if (starterPtrn != null) {
						builder.setStarterVal(starterPtrn)
						return builder.buildSavedConfig()
					}
					else { // no log entry pattern
						LOGGER.info("Starter was not identified")
						return null
					}
				}
				else { //no log entry pattern
					LOGGER.info("Layout string wasn't found by {}", APPENDER_CONVERSION_STRING_PTRN.toString())
					return null
				}
			}
			else { //no config id / file pattern
				LOGGER.info("File string wasn't found")
				return null
			}
		}
		else {
			LOGGER.info("File not found")
			return null
		}
	}

	@Override
    public String filter(String blockData) {
    	return parseConfig(blockData)
    }

	public PropertiesFilter(){

	}

	class SavedConfigBuilder {
		private String idVal
		private String dateFormatVal
		private String dateVal
		private String starterVal
		private int logThresholdVal
		private String patternVal
		

		SavedConfigBuilder setIdVal(String idVal_) {
			idVal = idVal_
			return this
		}

		SavedConfigBuilder setDateFormatVal(String dateFormat_) {
			dateFormatVal = dateFormat_
			return this
		}

		SavedConfigBuilder setDateVal(String date_) {
			dateVal = date_
			return this
		}

		SavedConfigBuilder setStarterVal(String starter_) {
			starterVal = starter_
			return this
		}

		SavedConfigBuilder setLogThresholdVal(String logThreshold_) {
			logThresholdVal = logThreshold_
			return this
		}

		SavedConfigBuilder setPatternVal(String pattern_) {
			patternVal = pattern_
			return this
		}

		String buildSavedConfig(){
						
			if (idVal == null || idVal == "") {//pointless to create if config doesn't have an identifier
				return null
			}

			def config = new ConfigObject()
			def props = config.savedConfigs."$idVal"

			if (dateFormatVal != null) {
				props.dateFormat.value = dateFormatVal
				config.logDateFormats."$idVal".value = props.dateFormat.value				
			}

			if (dateVal != null) {
				props.dateFormat.regex = "(" + dateVal + ")"
				config.logDateFormats."$idVal".regex = props.dateFormat.regex
			}

			if (starterVal != null && starterVal != dateVal) {
				props.starter = starterVal + ".*"
			}

			if (logThresholdVal != 0) {
				config.logThreshold = logThresholdVal
			}

			if (patternVal != null) {
				props.pattern = patternVal
			}

			def writer = new StringWriter()
			config.writeTo(writer)
			return GreppUtil.escapeRegexes(writer.toString())

		}
	}
}




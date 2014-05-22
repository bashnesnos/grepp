package org.smltools.grepp.util;

import java.util.regex.*
import org.smltools.grepp.util.GreppUtil
import groovy.util.logging.Slf4j

@Slf4j("LOGGER")
public class Log4jLogbackPropUtil {
	//CW stands for Conversion Word
	public static String CW_STARTER = "%"
	public static String CW_OPTION_START = "\\{"
	public static String CW_OPTION_END = "\\}"
	public static String CW_FORMAT_MODIFER = "-?\\d*\\.?-?\\d*"
	public static String CONVERSION_WORD = "\\p{Alpha}*"
	public static String CONVERSION_OPTION = "(?:\\p{Graph}| )*?"
	public static Pattern CONVERSION_GROUPS_PTRN = Pattern.compile(CW_STARTER + "(" + CW_FORMAT_MODIFER + ")?" + "(" + CONVERSION_WORD + ")" + "(" + CW_OPTION_START  + CONVERSION_OPTION  + CW_OPTION_END + ")?")
	public static Pattern APPENDER_FILE_PTRN = Pattern.compile(".*?[/\\\\]?(\\w+)(\\\$\\p{Graph}+\\})?\\.(\\w+)")
	public static Pattern APPENDER_FILE_STRING_PTRN = Pattern.compile("\\.[fF]ile=(.*)")
	public static Pattern APPENDER_CONVERSION_STRING_PTRN = Pattern.compile("\\.[cC]onversion[pP]attern=(.*)")
	public static Pattern LAYOUT_SEPARATOR_PTRN = Pattern.compile("(?:[\\{].*? .*?[\\}])?( )")

	public static Pattern REGEX_CHAR_PTRN = Pattern.compile("([\\[\\]\\\$^|+?:(){}])")

	public static String CLASSNAME_REGEX = "(\\\\w+\\\\.)*\\\\w*"
	public static String PADDING_REGEX = " *"
	public static String LOG_LEVEL_REGEX = "[TRACEDBUGINFLOWSV]*"
	public static String NOT_VALUABLE_REGEX = null //i.e. the results of the conversion are immaterial/belong more to message body rather than can be used for log entry start judgement
	public static String FILENAME_REGEX = "\\\\w*\\\\.(java|groovy)"
	public static String NUMBER_REGEX = "\\\\d*"

	//CW stands for Conversion Word
	public static Set<String> LOG_LEVEL_CW = ["c", "p", "le", "level"]
	public static Set<String> CLASSNAME_CW = ["C", "logger"]
	public static Set<String> FILENAME_CW = ["F", "file"]
	public static Set<String> CALLER_CW = ["l", "L", "M", "caller", "line", "method"]
	public static Set<String> DATE_CW = ["d", "date"]
	public static Set<String> NUMBER_CW = ["r", "relative"]
	public static Set<String> LINE_SEPARATOR_CW = ["n"]
	//Not valuable in terms of log entry header distinction, conversion from this group are rather related to the log message body rather than to separate one log entry from another
	private static final Set<String> NOT_VALUABLE_CW = ["t", "thread", "x", "X", "contextName", "cn", "mdc", "nopex", "nopexception", "marker"
												, "property", "replace"
													]

	public static Set<String> getNotValuableConversionWords() {
		return NOT_VALUABLE_CW
	}

	private static final Set<String> MESSAGE_BODY_CW = ["m", "msg", "message", "ex"
												,"exception", "throwable", "xEx", "xException", "xThrowable"
												,"rEx", "rootException"
										 		]

	public static Set<String> getMessageBodyConversionWords() {
		return MESSAGE_BODY_CW
	}
	
	static {
		//adding those to not valuable as they won't help to identify log entry's start
		NOT_VALUABLE_CW.addAll(CALLER_CW)
		NOT_VALUABLE_CW.addAll(LINE_SEPARATOR_CW)
		NOT_VALUABLE_CW.addAll(MESSAGE_BODY_CW)
	}										 		

	private static final Map<Set<String>, List<String>> CONVERSION_LAYOUT_TO_REGEX_MAP = [
		(Log4jLogbackPropUtil.LOG_LEVEL_CW):LOG_LEVEL_REGEX,
		(Log4jLogbackPropUtil.CLASSNAME_CW):CLASSNAME_REGEX,
		(Log4jLogbackPropUtil.FILENAME_CW):FILENAME_REGEX,
		(Log4jLogbackPropUtil.DATE_CW):NOT_VALUABLE_REGEX,
		(Log4jLogbackPropUtil.NUMBER_CW):NUMBER_REGEX,
		(Log4jLogbackPropUtil.NOT_VALUABLE_CW):NOT_VALUABLE_REGEX
	]

	public static String ABSOLUTE_DATE_OPTION = "ABSOLUTE"
	public static String ISO_DATE_OPTION = "ISO8601"
	public static String DATE_DATE_OPTION = "DATE"
	public static String ABSOLUTE_DATE_REGEX = "\\d{2}:\\d{2}:\\d{2},\\d{3}"
	public static String ISO_DATE_REGEX = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3}"
	public static String DATE_DATE_REGEX = "\\d{2} \\p{Alpha}{3} \\d{4} \\d{2}:\\d{2}:\\d{2},\\d{3}"
	public static String ABSOLUTE_DATE_SIMPLE = "HH:mm:ss,SSS"
	public static String ISO_DATE_SIMPLE = "yyyy-MM-dd HH:mm:ss,SSS"
	public static String DATE_DATE_SIMPLE = "dd MMM yyyy HH:mm:ss,SSS"

	public static String SIMPLE_DATE_TIME_DIGITS = "[ywWDdFHkKhmsSZ]"
	public static String SIMPLE_DATE_TIME_DIGIT_REGEX = "\\\\d"
	public static String SIMPLE_DATE_TIME_CHARS = "[GMEaz]"
	public static String SIMPLE_DATE_TIME_CHAR_REGEX = "\\\\p{Alpha}"


	private static final Map<Set<String>, List<String>> DATE_CONVERSION_OPTION_TO_REGEX_MAP = [
		[ABSOLUTE_DATE_OPTION]:[ABSOLUTE_DATE_REGEX, ABSOLUTE_DATE_SIMPLE],
		[ISO_DATE_OPTION]:[ISO_DATE_REGEX, ISO_DATE_SIMPLE],
		[DATE_DATE_OPTION]:[DATE_DATE_REGEX, DATE_DATE_SIMPLE]
	]

	public enum Conversion {
		DATE(Log4jLogbackPropUtil.DATE_CW) {
			public String[] applyConversion(String regex, String conversionOption) {
				def predefinedConversion = Log4jLogbackPropUtil.DATE_CONVERSION_OPTION_TO_REGEX_MAP.keySet().find { it.contains(conversionOption)}
				String datePtrn = null
				if (predefinedConversion != null) {
					predefinedConversion = Log4jLogbackPropUtil.DATE_CONVERSION_OPTION_TO_REGEX_MAP[predefinedConversion]
					datePtrn = predefinedConversion[0]
					return [datePtrn, predefinedConversion[1]] as String[]
				}
				else {
					datePtrn = conversionOption.replaceAll(Log4jLogbackPropUtil.SIMPLE_DATE_TIME_DIGITS, Log4jLogbackPropUtil.SIMPLE_DATE_TIME_DIGIT_REGEX)
												.replaceAll(Log4jLogbackPropUtil.SIMPLE_DATE_TIME_CHARS, Log4jLogbackPropUtil.SIMPLE_DATE_TIME_CHAR_REGEX)
					return [datePtrn, conversionOption] as String[]
				}				
			}
		};

		Set<String> conversionWordSet
		Conversion(Set<String> conversionWordSet) {
			GreppUtil.throwIllegalAEifNull(conversionWordSet, "Null conversionWordSets not allowed")
			this.conversionWordSet = conversionWordSet
		}

		public static Conversion findConversion(Set<String> conversionWordSet) {
			return this.values().find { conversion ->
				conversion.conversionWordSet.find { conversionWordSet.contains(it) }
			}
		}

		public abstract String[] applyConversion(String regex, String conversionOption);
	}

	public static String[] groupSplit(String input, Pattern splitter) {
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

	public static String escapeCharForRegex(String input) {
		Matcher charReplacer = REGEX_CHAR_PTRN.matcher(input)
		StringBuffer sb = new StringBuffer()
		while (charReplacer.find()) {
			String replaceString = "\\\\" + charReplacer.group(1)
			charReplacer.appendReplacement(sb, replaceString)
		}
		charReplacer.appendTail(sb)
		return sb.toString()
	}

	public static String applyFormatModifier(String regex, String formatModifier) {
		String result = regex
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
				result = regex + PADDING_REGEX
			}
			else if (rightPart != null && rightPart.contains("-")) {
				result = regex + PADDING_REGEX
			}
			else {
				result = PADDING_REGEX + regex
			}
		}
		return result
	}

	public static String[] parseLogEntryPtrn(String conversionPattern, Set<String> limitingConversionFamily) {
		Matcher conversionGroups = CONVERSION_GROUPS_PTRN.matcher(conversionPattern)
		String[] result = null
		if (conversionGroups.find()) {
			def parts = conversionPattern.split(CONVERSION_GROUPS_PTRN.toString())

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
			Set<String> conversionFamily = CONVERSION_LAYOUT_TO_REGEX_MAP.keySet().find { it.contains(conversionWord)}
			LOGGER.info("Conversion belongs to: {}", conversionFamily)
			
			if (conversionFamily != null) {
				if (limitingConversionFamily == null || limitingConversionFamily.contains(conversionWord)) {
					String initialRegex = CONVERSION_LAYOUT_TO_REGEX_MAP[conversionFamily]
					Conversion conversion = Conversion.findConversion(conversionFamily)
					switch(conversion) {
						case Conversion.DATE :
							//date regex found!
							result = conversion.applyConversion(applyFormatModifier(initialRegex, formatModifier), conversionOption)
							break
						default:
							LOGGER.trace("By default conversion is not applied")
							def resolvedOptionPattern = applyFormatModifier(initialRegex, formatModifier)
							//it should be a starter then
							result = [resolvedOptionPattern != null ? ((left != null ? left : "") + resolvedOptionPattern + (right != null ? right : "")) : null] as String[]
					}				
				}
				else {
					LOGGER.debug("Skipping {} since it's not one of the target one {}", conversionWord, limitingConversionFamily)
				}
			}
			else {
				LOGGER.info("Unknown conversion word: {}", conversionWord)
			}
		}
		else {
			LOGGER.info("{} not a valid conversion for /{}/", conversionPattern, CONVERSION_GROUPS_PTRN.toString())
		}
		LOGGER.info("parsed: {}", result as Object)
		return result
	}

	public static String[] findDateRegexAndDateFormat(String layoutString) {
		return groupSplit(layoutString, LAYOUT_SEPARATOR_PTRN).findResult { //need to parse all to see if there is any date ptrn
			parseLogEntryPtrn(it, DATE_CW)
		}
	}

	public static String findStarter(String layoutString) {
		String[] result = null
		String[] dateRegex = null
		groupSplit(layoutString, LAYOUT_SEPARATOR_PTRN).each { //we should look only before a date conversion
			if (result == null) {
				if (dateRegex == null) {
					dateRegex = parseLogEntryPtrn(it, DATE_CW) //checking if it's not a date first
					if (dateRegex == null) {
						result = parseLogEntryPtrn(it, null)
					}
					else {
						LOGGER.trace("Skipping $it as it's a date conversion")
					}
				}
				else {
					LOGGER.trace("Skipping $it as it's after a date conversion")
				}
			}
			else {
				LOGGER.trace("Skipping $it as a starter was found already")
			}
		}
		return result != null ? result[0] : null
	}

	public static String[] findIdAndFileNamePattern(String fileNameString) {
		Matcher fileGroups = APPENDER_FILE_PTRN.matcher(fileNameString)
		if (fileGroups.matches()) {
			String fileNamePtrn = fileGroups.group(1)
			String macroPtrn = fileGroups.group(2)
			String fileExtPtrn = fileGroups.group(3)
			LOGGER.info("Matched! {} = {}; {}; {};", fileGroups.group(0), fileNamePtrn, macroPtrn, fileExtPtrn)
			return [fileNamePtrn, fileNamePtrn + ((macroPtrn != null) ? ".*" : "") + ((fileExtPtrn != null) ? ("\\." + fileExtPtrn) : "")] as String[]
		}
		else {
			LOGGER.info("{} not a valid filename", fileNameString)
			return null
		}

	}

}




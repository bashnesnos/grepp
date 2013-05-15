import groovy.xml.MarkupBuilder
import groovy.util.logging.Slf4j
import java.util.regex.*

@Slf4j
class WgrepConfigParserImpl {
	//CW stands for Conversion Word
	private static String CW_STARTER = "%"
	private static String CW_OPTION_START = "\\{"
	private static String CW_OPTION_END = "\\}"
	private static String CW_FORMAT_MODIFER = "-?\\d*\\.?-?\\d*"
	private static String CONVERSION_WORD = "\\p{Alpha}*"
	private static String CONVERSION_OPTION = "\\p{Graph}*"
	private static Pattern CONVERSION_GROUPS_PTRN = Pattern.compile(CW_STARTER + "(" + CW_FORMAT_MODIFER + ")?" + "(" + CONVERSION_WORD + ")" + "(" + CW_OPTION_START  + CONVERSION_OPTION  + CW_OPTION_END + ")?")
	
	private static String CLASSNAME_REGEX = "(\\\\w+\\\\.)*\\\\w*"
	private static String PADDING_REGEX = " *"
	private static String LOG_LEVEL_REGEX = "[TRACEDBUGINFOWLSV]*"
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
		(WgrepConfigParserImpl.LOG_LEVEL_CW):LOG_LEVEL_REGEX,
		(WgrepConfigParserImpl.CLASSNAME_CW):CLASSNAME_REGEX,
		(WgrepConfigParserImpl.FILENAME_CW):FILENAME_REGEX,
		(WgrepConfigParserImpl.DATE_CW):NOT_VALUABLE_REGEX,
		(WgrepConfigParserImpl.NUMBER_CW):NUMBER_REGEX,
		(WgrepConfigParserImpl.NOT_VALUABLE_CW):NOT_VALUABLE_REGEX
	]

	private static Map<Set<String>, List<String>> ConversionOptionSpecialConverters = [
		(WgrepConfigParserImpl.DATE_CW):"applyDateConversionOption"
	]

	private static String ABSOLUTE_DATE_OPTION = "ABSOLUTE"
	private static String ISO_DATE_OPTION = "ISO8601"
	private static String DATE_DATE_OPTION = "DATE"
	private static String ABSOLUTE_DATE_REGEX = "ABSOLUTE"
	private static String ISO_DATE_REGEX = "ISO8601"
	private static String DATE_DATE_REGEX = "DATE"
	private static String ABSOLUTE_DATE_SIMPLE = "ABSOLUTE"
	private static String ISO_DATE_SIMPLE = "ISO8601"
	private static String DATE_DATE_SIMPLE = "DATE"

	private static String SIMPLE_DATE_TIME_DIGITS = "[ywWDdFHkKhmsSZ]"
	private static String SIMPLE_DATE_TIME_DIGIT_REGEX = "\\\\d"
	private static String SIMPLE_DATE_TIME_CHARS = "[GMEaz]"
	private static String SIMPLE_DATE_TIME_CHAR_REGEX = "\\\\p{Alpha}"


	private static Map<Set<String>, List<String>> DateConversionOptionToRegex = [
		[ABSOLUTE_DATE_OPTION]:[ABSOLUTE_DATE_REGEX, ABSOLUTE_DATE_SIMPLE],
		[ISO_DATE_OPTION]:[ISO_DATE_REGEX, ISO_DATE_SIMPLE],
		[DATE_DATE_OPTION]:[DATE_DATE_REGEX, DATE_DATE_SIMPLE]
	]

	public static CustomConfigXmlBuilder applyDateConversionOption(CustomConfigXmlBuilder builder, String conversionOption) {
		def predefinedConversion = DateConversionOptionToRegex.keySet().find { it.contains(conversionOption)}
		if (predefinedConversion != null) {
			log.info("Predefined date format conversion")
			builder.setDate(predefinedConversion[0]).setDateFormat(predefinedConversion[1])
		}
		else {
			log.info("SimpleDateFormat conversion")
			builder.setDate(conversionOption.replaceAll(SIMPLE_DATE_TIME_DIGITS, SIMPLE_DATE_TIME_DIGIT_REGEX)
											.replaceAll(SIMPLE_DATE_TIME_CHARS, SIMPLE_DATE_TIME_CHAR_REGEX))
					.setDateFormat(conversionOption)
		}
		return builder

	}

	public static String applyDefaultConversionOption(String regex, String conversionOption) {
		log.info("By default conversion option is not used")
	}

	public static String applyFormatModifier(String regex, String formatModifier) {
		def parts = formatModifier.split("\\.")
		log.info("Format modifier parts: {};{}", parts[0], parts[1])
		println parts[0] + " " + parts[1]
	}

	public static CustomConfigXmlBuilder parseLogEntryPtrn(CustomConfigXmlBuilder builder, String conversionPattern) {
		Matcher conversionGroups = CONVERSION_GROUPS_PTRN.matcher(conversionPattern)
		if (conversionGroups.matches()) {
			String formatModifier = conversionGroups.group(1)
			String conversionWord = conversionGroups.group(2)
			String conversionOption = conversionGroups.group(3)
			log.info("Matched! {} = {}; {}; {};", conversionGroups.group(0), formatModifier , conversionWord,  conversionOption)
			Set<String> conversionFamily = ConversionLayoutToRegex.keySet().find { it.contains(conversionWord)}
			log.info("Conversion belongs to: {}", conversionFamily)
			if (conversionFamily != null) {
				String initialRegex = ConversionLayoutToRegex[conversionFamily]
				return builder
			}
			else {
				log.info("Unknown conversion word: {}", conversionWord)
				return null
			}
		}
		else {
			log.info("{} not a valid conversion", conversionPattern)
			return null
		}
	}

	public static CustomConfigXmlBuilder parseConfigLayout(CustomConfigXmlBuilder builder, String layoutString) {
		layoutString.split(" ").each {

		}
		return builder
	}


	private WgrepConfigParserImpl() { throw new AssertionError() } //don't instantiate it, really

	public static void main(String[] args) {

	}

	class CustomConfigXmlBuilder {
		private String idVal
		private String dateFormatVal
		private String dateVal
		private String starterVal
		private int logThresholdVal
		private String patternVal
		

		CustomConfigXmlBuilder setId(String idVal_) {
			idVal = idVal_
			return this
		}

		CustomConfigXmlBuilder setDateFormat(String dateFormat_) {
			dateFormatVal = dateFormat_
			return this
		}

		CustomConfigXmlBuilder setDate(String date_) {
			dateVal = date_
			return this
		}

		CustomConfigXmlBuilder setStarter(String starter_) {
			starterVal = starter_
			return this
		}

		CustomConfigXmlBuilder setLogThreshold(String logThreshold_) {
			logThresholdVal = logThreshold_
			return this
		}

		CustomConfigXmlBuilder setPattern(String pattern_) {
			patternVal = pattern_
			return this
		}

		String buildXmlString(){
			def writer = new StringWriter()
			def xml = new MarkupBuilder(writer)
			
			if (idVal == null || idVal == "") //pointless to create if config doesn't have an identifier
				return null

			xml.records() {
				config(id:idVal) {
					if (dateFormatVal != null)
						date_format(dateFormatVal)
					if (dateVal != null)
						date(dateVal)
					if (starterVal != nul)
						starter(starterVal)
					if (logThresholdVal != null)
						log_threshold(logThresholdVal)
					if (patternVal != null)
						pattern(patternVal)
				}	
			}

			return writer.toString()

		}
	}
}

println WgrepConfigParserImpl.parseLogEntryPtrn(null, "%.30logger{30}")
println WgrepConfigParserImpl.applyFormatModifier(null, ".-30")
//println WgrepConfigParserImpl.dateOptionConverter("dd MMM yyyy HH:mm:ss,SSS")


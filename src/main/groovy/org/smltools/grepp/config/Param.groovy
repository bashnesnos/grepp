package org.smltools.grepp.config;

public enum Param {
	FILES("Files to process", Type.DATA),
	FOLDER_SEPARATOR("System folder separator"),
	CWD("Current work directory"),
	HOME_DIR("Grepp install directory"),
	HELP("Command-line style help"),
	RESULTS_DIR("Results directory"),
	SPOOLING_EXT("Spooling file extension"),
	SPOOLING("Toggles spooling to configured results dir and with configured spooling extension"),
	FILE_MERGING("Toggles non-stop file traversing"),
	CONFIG_FILE_PATH("Path to used config.xml"),
	FILTER_PATTERN("Regex like string to be used for filtering"),
	LOG_ENTRY_PATTERN("Regex like string to be used for entries splitting"),
	USER_ENTRY_PATTERN("Regex like string to be used for entries splitting"),
	DATE_TIME_FILTER("Date filtering identifier"),
	FROM_DATE("Date representing left boundary"),
	TO_DATE("Date representing right boundary"),
	LOG_DATE_PATTERN("Regex like pattern representing log entry date"),
	LOG_DATE_FORMAT("Java format to convert entry date"),
	FILE_DATE_FORMAT("Java format to convert file date"),
	LOG_FILE_THRESHOLD("Amount in hours which shows when the log file is expired"),
	ATMTN_LEVEL("Level of settings prediction"),
	TRACE("Indicator that force tracing is on"),
	PREDEF_TAG("Predefined tag for processing set up"),
	PRESERVE_THREAD("Toggles thread ID preserving, i.e. all the records for a thread will be fetched"),
	PRESERVE_THREAD_PARAMS("Map contatinig all needed params for complex filtering processor"),
	POST_PROCESSING("Toggles results grouping and stuff to produce nice CSV files"),
	POST_PROCESS_PARAMS("Map containing all needed params for post processor"),
	PARSE_PROPERTIES("Toggles .property file to config.xml parsing");
		
	private final String descrptn;
	private final Type type;
	
	Param(String descrptn) {
	   this(descrptn, Type.PARAM)
	}
	
	Param(String descrptn, Type type) {
		this.type = type;
		this.descrptn = descrptn;
	 }
	
	public String getDescription() {
	  return descrptn;
	}
	
	public Object getDefaultValue() {
		return type.getDefaultValue()
	}
	
	public static List<Param> getDataParams() {
		return Param.values().findAll {
			Type.DATA.equals(it.type)
		}
	}
	
	enum Type {
		DATA{ public Object getDefaultValue() { return java.lang.System.in }},
		TOGGLE,
		PARAM,
		PARAMS;
		
		//private final Object defaultVal;
		
		//Type(Object defaultVal) {
		//	this.defaultVal = defaultVal
		//}
		
		public Object getDefaultValue() {
			return null;
		};
	}
	
}

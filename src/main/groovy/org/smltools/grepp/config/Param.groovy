package org.smltools.grepp.config;

public enum Param {
	FILES("Files to process", Type.DATA),
	FOLDER_SEPARATOR("System folder separator"),
	CONFIG("Current config"),
	CWD("Current work directory"),
	HOME_DIR("Grepp install directory"),
	RESULTS_DIR("Results directory"),
	SPOOLING_EXT("Spooling file extension"),
	SPOOLING("Toggles spooling to configured results dir and with configured spooling extension", Type.FLAG),
	FILE_MERGING("Toggles non-stop file traversing", Type.FLAG),
	FILTER_PATTERN("Regex like string to be used for filtering"),
	LOG_ENTRY_PATTERN("Regex like string to be used for entries splitting"),
	USER_ENTRY_PATTERN("Use supplied log entry pattern over any configure one", Type.FLAG),
	DATE_TIME_FILTER("Date filtering toggle", Type.FLAG),
	FROM_DATE("Date representing left boundary"),
	TO_DATE("Date representing right boundary"),
	LOG_DATE_PATTERN("Regex like pattern representing log entry date"),
	LOG_DATE_FORMAT("Java format to convert entry date"),
	FILE_DATE_FORMAT("Java format to convert file date"),
	LOG_FILE_THRESHOLD("Amount in hours which shows when the log file is expired"),
	TRACE("Indicator that force tracing is on", Type.FLAG),
	PREDEF_TAG("Predefined tag for processing set up"),
	PRESERVE_THREAD("Toggles thread ID preserving, i.e. all the records for a thread will be fetched"),
	PRESERVE_THREAD_PARAMS("Map contatinig all needed params for complex filtering processor", Type.PARAMS),
	POST_PROCESSING("Toggles results grouping and stuff to produce nice CSV files"),
	POST_PROCESS_PARAMS("Map containing all needed params for post processor", Type.PARAMS),
	PARSE_PROPERTIES("Toggles logging .properties file to grepp config parsing", Type.FLAG),
	VERBOSE("Indicator that force info is on", Type.FLAG);
		
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
		FLAG{ public Object getDefaultValue() { return false }},
		PARAM,
		PARAMS;

		public Object getDefaultValue() {
			return null;
		};
	}
	
}

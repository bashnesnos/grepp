package org.smltools.grepp.config.varparsers

import java.text.SimpleDateFormat
import java.util.Map;

import groovy.util.ConfigObject
import groovy.util.logging.Slf4j;
import groovy.xml.dom.DOMCategory

/**
 * Provides parsing for date time filters. <br>
 * Identifies input date format and patterns from config.xml
 * 
 * @author Alexander Semelit
 *
 */
@Slf4j
class DateTimeParser implements ParamParser<String>
{
    private static final String FROM_DATE = 'from'
    private static final String TO_DATE = 'to'

    private Collection INPUT_DATE_PTTRNS
	private boolean initialized = false
    private boolean isFromParsed = false
    private boolean isToParsed = false
	private ConfigObject config
    private SimpleDateFormat defaultDateFormat = null
    private Map<String, String> offsetParamMap = [:]
	
	/**
	 * Fetches needed params from config and parses patterns from config.xml by identified <config> id.
	 * 
	 * @param config initialized instance of ConfigObject
	 */
    public DateTimeParser()
    {
		offsetParamMap[FROM_DATE] = null
		offsetParamMap[TO_DATE] = FROM_DATE
		INPUT_DATE_PTTRNS =  [ "yyyy-MM-dd'T'HH:mm:ss"
        , "yyyy-MM-dd'T'HH:mm"
        , "yyyy-MM-dd'T'HH"
        , "yyyy-MM-dd"
        , "yyyy-MM"
        , "yyyy" ]
        defaultDateFormat = new SimpleDateFormat(INPUT_DATE_PTTRNS[0])
    }

    /**
    *
    * Checking if dates are ok.
    */
    def checkDates() {
        log.trace("checking if dates are valid")
        Date from = config.runtime.dateFilter."$FROM_DATE"
        Date to =  config.runtime.dateFilter."$TO_DATE"
        def swapDate = null
        if (from != null && to != null)
        {
            if (from.after(to)) { //swapping dates
                log.trace("FROM is AFTER to, swapping them")
                config.runtime.dateFilter."$FROM_DATE" = to
                config.runtime.dateFilter."$TO_DATE" = from
            }
        } 
        else {
            if (from == null)
                log.warn('FROM is null')
            if (to == null)
                log.warn('TO is null')
        }

    }

	/**
	 * Iteratively applies configured INPUT_DATE_PTTRNS until matching will be found. <br>
	 * If non matches null will be returned.
	 * 
	 * @param dateStr input date String
	 * @return Date value of supplied String if it was parsed by configured date patterns.
	 */
    def parseInput(def dateStr)
    {
        def date = null
        INPUT_DATE_PTTRNS.find { ptrn -> 
            log.trace("trying date pattern={}", ptrn); 
            try {
                date = new SimpleDateFormat(ptrn).parse(dateStr) 
                log.trace("Pattern found")
                true
            }
            catch(java.text.ParseException pe)
            {
                false
            }
        }
        if (date != null) return date 
        else null
    }

    /**
     * Aplies first pattern by default and sets date as an offset from a param; or from NOW if it is not specified . <br>
     * Returns null, if no minutes were supplied, i.e. if it is an indefinite offset.
     * 
     * @param paramName param which should be taken as a base to calculate offset from. Is nullable.
     * @param offsetStr String in format -\d* or +\d*, which shows how many minutes to sbutract/add.
     * @return Date value of supplied String if it was parsed by configured date patterns.
     */
    def parseOffset(String paramName, def offsetStr)
    {
        def date = null
        log.trace("Applying format#1 by default: {}", defaultDateFormat.toPattern())
        def minutesMatcher = offsetStr =~ /.(\d*)/
        if (offsetStr.size() > 1 && minutesMatcher.find())
        {
            log.trace("Total minutes: {}", minutesMatcher.group(1))
            def minutes = Integer.valueOf(minutesMatcher.group(1))*60*1000
            def curDate = config.runtime.dateFilter."$paramName"
			if (curDate == null || !curDate instanceof Date) curDate = new Date()
			
            date = curDate
            switch(offsetStr) {
                case ~/^\+.*/:
                    log.trace("adding")
                    date = new Date(curDate.getTime() + minutes) //adding minutes to NOW
                    break
                case ~/^-.*/:
                    log.trace("subtracting")
                    date = new Date(curDate.getTime() - minutes) //subtracting minutes to NOW
                    break
            }
        }
        if (date != null) return date 
        else null
    }


    def parseDate(String paramName, def dateStr) {
        def matcher = dateStr =~ /^[+-]/
        if (!matcher.find()) {
            log.trace("Usual dates parsing")
            config.runtime.dateFilter."$paramName" = parseInput(dateStr)
        }
        else
        {
            log.trace("Offset dates parsing")
            config.runtime.dateFilter."$paramName" = parseOffset(offsetParamMap[paramName],dateStr)
        }
    }
	/**
	 * Parses and sets supplied string as FROM_DATE parameter.
	 * 
	 * @param date string to be parsed
	 */
    void setDateFrom(String date)
    {
        parseDate(FROM_DATE, date)
        isFromParsed = true
    }

	/**
	 * Parses and sets supplied string as TO_DATE parameter.
	 *
	 * @param date string to be parsed
	 */
    void setDateTo(String date)
    {
        parseDate(TO_DATE, date)
        isToParsed = true
    }

	@Override
	public boolean parseVar(ConfigObject config, String arg) {
        this.config = config

		log.trace("Parsing var: {}", arg)
		if (!isFromParsed) setDateFrom(arg)
		else if (!isToParsed)
		{
			setDateTo(arg)
			checkDates()
			return true;
		}
		return false;
	}

}
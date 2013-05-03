package org.smlt.tools.wgrep.config.varparsers

import java.text.SimpleDateFormat
import org.smlt.tools.wgrep.config.WgrepConfig
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
class DateTimeParser extends ParserBase
{
    private def INPUT_DATE_PTTRNS = []
    private boolean isFromParsed = false
    private boolean isToParsed = false
    private SimpleDateFormat defaultDateFormat = null
    private offsetParamMap = ['FROM_DATE':null,
        'TO_DATE':'FROM_DATE']

	/**
	 * Fetches needed params from config and parses patterns from config.xml by identified <config> id.
	 * 
	 * @param config initialized instance of WgrepConfig
	 */
    DateTimeParser(WgrepConfig config)
    {
		super(config)
		def dt_tag = getParam('DATE_TIME_FILTER')
        use(DOMCategory)
        {
            def ptrns = getRoot().date_time_config.pattern.findAll { it.'@tags' =~ dt_tag }
            ptrns.sort { it.'@order' }.each {INPUT_DATE_PTTRNS.add(it.text())}
        }
        defaultDateFormat = new SimpleDateFormat(INPUT_DATE_PTTRNS[0])
    }

	@Override
    boolean isConfigValid() {
        boolean checkResult = super.isConfigValid()
        if (getParam('DATE_TIME_FILTER') == null)
        {
            log.warn('DATE_TIME_FILTER is not specified')
            checkResult = false
        }
        return checkResult
    }

	/**
	 * Parses FROM date and TO date and sequentially and then unsubscribes.
	 * 
	 */
	@Override
    void parseVar(def arg)
    {
        log.trace("Additional var: " + arg)
        if (!isFromParsed) setDateFrom(arg)
        else if (!isToParsed) 
        {
            setDateTo(arg)
            checkDates()
            unsubscribe()
        }
    }

    /**
    *
    * Checking if dates are ok.
    */
    def checkDates() {
        log.trace('checking if dates are valid')
        Date from = getParam('FROM_DATE')
        Date to = getParam('TO_DATE')
        def swapDate = null
        if (from != null && to != null)
        {
            if (from.after(to)) { //swapping dates
                log.trace('FROM is AFTER to, swapping them')
                setParam('FROM_DATE', to)
                setParam('TO_DATE', from)
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
            log.trace("trying date pattern="+ ptrn); 
            try {
                date = (new SimpleDateFormat(setFileDateFormat(ptrn))).parse(dateStr) 
                log.trace("Pattern found")
                true
            }
            catch(java.text.ParseException e)
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
    def parseOffset(def paramName, def offsetStr)
    {
        def date = null
        log.trace("Applying format#1 by default: " + defaultDateFormat.toPattern())
        setFileDateFormat(defaultDateFormat.toPattern())
        def minutesMatcher = offsetStr =~ /.(\d*)/
        if (offsetStr.size() > 1 && minutesMatcher.find())
        {
            log.trace("Total minutes: ${minutesMatcher.group(1)}")
            def minutes = Integer.valueOf(minutesMatcher.group(1))*60*1000
            def curDate = getParam(paramName) != null ? getParam(paramName) : new Date()
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


    def parseDate(def paramName, def dateStr) {
        def matcher = dateStr =~ /^[+-]/
        if (!matcher.find()) {
            log.trace("Usual dates parsing")
            setParam(paramName, parseInput(dateStr))
        }
        else
        {
            log.trace("Offset dates parsing")
            setParam(paramName, parseOffset(offsetParamMap[paramName],dateStr))
        }
    }
	/**
	 * Parses and sets supplied string as FROM_DATE parameter.
	 * 
	 * @param date string to be parsed
	 */
    void setDateFrom(String date)
    {
        parseDate('FROM_DATE', date)
        isFromParsed = true
    }

	/**
	 * Parses and sets supplied string as TO_DATE parameter.
	 *
	 * @param date string to be parsed
	 */
    void setDateTo(String date)
    {
        parseDate('TO_DATE', date)
        isToParsed = true
    }

	/**
	 * Sets matched INPUT_DATE_PTTRN as FILE_DATE_FORMAT parameter. <br>
	 * It identifies how far log file date will be truncated upon comparision.
	 * 
	 * @param format
	 * @return format back
	 */
    def setFileDateFormat(def format)
    {
        log.trace("FILE_DATE_FORMAT set to " + format)
        setParam('FILE_DATE_FORMAT', format)
        return format
    }

}
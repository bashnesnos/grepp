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
            unsubscribe()
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
	 * Parses and sets supplied string as FROM_DATE parameter.
	 * 
	 * @param date string to be parsed
	 */
    void setDateFrom(String date)
    {
        if (date != "+") setParam('FROM_DATE', parseInput(date))
        isFromParsed = true
    }

	/**
	 * Parses and sets supplied string as TO_DATE parameter.
	 *
	 * @param date string to be parsed
	 */
    void setDateTo(String date)
    {
        if (date != "+") setParam('TO_DATE', parseInput(date))
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
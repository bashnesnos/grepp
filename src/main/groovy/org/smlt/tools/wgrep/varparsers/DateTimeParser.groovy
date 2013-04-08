package org.smlt.tools.wgrep.varparsers

import java.text.SimpleDateFormat
import org.smlt.tools.wgrep.WgrepConfig
import groovy.util.logging.Slf4j;
import groovy.xml.dom.DOMCategory

@Slf4j
class DateTimeParser extends ParserBase
{
    private def INPUT_DATE_PTTRNS = []
    private boolean isFromParsed = false
    private boolean isToParsed = false

    DateTimeParser(WgrepConfig config)
    {
		super(config)
		def dt_tag = getParam('DATE_TIME_FILTER')
        if (dt_tag == null) throw new IllegalArgumentException("There should be some date time tag specified")
        use(DOMCategory)
        {
            def ptrns = getRoot().date_time_config.pattern.findAll { it.'@tags' =~ dt_tag }
            ptrns.sort { it.'@order' }.each {INPUT_DATE_PTTRNS.add(it.text())}
        }
    }

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

    def setDateFrom(def date)
    {
        if (date != "+") setParam('FROM_DATE', parseInput(date))
        isFromParsed = true
    }

    def setDateTo(def date)
    {
        if (date != "+") setParam('TO_DATE', parseInput(date))
        isToParsed = true
    }

    def setFileDateFormat(def format)
    {
        log.trace("FILE_DATE_FORMAT set to " + format)
        setParam('FILE_DATE_FORMAT', format)
        return format
    }

}
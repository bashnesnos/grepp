package org.smlt.tools.wgrep

import java.text.SimpleDateFormat
import groovy.xml.dom.DOMCategory

class DateTimeChecker extends ModuleBase
{
    //Checking dates everywhere

    SimpleDateFormat FILE_DATE_FORMAT = null
    SimpleDateFormat LOG_DATE_FORMAT = null
    def LOG_DATE_PATTERN = null
    def INPUT_DATE_PTTRNS = []
    Date FROM_DATE = null
    Date TO_DATE = null
    int LOG_FILE_THRESHOLD = 24
    int LOG_FILE_THRESHOLD_MLTPLR = 60*60*1000

    DateTimeChecker(def dt_tag)
    {
        if (dt_tag == null)
        {
            dt_tag = getFacade().getParam('DATE_TIME_FILTER')
        }
        use(DOMCategory)
        {
            def ptrns = getRoot().date_time_config.pattern.findAll { it.'@tags' =~ dt_tag }
            ptrns.sort { it.'@order' }.each {INPUT_DATE_PTTRNS.add(it.text())}
        }
        parseExtra()
    }

    def parseExtra()
    {
        def ptrn = getFacade().getExtraParam('LOG_DATE_PATTERN') 
        if (ptrn != null) LOG_DATE_PATTERN = ptrn
        def frmt = getFacade().getExtraParam('LOG_DATE_FORMAT') 
        if (frmt != null) LOG_DATE_FORMAT = new SimpleDateFormat(frmt)
        def trshld = getFacade().getExtraParam('LOG_FILE_THRESHOLD') 
        if (trshld != null) LOG_FILE_THRESHOLD = Integer.valueOf(trshld)
        setDateFrom(getFacade().getExtraParam('FROM_DATE'))
        setDateTo(getFacade().getExtraParam('TO_DATE'))
    }

    def setDateFrom(def date)
    {
        if (date != "+") FROM_DATE = parseInput(date)
    }

    def setDateTo(def date)
    {
        if (date != "+") TO_DATE = parseInput(date)
    }

    def setFileDateFormat(def format)
    {
        if (isTraceEnabled()) trace("FILE_DATE_FORMAT set to " + format)
        FILE_DATE_FORMAT = new SimpleDateFormat(format)
        return format
    }

    def setLogDateFormat(def format)
    {
        LOG_DATE_FORMAT = new SimpleDateFormat(format)
    }

    def setLogDatePattern(def val)
    {
        LOG_DATE_PATTERN = val
    }

    def parseInput(def dateStr)
    {
        def date = null
        INPUT_DATE_PTTRNS.find { ptrn -> 
            if (isTraceEnabled()) trace("trying date pattern="+ ptrn); 
            try {
                date = (new SimpleDateFormat(setFileDateFormat(ptrn))).parse(dateStr) 
                if (isTraceEnabled()) trace("Pattern found")
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

    def check(def data)
    {
        if (data instanceof File) checkFileTime(data)
        else if (data instanceof String) checkEntryTime(data)
    }

    /**
    * Facade method to check if supplied filename, and corresponding {@link File} object suits desired date and time. 
    * Calls {@link dtChecker.check()} method if {@link DATE_TIME_FILTER} is not null.
    *
    * @param fName A String with filename
    */

    def checkFileTime(def file)
    {
        if (file) if (isTraceEnabled()) trace("Checking file " + file.getName() + " if it suits " + FILE_DATE_FORMAT.format(FROM_DATE))
        if (TO_DATE) if (isTraceEnabled()) trace(" and " +  FILE_DATE_FORMAT.format(TO_DATE))

        def fileTime = new Date(file.lastModified())
        if (isTraceEnabled()) trace("fileTime:" + FILE_DATE_FORMAT.format(fileTime))
        if (FROM_DATE.compareTo(fileTime) <= 0)
        {
            if (TO_DATE)
            {
                if (TO_DATE.compareTo(fileTime) >= 0)
                {
                    return true
                }
                if (TO_DATE.compareTo(fileTime) < 0)
                {
                    if (isTraceEnabled()) trace("Passed TO_DATE")
                    if (fileTime.before(new Date(TO_DATE.getTime() + LOG_FILE_THRESHOLD*LOG_FILE_THRESHOLD_MLTPLR))) return file
                    else
                    {
                        if (isTraceEnabled()) trace("File is too far")
                        return false
                    }
                }
            }
            if (isTraceEnabled()) trace("Passed FROM_DATE only")
            return true
        }
        else
        {
            if (isTraceEnabled()) trace("Not passed")
            return false
        }
    }

    /**
    * Facade method to check if supplied entry suits desired date and time. 
    * Calls {@link dtChecker.check()} method if {@link DATE_TIME_FILTER} and <code>entry</code> are not null.
    *
    * @param entry A String to be checked
    */
    
    def checkEntryTime(def entry)
    {
        if (entry && LOG_DATE_PATTERN)
        {
            if (isTraceEnabled()) trace("Checking log entry " + entry + " for log date pattern |" + LOG_DATE_PATTERN + "| and formatting to |" +  LOG_DATE_FORMAT.toPattern() + "|")
            def entryDate =  LOG_DATE_FORMAT.parse((entry =~ LOG_DATE_PATTERN)[0])
            if (FROM_DATE.compareTo(entryDate) <= 0)
            {
                if (TO_DATE)
                {
                    if (TO_DATE.compareTo(entryDate) >= 0)
                    {
                        if (isTraceEnabled()) trace("Passed TO_DATE")
                        return true
                    }
                    else
                    {
                        if (isTraceEnabled()) trace("Not passed")
                        return false
                    }
                }
                if (isTraceEnabled()) trace("Passed FROM_DATE only")
                return true
            }
            else
            {
                if (isTraceEnabled()) trace("Not passed")
                return false
            }
        }
        return true
    }

}
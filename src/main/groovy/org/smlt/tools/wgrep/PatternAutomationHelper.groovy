package org.smlt.tools.wgrep

import groovy.xml.dom.DOMCategory

class PatternAutomationHelper extends ModuleBase
{

    def ATMTN_SEQ = []
    def ATMTN_DICT = [:]
    def currentConfigPtrn = null
    boolean isAmmended = false

    static PatternAutomationHelper getInstance(){
        return getFacade().getParam('ATMTN_LEVEL') != null ? new PatternAutomationHelper(getFacade().getParam('ATMTN_LEVEL')) : null
    }

    private PatternAutomationHelper(def lv_tag)
    {
        if (lv_tag == null) throw new IllegalArgumentException("There should be some level specified")
        use(DOMCategory)
        {
            def levels = getRoot().automation.level.findAll { it.'@tags' =~ lv_tag}.sort {it.'@order'}
            levels.each { ATMTN_SEQ.add(it.'@handler'); ATMTN_DICT[it.'@handler'] = it.'@id' }
        }
    }        

    boolean applySequenceByFileName(def filename)
    {
        def tag = null
        isAmmended = false
        if (currentConfigPtrn != null)
        {
            if (filename =~ currentConfigPtrn) return isAmmended
        }

        ATMTN_SEQ.each { handler ->
            tag = parse(ATMTN_DICT[handler], filename, tag, handler)
        }
        return isAmmended
    }

    boolean applySequenceByTag(def tag)
    {
        isAmmended = false
        ATMTN_SEQ.each { handler ->
            parse(ATMTN_DICT[handler], null, tag, handler)
        }
        return isAmmended
    }

    def parse(def level, def data, def tag_, def method)
    {
        if (isTraceEnabled()) trace('Identifying pattern for level=' + level + ' data=' + data + ' and tag=' + tag_ + ' with method=' + method)
        def tag = (tag_ != null) ? tag_ : findConfigByData(level, data)
        if (tag == null)  
        {
            throw new IllegalArgumentException("Failed to identify tag")
        }
        return this."$method"(tag)
    }

    def parseEntryConfig(def tag)
    {
        use(DOMCategory)
        {
            def customCfg = getRoot().custom.config.find { it.'@id' ==~ tag }
            if (customCfg != null)
            {
                if (isTraceEnabled()) trace('Parsing entry config for ' + tag)
                
                def starter = getCDATA(customCfg.starter[0])
                def date = getCDATA(customCfg.date[0])
                if (starter != null || date != null)
                {
                    getFacade().setLogEntryPattern( ((starter != null) ? starter : "") + ((date != null) ? date : "" ) )   
                }
                else
                {
                    throw new IllegalArgumentException("Either <starter> or <date> should filled for config: " + tag)
                }
                
                getFacade().setParam('LOG_DATE_PATTERN', customCfg.date.text())
                getFacade().setParam('LOG_DATE_FORMAT', customCfg.date_format.text())
                getFacade().setParam('LOG_FILE_THRESHOLD', customCfg.log_threshold.text())
                isAmmended = true
            }
            else
            {
                if (isTraceEnabled()) trace("Entry config is undefined")
            }
        }
        return tag
    }

    def parseFilterConfig(def tag)
    {
        use(DOMCategory)
        {
            if (isTraceEnabled()) trace('Parsing filter config for ' + tag)
            def customFilter = getRoot().custom.filters.filter.find { it.'@tags' =~ tag}
            if (customFilter != null)
            {
                getFacade().setFilterPattern(getCDATA(customFilter))
                isAmmended = true
            }
            else
            {
                if (isTraceEnabled()) trace("Filter is undefined")
            }
        }
        return tag
    }

    def parsePostFilterConfig(def tag)
    {
        getFacade().setParam('POST_PROCESSING', tag)
        isAmmended = true
        return tag
    }

    def parseBulkFilterConfig(def tag)
    {
        parseFilterConfig(tag)
        parsePostFilterConfig(tag)
    }

    def parseExecuteThreadConfig(def tag)
    {
        getFacade().setParam('PRESERVE_THREAD', tag)
        isAmmended = true
        return tag
    }

    def findConfigByData(def level, def data)
    {
        if (isTraceEnabled()) trace("findConfigByData started")

        if (data == null)
        {
            throw new IllegalArgumentException("Data shouldn't be null")
        }

        def tag = null
        use(DOMCategory)
        {
            def configs = getRoot().custom.config.findAll { it.pattern[0].'@alevel' ==~ level }
            def config = configs.find { config ->
                currentConfigPtrn = getCDATA(config.pattern[0])
                if (isTraceEnabled()) trace("ptrn=/" + currentConfigPtrn + "/ data='" + data + "'")
                data =~ currentConfigPtrn
            }
            if (config != null) tag = config.'@id'
        }
        return tag
    }

}
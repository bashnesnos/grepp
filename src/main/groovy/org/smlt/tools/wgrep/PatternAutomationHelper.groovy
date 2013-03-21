package org.smlt.tools.wgrep

import groovy.xml.dom.DOMCategory

class PatternAutomationHelper extends ModuleBase
{

    def ATMTN_SEQ = []
    def ATMTN_DICT = [:]
    def currentConfigPtrn = null

    static PatternAutomationHelper getInstance(){
        return getFacade().getParam('ATMTN_LEVEL') ? new PatternAutomationHelper() : null
    }

    private PatternAutomationHelper()
    {
        def lv_tag = getFacade().getParam('ATMTN_LEVEL')
        use(DOMCategory)
        {
            def levels = getRoot().automation.level.findAll { it.'@tags' =~ lv_tag}.sort {it.'@order'}
            levels.each { ATMTN_SEQ.add(it.'@handler'); ATMTN_DICT[it.'@handler'] = it.'@id' }
        }
        
        def predefnd = getFacade().getParam('PREDEF_TAG')
        if (predefnd)
        {
            automateByTag(predefnd)
        } 

        automateByFile(getFacade().getParam('FILES')[0])
    }

    def automateByFile(def filename)
    {
        def rslt = null
        if (currentConfigPtrn)
        {
            if (filename =~ currentConfigPtrn) return
        }

        ATMTN_SEQ.each {
            rslt = parse(ATMTN_DICT[it], filename, rslt, it)
        }
    }

    def automateByTag(def tag)
    {
        ATMTN_SEQ.each {
            parse(ATMTN_DICT[it], tag, tag, it)
        }
    }

    def parse(def level, def data, def tag_, def method)
    {
        if (isTraceEnabled()) trace('Identifying pattern for data=' + data + ' and tag=' + tag_ + ' with method=' + method)
        def tag = (tag_) ? tag_ : findConfigByFileName(level, data)
        if (!tag)  
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
            if (customCfg)
            {
                if (isTraceEnabled()) trace('Parsing entry config for ' + tag)
                getFacade().setLogEntryPattern(customCfg.starter.text() + customCfg.date.text())
                getFacade().setExtraParam('LOG_DATE_PATTERN', customCfg.date.text())
                getFacade().setExtraParam('LOG_DATE_FORMAT', customCfg.date_format.text())
                getFacade().setExtraParam('LOG_FILE_THRESHOLD', customCfg.log_threshold.text())
            }
            else
            {
                if (isTraceEnabled()) trace("Entry config is undefined")
            }
            def customFilter = getRoot().custom.filters.filter.find { it.'@tags' =~ tag}
            if (customFilter)
            {
                if (isTraceEnabled()) trace('Parsing filter config for ' + tag)
                getFacade().setFilterPattern(getCDATA(customFilter))
            }
            else
            {
                if (isTraceEnabled()) trace("Filter is undefined")
            }
        }
        return tag
    }

    def parseExecuteThreadConfig(def tag)
    {
        getFacade().setParam('PRESERVE_THREAD', tag)
        return tag
    }

    def findConfigByFileName(def level, def fileName)
    {
        if (isTraceEnabled()) trace("findConfigByFileName started")
        def tag = null
        use(DOMCategory)
        {
            def configs = getRoot().custom.config.findAll { it.pattern[0].'@alevel' ==~ level }
            def config = configs.find { config ->
                currentConfigPtrn = getCDATA(config.pattern[0])
                if (isTraceEnabled()) trace("ptrn=/" + currentConfigPtrn + "/ fileName='" + fileName + "'")
                fileName =~ currentConfigPtrn
            }
            if (config) tag = config.'@id'
        }
        return tag
    }

}
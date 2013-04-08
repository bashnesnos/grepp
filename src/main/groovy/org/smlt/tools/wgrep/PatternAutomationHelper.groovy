package org.smlt.tools.wgrep

import groovy.util.logging.Slf4j;
import groovy.xml.dom.DOMCategory

@Slf4j
class PatternAutomationHelper extends ModuleBase
{

    List ATMTN_SEQ = []
    Map ATMTN_DICT = [:]
    String currentConfigPtrn = null
    boolean isAmmended = false

    PatternAutomationHelper(WgrepConfig config)
    {
		super(config)
		def lv_tag = configInstance.getParam('ATMTN_LEVEL')
        if (lv_tag == null) throw new IllegalArgumentException("There should be some level specified")
        use(DOMCategory)
        {
            def levels = getRoot().automation.level.findAll { it.'@tags' =~ lv_tag}.sort {it.'@order'}
            levels.each { ATMTN_SEQ.add(it.'@handler'); ATMTN_DICT[it.'@handler'] = it.'@id' }
        }
    }        

    boolean applySequenceByFileName(String filename)
    {
        String tag = null
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

    boolean applySequenceByTag(String tag)
    {
        isAmmended = false
        ATMTN_SEQ.each { handler ->
            parse(ATMTN_DICT[handler], null, tag, handler)
        }
        return isAmmended
    }

    String parse(String level, String data, String tag_, String method)
    {
        log.trace('Identifying pattern for level=' + level + ' data=' + data + ' and tag=' + tag_ + ' with method=' + method)
        def tag = (tag_ != null) ? tag_ : findConfigTagByData(level, data)
        if (tag == null)  
        {
            throw new IllegalArgumentException("Failed to identify tag")
        }
        return this."$method"(tag)
    }

    String parseEntryConfig(String tag)
    {
        use(DOMCategory)
        {
            def customCfg = getRoot().custom.config.find { it.'@id' ==~ tag }
            if (customCfg != null)
            {
                log.trace('Parsing entry config for ' + tag)
                
                String starter = getCDATA(customCfg.starter[0])
                String date = getCDATA(customCfg.date[0])
                if (starter != null || date != null)
                {
                    setLogEntryPattern( ((starter != null) ? starter : "") + ((date != null) ? date : "" ) )   
                }
                else
                {
                    throw new IllegalArgumentException("Either <starter> or <date> should filled for config: " + tag)
                }
                
                setParam('LOG_DATE_PATTERN', customCfg.date.text())
                setParam('LOG_DATE_FORMAT', customCfg.date_format.text())
                setParam('LOG_FILE_THRESHOLD', customCfg.log_threshold.text())
                isAmmended = true
            }
            else
            {
                log.trace("Entry config is undefined")
            }
        }
        return tag
    }

    String parseFilterConfig(String tag)
    {
        use(DOMCategory)
        {
            log.trace('Parsing filter config for ' + tag)
            def customFilter = getRoot().custom.filters.filter.find { it.'@tags' =~ tag}
            if (customFilter != null)
            {
                setFilterPattern(getCDATA(customFilter))
                isAmmended = true
            }
            else
            {
                log.trace("Filter is undefined")
            }
        }
        return tag
    }

    String parsePostFilterConfig(String tag)
    {
        setParam('POST_PROCESSING', tag)
        isAmmended = true
        return tag
    }

    String parseBulkFilterConfig(String tag)
    {
        parseFilterConfig(tag)
        parsePostFilterConfig(tag)
    }

    String parseExecuteThreadConfig(String tag)
    {
        setParam('PRESERVE_THREAD', tag)
        isAmmended = true
        return tag
    }

    String findConfigTagByData(String level, String data)
    {
        log.trace("findConfigByData started")

        if (data == null)
        {
            throw new IllegalArgumentException("Data shouldn't be null")
        }

        String tag = null
        use(DOMCategory)
        {
            def configs = getRoot().custom.config.findAll { it.pattern[0].'@alevel' ==~ level }
            def config = configs.find { config ->
                currentConfigPtrn = getCDATA(config.pattern[0])
                log.trace("ptrn=/" + currentConfigPtrn + "/ data='" + data + "'")
                data =~ currentConfigPtrn
            }
            if (config != null) tag = config.'@id'
        }
        return tag
    }

}
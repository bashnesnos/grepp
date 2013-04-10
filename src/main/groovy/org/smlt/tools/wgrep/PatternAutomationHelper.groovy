package org.smlt.tools.wgrep

import groovy.util.logging.Slf4j;
import groovy.xml.dom.DOMCategory

/**
 * A helper class to provide automatic filter, log entry pattern identification. Currently supported identification by filename, or by specifying tag explicitly via option in config.xml
 *
 * @author Alexander Semelit 
 */

@Slf4j
class PatternAutomationHelper extends ModuleBase
{

    List ATMTN_SEQ = []
    Map ATMTN_DICT = [:]
    String currentConfigPtrn = null
    String currentConfigId = null
    boolean isAmmended = false
	String lv_tag

    /**
    * Constructor. Accepts WgrepConfig to initialize needed params. <br>
    * Parses automation method sequence from the config.xml depending on level tag as specified in <automation> section of the config.xml.
    */

    PatternAutomationHelper(WgrepConfig config)
    {
		super(config)
		lv_tag = getParam('ATMTN_LEVEL')
        use(DOMCategory)
        {
            def levels = getRoot().automation.level.findAll { it.'@tags' =~ lv_tag}.sort {it.'@order'}
            levels.each { ATMTN_SEQ.add(it.'@handler'); ATMTN_DICT[it.'@handler'] = it.'@id' }
        }
    }        
	
	/**
	 * Overriden method to check if needed params are fulfilled in the config.
	 * @return true if config has desired params, false otherwise. 
	 */
	
	@Override
    boolean isConfigValid() {
        boolean checkResult = super.isConfigValid()
        if (getParam('ATMTN_LEVEL') == null)
        {
            log.warn('ATMTN_LEVEL is not specified')
            checkResult = false
        }
        return checkResult
    }

	/**
	 * Applies automation sequence to the filename and tries to find <config> section which has matching pattern. <br>
	 * 
	 * 
	 * @param filename String representing file name
	 * @return true if config params were changed.
	 */
	
    boolean applySequenceByFileName(String filename)
    {
        String tag = null
        isAmmended = false
        if (currentConfigPtrn != null)
        {
            if (filename =~ currentConfigPtrn) return isAmmended
        }
		
        ATMTN_SEQ.each { handler ->
			currentConfigId = (currentConfigId != null) ? currentConfigId : findConfigIdByData(ATMTN_DICT[handler], filename)
			applyMethod(currentConfigId, handler)
        }
        return isAmmended
    }

	/**
	 * Applies automation sequence to the tag and tries to find <config> with id which matches the tag. <br>
	 * 
	 * @param tag String representing config tag as in config.xml
	 * @return true if config params were changed.
	 */
	
    boolean applySequenceByTag(String tag)
    {
        isAmmended = false
        if (currentConfigId != null)
        {
            if (tag ==~ currentConfigId) return isAmmended
        }

        ATMTN_SEQ.each { handler ->
            applyMethod(tag, handler)
        }
        return isAmmended
    }
	
	/**
	 * Applies method of PatternAutomationHelper to a via reflection. <br>
	 * 
	 * @param tag String representing tag to apply method for
	 * @param method String which should be name of a declared method in PatterAutomationHelper accepting tag
	 */
	
    void applyMethod(String tag, String method)
    {
        log.trace('Applying method=' + method + ' for tag=' + tag)
        if (tag == null)  
        {
            throw new IllegalArgumentException("Tag shouldn't be null")
        }
		if (method == null)
		{
			throw new IllegalArgumentException("Method shouldn't be null")
		}
        this."$method"(tag)
    }
	
	/**
	 * Parses <config> section with id equal to supplied tag. Depending on which elements are supplied it fills: <br>
	 * LOG_ENTRY_PATTERN <br>
	 * LOG_DATE_PATTERN <br>
	 * LOG_DATE_FORMAT <br>
	 * LOG_FILE_THRESHOLD <br>
	 * If custom config was found and params were filled, toggles isAmmended flag.
	 * 
	 * @param tag <config> section's id
	 */
	
    void parseCustomConfig(String tag)
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
                    log.warn("Either <starter> or <date> should be filled for config: " + tag)
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
    }
	
	/**
	 * Looks for <filter> element with "tags" parameter containing supplied tag. Method fills: <br>
	 * FILTER_PATTERN
	 * 
	 * @param tag One of "tags", which could be found in <filter> element.
	 */

    void parseFilterConfig(String tag)
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
    }
	
	/**
	 * Simply sets POST_PROCESSING to a supplied tag value
	 * 
	 * @param tag One of a <splitter> element tags
	 */
	
    void parsePostFilterConfig(String tag)
    {
        setParam('POST_PROCESSING', tag)
        isAmmended = true
    }
	
	/**
	 * Method applies parseFilterConfig and parsePostFilter to one tag.
	 * 
	 * @param tag One of "tags", which could be found in <filter> element which is at the same time one of a <splitter> element tags
	 */
	
    void parseBulkFilterConfig(String tag)
    {
        parseFilterConfig(tag)
        parsePostFilterConfig(tag)
    }

	/**
	 * Method simply sets PRESERVE_THREAD value to supplied tag
	 * 
	 * @param tag One of "tags", which could be found in <extractor> or <pattern> element
	 */
	
    void parseExecuteThreadConfig(String tag)
    {
        setParam('PRESERVE_THREAD', tag)
        isAmmended = true
    }

	/**
	 * Finds config id by specified String. Method looks up for <config> element containing matching <pattern> with "alevel" parameter equal to level.
	 * 
	 * @param level One of levels as specified in <automation> section of the config.xml
	 * @param data String which would be matched to <pattern> element values which have corresponding to level "alevel" parameter.
	 * @return
	 */
	
    String findConfigIdByData(String level, String data)
    {
        log.trace("findConfigByData started")

        if (data == null)
        {
            throw new IllegalArgumentException("Data shouldn't be null")
        }

        String id = null
        use(DOMCategory)
        {
            def configs = getRoot().custom.config.findAll { it.pattern[0].'@alevel' ==~ level }
            def config = configs.find { config ->
                currentConfigPtrn = getCDATA(config.pattern[0])
                log.trace("ptrn=/" + currentConfigPtrn + "/ data='" + data + "'")
                data =~ currentConfigPtrn
            }
            if (config != null) id = config.'@id'
        }
        return id
    }

}
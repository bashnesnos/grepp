package org.smlt.tools.wgrep.filters

import groovy.util.logging.Slf4j;
import groovy.xml.dom.DOMCategory
import java.util.regex.Matcher
import org.smlt.tools.wgrep.WgrepConfig
import org.smlt.tools.wgrep.filters.enums.Event
import org.smlt.tools.wgrep.filters.enums.Qualifier

@Slf4j
class PostFilter extends FilterBase {

    //Postprocessing stuff
    def PATTERN = new StringBuilder("(?ms)")
    def POST_PROCESS_PTTRNS = []
    def POST_PROCESS_SEP = null
    def POST_PROCESS_DICT = [:]
    def POST_PROCESS_HEADER = null
    def POST_GROUPS = [:]
    def currentGroup = null
    def groupMethod = null
    def POST_GROUPS_METHODS = []
    def HEADER_PRINTED = false

    PostFilter(FilterBase nextFilter_, WgrepConfig config)
    {
        super(nextFilter_, config)
		def pp_tag = getParam('POST_PROCESSING')
        if (pp_tag == null) throw new IllegalArgumentException("There should be some post processing tag specified")
        log.trace("Added on top of " + nextFilter.getClass().getCanonicalName())

        use(DOMCategory)
        {
            log.trace("Looking for splitters of type=" + pp_tag)
            def pttrns = getRoot().custom.pp_splitters.splitter.findAll { it.'@tags' =~ pp_tag}
            log.trace("Patterns found=" + pttrns)
            if (pttrns != null)
            {
                pttrns.sort { it.'@order' }
                pttrns.each { ptrn_node ->
                    String pttrn = getCDATA(ptrn_node)
                    setSeparator(ptrn_node.'@sep')
                    POST_PROCESS_PTTRNS.add(pttrn)
                    PATTERN.append(pttrn).append(Qualifier.and.getPattern())
                    def splitter_type = getRoot().pp_config.pp_splitter_types.splitter_type.find { sp_type -> sp_type.'@id' ==~ ptrn_node.'@type' }
                    def handler = splitter_type.'@handler'
                    POST_PROCESS_DICT[pttrn] = handler
                    if (splitter_type.'@handler_type' ==~ "group_method")
                    {
                        POST_GROUPS_METHODS.add(handler)
                    }
                    POST_PROCESS_HEADER = (POST_PROCESS_HEADER) ? POST_PROCESS_HEADER + POST_PROCESS_SEP + ptrn_node.'@col_name' : ptrn_node.'@col_name'
                }
                PATTERN.delete(PATTERN.length() - Qualifier.and.getPattern().length(), PATTERN.length()) //removing last and qualifier
            }
        }
    }


    void setSeparator(String sep_tag)
    {
        if (POST_PROCESS_SEP != null) return

        use(DOMCategory)
        {
                if (sep_tag != null && sep_tag != '') 
                {
                    POST_PROCESS_SEP = sep_tag
                }
                else 
                {
                    POST_PROCESS_SEP = getRoot().pp_config.'@default_sep'[0]
                }
                log.trace("Looking for separator=" + POST_PROCESS_SEP)
                
                def sep = getRoot().pp_config.pp_separators.separator.find { it.'@id' ==~ POST_PROCESS_SEP}
                POST_PROCESS_SEP = sep.text()
                if (sep.'@spool' != null) setSpoolingExt(sep.'@spool')
        }
    }

    /**
    * Method for post processing.
    * <p> 
    * Is called against each block.
    * 
    * @param blockData A String to be post processed.
    */

    def filter(def blockData)
    {
        StringBuilder rslt = null
        setPattern(PATTERN.toString())
        printHeader()
        Matcher postPPatternMatcher = blockData =~ filterPtrn
        if (postPPatternMatcher.find()) //bulk matching all patterns. If any is absent nothing will be returned
        {
            rslt = new StringBuilder("")
            POST_PROCESS_PTTRNS.each { ptrn -> rslt = smartPostProcess(postPPatternMatcher, rslt, POST_PROCESS_SEP, POST_PROCESS_DICT[ptrn], POST_PROCESS_PTTRNS.indexOf(ptrn) + 1)} //TODO: new handlers model is needed
        }

        if (rslt != null) 
        {
            super.filter(rslt.toString())
        }
        else
        {
            log.trace("not passed")
        }
    }

    StringBuilder smartPostProcess(Matcher mtchr, StringBuilder agg, String sep, String method, Integer groupIdx)
    {
        log.trace(new StringBuilder('smart post processing, agg=') + ' agg=' + agg + ' method=' + method + ' groupIdx=' + groupIdx)
        log.trace("mtch found")
        def mtchResult = this."$method"(mtchr, groupIdx)
        if (agg != null && mtchResult != null) //omitting printing since one of the results was null. Might be a grouping
        {
            return aggregatorAppend(agg, sep, mtchResult)
        }
        else
        {
            return null
        }
    }

    StringBuilder aggregatorAppend(StringBuilder agg, String sep, def val)
    {
        return (agg.size() > 0) ? agg.append(sep).append(val):agg.append(val)
    }

    def processPostFilter(Matcher mtchResults, def groupIdx)
    {
        return mtchResults.group(groupIdx)
    }

    def processPostCounter(Matcher mtchResults, def groupIdx)
    {
        String currentPattern = mtchResults.group(groupIdx)
        Matcher countableMatcher = mtchResults.group() =~ currentPattern
        if (countableMatcher.find()) {
            return countableMatcher.size()    
        }
        else
        {
            return 0
        }
        
    }

    def processPostGroup(Matcher mtchResults, def groupIdx)
    {
        String newGroup = mtchResults.group(groupIdx)
        Map existingGroup = POST_GROUPS[newGroup]
        if (existingGroup == null)
        {
            POST_GROUPS[newGroup] = [:]
            existingGroup = POST_GROUPS[newGroup]
        }
        currentGroup = existingGroup
        return null
    }

    def processPostAverage(Matcher mtchResults, def groupIdx)
    {
        Integer newIntVal = 0
        try {
            newIntVal = Integer.valueOf(mtchResults.group(groupIdx))            
        }
        catch(NumberFormatException e) {
            log.trace("attempting to count current group")
            newIntVal = processPostCounter(mtchResults, groupIdx)
        }

        List<Integer> averageAgg = currentGroup["averageAgg"]
        if (averageAgg != null)
        {
            averageAgg.add(newIntVal)
        }
        else
        {
            currentGroup["averageAgg"] = [newIntVal]
        }
        log.trace ("added new val: " + newIntVal)
        return null
    }

    def processPostAverage(Map group)
    {
        log.trace ("average group: " + group)
        List<Integer> averageAgg = group["averageAgg"]
        if (averageAgg == null || averageAgg.size() == 0) return 0
        Integer sum = 0
        averageAgg.each { Integer val ->
            sum += val
        }
        return sum/averageAgg.size()
    }

    def processGroups()
    {
        POST_GROUPS.each { group ->
            StringBuilder rslt = new StringBuilder(group.getKey())
            POST_GROUPS_METHODS.each { method ->
                rslt = aggregatorAppend(rslt, POST_PROCESS_SEP, this."$method"(group.getValue()))
            }
            super.filter(rslt.toString())
        }
    }

    def printHeader()
    {
        if (!HEADER_PRINTED) 
        {   
            HEADER_PRINTED = true
            nextFilter.filter(POST_PROCESS_HEADER) //if next filter won't allow to pass the header, that's ok. Flexibility though.
        }
    }

    def processEvent(def event) {
        switch (event)
        {
            case Event.ALL_FILES_PROCESSED:
                processGroups()
                break
            default:
                break
        }
        super.processEvent(event)
    }

}
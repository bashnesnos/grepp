package org.smlt.tools.wgrep

import groovy.xml.dom.DOMCategory
import java.util.regex.Matcher

class PostFilter extends ModuleBase
{
    private nextFilter
    //Postprocessing stuff
    def POST_PROCESS_PTTRNS = []
    def POST_PROCESS_SEP = null
    def POST_PROCESS_DICT = [:]
    def POST_PROCESS_HEADER = null
    def HEADER_PRINTED = false

    PostFilter(def nextOne)
    {
        nextFilter = nextOne
        if (isTraceEnabled()) trace("Added on top of " + nextFilter.getClass().getCanonicalName())

        def pp_tag = getFacade().getParam('POST_PROCESSING')
        use(DOMCategory)
        {
            if (isTraceEnabled()) trace("Looking for splitters of type=" + pp_tag)
            def pttrns = getRoot().custom.pp_splitters.splitter.findAll { it.'@tags' =~ pp_tag}
            if (isTraceEnabled()) trace("Patterns found=" + pttrns)
            if (pttrns != null)
            {
                pttrns.sort { it.'@order' }
                pttrns.each { ptrn_node ->
                    def pttrn = getCDATA(ptrn_node)
                    setSeparator(ptrn_node.'@sep')
                    POST_PROCESS_PTTRNS.add(pttrn);
                    POST_PROCESS_DICT[pttrn] = (getRoot().pp_config.pp_splitter_types.splitter_type.find { sp_type -> sp_type.'@id' ==~ ptrn_node.'@type' }).'@handler'
                    POST_PROCESS_HEADER = (POST_PROCESS_HEADER) ? POST_PROCESS_HEADER + POST_PROCESS_SEP + ptrn_node.'@col_name' : ptrn_node.'@col_name'
                }
            }
        }
    }


    def setSeparator(def sep_tag)
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
                if (isTraceEnabled()) trace("Looking for separator=" + POST_PROCESS_SEP)
                
                def sep = getRoot().pp_config.pp_separators.separator.find { it.'@id' ==~ POST_PROCESS_SEP}
                POST_PROCESS_SEP = sep.text()
                if (sep.'@spool' != null) getFacade().setSpoolingExt(sep.'@spool')
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
        StringBuilder rslt = new StringBuilder("")
        if (!HEADER_PRINTED) 
        {   
            HEADER_PRINTED = true
            getFacade().printBlock(POST_PROCESS_HEADER)
        }
        POST_PROCESS_PTTRNS.each { ptrn -> rslt = smartPostProcess(ptrn, blockData, rslt, POST_PROCESS_SEP, POST_PROCESS_DICT[ptrn])} //TODO: new handlers model is needed
        def result = rslt.toString()
        
        if (result != null) 
        {
            if (nextFilter != null)
            {
                nextFilter.filter(result)
            }
            else
            {
                throw new RuntimeException("PostFilter shouldn't be the last in chain")
            }
        }
        else
        {
            if (isTraceEnabled()) trace("PostFilter not passed")
        }
    }

    def smartPostProcess(def ptrn, def val, def agg, def sep, def method)
    {
        if (isTraceEnabled()) trace(new StringBuilder('smart post processing, ptrn=') + ptrn + ' val=' + val + ' agg=' + agg + ' method=' + method)
        Matcher mtch = (val =~ ptrn)
        if (mtch.find()) return (agg.size() > 0)?agg.append(sep).append(this."$method"(mtch)):agg.append(this."$method"(mtch))
        return agg
    }

    def processPostFilter(Matcher mtchResults)
    {
        return mtchResults.group(1)
    }

    def processPostCounter(Matcher mtchResults)
    {
        return mtchResults.size()
    }
}
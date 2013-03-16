package org.smlt.tools.wgrep

import groovy.xml.dom.DOMCategory

class PostProcessor extends FacadeBase
{
    //Postprocessing stuff
    def POST_PROCESS_PTTRNS = []
    def POST_PROCESS_SEP = null
    def POST_PROCESS_DICT = [:]
    def POST_PROCESS_HEADER = null
    def HEADER_PRINTED = null

    PostProcessor(def pp_tag)
    {
        setCallingClass(this.getClass())
        if (!pp_tag)
        {
            pp_tag = getFacade().getParam('POST_PROCESSING')
        }
        use(DOMCategory)
        {
            trace("Looking for splitters of type=" + pp_tag)
            def pttrns = getRoot().custom.pp_splitters.splitter.findAll { it.'@tags' =~ pp_tag}
            trace("Patterns found=" + pttrns)
            if (pttrns)
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
        if (POST_PROCESS_SEP) return
        use(DOMCategory)
        {
                if (sep_tag) POST_PROCESS_SEP = sep_tag
                else POST_PROCESS_SEP = getRoot().pp_config.'@default_sep'[0]
                trace("Looking for separator=" + POST_PROCESS_SEP)
                def sep = getRoot().pp_config.pp_separators.separator.find { it.'@id' ==~ POST_PROCESS_SEP}
                POST_PROCESS_SEP = sep.text()
                if (sep.'@spool') getFacade().setSpoolingExt(sep.'@spool')
        }
    }

    def process(def data)
    {
        StringBuffer rslt = new StringBuffer("")
        if (!HEADER_PRINTED) 
        {   
            HEADER_PRINTED = 1
            getFacade().printBlock(POST_PROCESS_HEADER)
        }
        POST_PROCESS_PTTRNS.each { ptrn -> rslt = smartPostProcess(ptrn, data, rslt, POST_PROCESS_SEP, POST_PROCESS_DICT[ptrn])} //TODO: new handlers model is needed
        return rslt.toString()
    }

    def smartPostProcess(def ptrn, def val, def agg, def sep, def method)
    {
        trace(new StringBuffer('smart post processing, ptrn=') + ptrn + ' val=' + val + ' agg=' + agg + ' method=' + method)
        def mtch = (val =~ ptrn)
        if (mtch) return (agg.size() > 0)?agg.append(sep).append(this."$method"(mtch)):agg.append(this."$method"(mtch))
        return agg
    }

    def processPostFilter(def mtchResults)
    {
        return mtchResults.group(1)
    }

    def processPostCounter(def mtchResults)
    {
        return mtchResults.size()
    }
}
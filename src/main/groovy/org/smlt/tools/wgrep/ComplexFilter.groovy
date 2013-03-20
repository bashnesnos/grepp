package org.smlt.tools.wgrep

import groovy.xml.dom.DOMCategory

class ComplexFilter extends ModuleBase
{
    private nextFilter
    //Complex pattern processing and stuff
    def EXTNDD_PTTRNS = []
    def EXTNDD_PTTRN_DICT = [:]
    def EXTNDD_QUALIFIERS = []

    def THRD_START_EXTRCTRS =[:]
    def THRD_START_PTTRNS = []
    def THRD_SKIP_END_PTTRNS = []
    def THRD_END_PTTRNS =[]

    ComplexFilter(def nextOne)
    {
        nextFilter = nextOne
        if (isTraceEnabled()) trace("Added on top of " + nextFilter.getClass().getCanonicalName())
        def pt_tag = getFacade().getParam('PRESERVE_THREAD')
        use(DOMCategory)
        {
            getRoot().extended_patterns.qualifier.each{ EXTNDD_QUALIFIERS.add(Qualifiers.valueOf(it.'@id'))}
            if (pt_tag)
            {
                def extrctrs = getRoot().custom.thread_configs.extractor.findAll { it.'@tags' =~ pt_tag }
                extrctrs.each { THRD_START_EXTRCTRS[it.text()] = it.'@qlfr' }
                def pttrns = getRoot().custom.thread_configs.pattern.findAll { it.'@tags' =~ pt_tag } 
                pttrns.each { this."${it.'@clct'}".add(it.text()) }
            }
        }
        processExtendedPattern(getFacade().getParam('FILTER_PATTERN'))
    }

    def addExtendedFilterPattern(def val, def qualifier)
    {
      if (isTraceEnabled()) trace("adding complex pattern: val=" + val + " qual=" + qualifier)
      EXTNDD_PTTRNS.add(val)
      EXTNDD_PTTRN_DICT[val] = qualifier ? Qualifiers.valueOf(qualifier) : null
      if (isTraceEnabled()) trace(EXTNDD_PTTRNS)
      if (isTraceEnabled()) trace(EXTNDD_PTTRN_DICT)
    }

    def removeExtendedFilterPattern(def val)
    {
      EXTNDD_PTTRNS.remove(val)
      EXTNDD_PTTRN_DICT.remove(val)
    }

    def processExtendedPattern(def val)
    {
        def filterPattern = null
        def qRegex = ""
        EXTNDD_QUALIFIERS.each { qRegex += '%' + it + '%|' }
        qRegex = qRegex[0..qRegex.size()-2] //removing last |
        def mtch = (val =~ /$qRegex/)
        if (mtch.size() > 0)
        {
            if (isTraceEnabled()) trace('Processing complex pattern')
            mtch = val.tokenize("%")
            def nextQualifier = null
            if (mtch)
            {
                qRegex = qRegex.replaceAll(/%/, "")
                for (grp in mtch)
                {
                    if (isTraceEnabled()) trace('Next group in match: ' + grp)
                    def qualifier = (grp =~ /$qRegex/)
                    if (qualifier)
                    {
                        nextQualifier = qualifier[0]
                        continue
                    }

                    addExtendedFilterPattern(grp, nextQualifier)
                    nextQualifier = null

                }
            }
            else throw new IllegalArgumentException('Check your complex pattern:/' + val + '/')
        }
        else 
        {
            if (isTraceEnabled()) trace('No extended pattern supplied, might be a preserve thread')
            addExtendedFilterPattern(val, null)
        }
    }

    /**
    * Method for complex pattern processing.
    * <p> 
    * Is called against each block.
    *
    * @param blockData A String to be filtered.
    *
    */

    def filter(def blockData)
    {
        if (process(blockData, null))
        {
            if (nextFilter)
            {
                nextFilter.filter(blockData)
            }
            else
            {
                throw new RuntimeException("ComplexFilter shouldn't be the last in chain")
            }
        }
        else 
        {
            if (isTraceEnabled()) trace("ComplexFilter not passed")
        }
    }

    def process(def blockData, def matched)
    {
       return process(blockData, EXTNDD_PTTRNS, matched)
    }

    def process(def blockData, def patterns, def matched)
    {
        if (isTraceEnabled()) trace('Data: ' + blockData)
        if (isTraceEnabled()) trace('List of patterns: ')
        if (isTraceEnabled()) trace(patterns)
        if (isTraceEnabled()) trace('Is matched? ' + matched)    

        def pattern = patterns[0]
        def qlfr = EXTNDD_PTTRN_DICT[pattern]
        def ptrnMatcher = blockData =~ pattern
        def hasMorePatterns = patterns.size()-1 > 0

        if ((qlfr && qlfr.check(matched, ptrnMatcher)) || (!qlfr && ptrnMatcher))
        {
            if (isTraceEnabled()) trace('Proceeding')
            if (!hasMorePatterns)
            {
                if (isTraceEnabled()) trace('Returning data')
                return extractThreadPatterns(blockData)
            }
            else
            {
                if (isTraceEnabled()) trace('Going deeper')
                return process(blockData, patterns[1..(patterns.size()-1)], 1)
            }
        }
        else
        {
            if (isTraceEnabled()) trace('Nothing matched.')
            if (!hasMorePatterns)
            {
                if (isTraceEnabled()) trace('Returning null since no patterns')
                return
            }
            else
            {
                if (isTraceEnabled()) trace('Going deeper')
                return process(blockData, patterns[1..(patterns.size()-1)], null)
            }
        }

        if (isTraceEnabled()) trace('This return shouldn\'t be reachable')
        return
    }

    def extractThreadPatterns(def data)
    {
        if (getFacade().PRESERVE_THREAD)
        {
            if (searchThreadEnds(data)) 
            { 
                extractThreadStarts(data, "removeThreadStart")
            }
            else
            {
                if (isTraceEnabled()) trace("Thread continues. Keeping starts")
                extractThreadStarts(data, "addThreadStart")
            }
        }
        return data
    }

    def extractThreadStarts(def data, def method)
    {
        THRD_START_EXTRCTRS.each
        {extrctr, qlfr -> 
            if (isTraceEnabled()) trace(extrctr);
            def srch = (data =~ extrctr);
            if (srch)
            {
                def start = srch[0]
                if (isTraceEnabled()) trace("extracted; " + start)
                this."$method"(start, qlfr)
            }
        }
        return
    }

    def searchThreadEnds(def data)
    {
        if (!shouldBeSkipped(data))
        {
            def decision = THRD_END_PTTRNS.find
            { thrend ->
                if (isTraceEnabled()) trace("thrend ptrn: " + thrend);
                data =~ thrend
            }
            return decision
        }
        return false
    }

    def shouldBeSkipped(def data)
    {
        def decision = THRD_SKIP_END_PTTRNS.find
        {skip->
            if (isTraceEnabled()) trace("skip ptrn: " + skip)
            data =~ skip
        }
        return decision
    }

    def addThreadStart(def start, def qlfr)
    {
      if (isTraceEnabled()) trace("adding thread start: " + start);
      if (!THRD_START_PTTRNS.contains(start))
      {
        THRD_START_PTTRNS.add(start)
        addExtendedFilterPattern(start, qlfr)
      }
      else if (isTraceEnabled()) trace("Start exists")
    }

    def removeThreadStart(def start, def qlfr)
    {
      if (isTraceEnabled()) trace("removing thread start: " + start);
      THRD_START_PTTRNS.remove(start);
      removeExtendedFilterPattern(start);
    }

}
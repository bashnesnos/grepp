package org.smlt.tools.wgrep

import java.util.regex.Matcher
import groovy.xml.dom.DOMCategory

class ComplexFilter extends ModuleBase
{
    private nextFilter
    //Complex pattern processing and stuff
    def PATTERN = new StringBuffer()
    def EXTNDD_PTTRNS = []
    def EXTNDD_PTTRN_DICT = [:]

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
            if (pt_tag != null)
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
      
      if (qualifier != null) PATTERN = PATTERN.append(Qualifier.valueOf(qualifier).getPattern())
      PATTERN = PATTERN.append(val)
      
      EXTNDD_PTTRNS.add(val)
      EXTNDD_PTTRN_DICT[val] = qualifier ? Qualifier.valueOf(qualifier) : null
      
      if (isTraceEnabled()) trace(EXTNDD_PTTRNS)
      if (isTraceEnabled()) trace(EXTNDD_PTTRN_DICT)
    }

    def removeExtendedFilterPattern(def val)
    {
      def qlfr = EXTNDD_PTTRN_DICT[val]
      def ptrn = (qlfr ? qlfr.getPattern() : '') + val
      def ptrnIndex = PATTERN.indexOf(ptrn)
      if (isTraceEnabled()) trace('to delete:/' + ptrn +'/ index:' + ptrnIndex)
      PATTERN = PATTERN.delete(ptrnIndex, ptrnIndex + ptrn.length())
      EXTNDD_PTTRNS.remove(val)
      EXTNDD_PTTRN_DICT.remove(val)
    }

    def processExtendedPattern(def val)
    {
        def filterPattern = null
        def qRegex = ""
        Qualifier.each { qRegex += '%' + it + '%|' }
        qRegex = qRegex[0..qRegex.size()-2] //removing last |
        def mtch = (val =~ /$qRegex/)
        if (mtch.find())
        {
            if (isTraceEnabled()) trace('Processing complex pattern')
            mtch = val.tokenize("%")
            def nextQualifier = null
            if (mtch != null)
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
        def curPattern = PATTERN.toString()
        if (isTraceEnabled()) 
        {
            trace('Data: ' + blockData + '\n' +
                'List of patterns:\n ' +    
                curPattern + '\n')
        }

        def blockMtchr = blockData =~ curPattern
        if (blockMtchr.find())
        {
            extractThreadPatterns(blockData)
            if (nextFilter != null)
            {
                if (isTraceEnabled()) trace('Returning data')
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

    def extractThreadPatterns(def data)
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
            return decision != null
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
        return decision != null
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
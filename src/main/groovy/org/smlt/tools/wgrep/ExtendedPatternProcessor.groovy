package org.smlt.tools.wgrep

import groovy.xml.dom.DOMCategory

class ExtendedPatternProcessor extends FacadeBase
{
    //Complex pattern processing and stuff
    def EXTNDD_PTTRNS = []
    def EXTNDD_PTTRN_DICT = [:]
    def EXTNDD_QUALIFIERS = []

    def THRD_START_EXTRCTRS =[:]
    def THRD_START_PTTRNS = []
    def THRD_SKIP_END_PTTRNS = []
    def THRD_END_PTTRNS =[]

    ExtendedPatternProcessor(def pt_tag)
    {
        setCallingClass(this.getClass())
        if (!pt_tag)
        {
            pt_tag = getFacade().getParam('PRESERVE_THREAD')
        }
        use(DOMCategory)
        {
            getRoot().extended_patterns.qualifier.each{ EXTNDD_QUALIFIERS.add(it.'@id')}
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
      trace("adding complex pattern: val=" + val + " qual=" + qualifier)
      EXTNDD_PTTRNS.add(val)
      EXTNDD_PTTRN_DICT[val] = qualifier
      trace(EXTNDD_PTTRNS)
      trace(EXTNDD_PTTRN_DICT)
    }

    def removeExtendedFilterPattern(def val)
    {
      EXTNDD_PTTRNS.remove(val)
      EXTNDD_PTTRN_DICT.remove(val)
    }

    def processExtendedPattern(def val)
    {
        def filterPattern = val
        def qRegex = ""
        EXTNDD_QUALIFIERS.each { qRegex += '%' + it + '%|' }
        qRegex = qRegex[0..qRegex.size()-2] //removing last |
        def mtch = (filterPattern =~ /$qRegex/)
        if (mtch.size() > 0)
        {
            trace('Processing complex pattern')
            mtch = val.tokenize("%")
            filterPattern = null
            def nextGroup = null
            if (mtch)
            {
                for (grp in mtch)
                {
                    trace('Next group in match: ' + grp)
                    if (!filterPattern)
                    {
                        filterPattern = grp
                        continue
                    }
                    qRegex = qRegex.replaceAll(/%/, "")
                    def qualifier = (grp =~ /$qRegex/)
                    if (qualifier)
                    {
                        nextGroup = qualifier[0]
                        continue
                    }
                    if (nextGroup)
                    {
                        addExtendedFilterPattern(grp, nextGroup)
                        nextGroup = null
                    }
                }
            }
            else print 'Check your complex pattern. Likely it\'s wrong'
        }

        getFacade().setParam('FILTER_PATTERN', filterPattern)
    }

    def process(def blockData, def matched)
    {
       return process(blockData, EXTNDD_PTTRNS, matched)
    }

    def process(def blockData, def patterns, def matched)
    {
        trace('Data: ' + blockData)
        trace('List of patterns: ')
        trace(patterns)
        trace('Is matched? ' + matched)    
        if (matched) extractThreadPatterns(blockData)

        def pattern = patterns[0]
        def qlfr = EXTNDD_PTTRN_DICT[pattern]

        if (matched && qlfr == 'and')
        {
            trace('Complex pattern \'and\'. Previous matched')
            if (blockData =~ pattern)
            {
                trace(pattern + ' matched.')
                if (patterns.size()-1 > 0)
                {
                    trace('Going deeper')
                    process(blockData, patterns[1..(patterns.size()-1)], 1)
                }
                else
                {
                    trace('Returning')
                    return extractThreadPatterns(blockData)
                }
            }
            else if ((patterns.size()-1) > 0)
            {
                trace(pattern + ' not matched. Going deeper')
                process(blockData, patterns[1..(patterns.size()-1)], null)
            }
            else
            {
                trace(pattern + ' not matched. Returning')
                return
            }
        }
        else if (qlfr == 'or')
        {
            trace('Complex pattern \'or\'.')
            if (matched)
            {
                trace('Previous matched.')
                return extractThreadPatterns(blockData)
            }
            if (blockData =~ pattern)
            {
                trace(pattern + ' matched.')
                if (patterns.size()-1 > 0)
                {
                    trace('Going deeper')
                    process(blockData, patterns[1..(patterns.size()-1)], 1)
                }
                else
                {
                    trace('Returning')
                    return extractThreadPatterns(blockData)
                }
            }
            else if (patterns.size()-1 > 0)
            {
                trace(pattern + ' not matched. Going deeper')
                process(blockData, patterns[1..(patterns.size()-1)], null)
            }
        }
        else
        {
            trace(pattern + ' not matched. Returning')
            return
        }

        trace('All patterns checked. Returning')
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
                trace("Thread continues. Keeping starts")
                extractThreadStarts(data, "addThreadStart")
            }
        }
        return data
    }

    def extractThreadStarts(def data, def method)
    {
        THRD_START_EXTRCTRS.each
        {extrctr, qlfr -> 
            trace(extrctr);
            def srch = (data =~ extrctr);
            if (srch)
            {
                def start = srch[0]
                trace("extracted; " + start)
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
                trace("thrend ptrn: " + thrend);
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
            trace("skip ptrn: " + skip)
            data =~ skip
        }
        return decision
    }

    def addThreadStart(def start, def qlfr)
    {
      trace("adding thread start: " + start);
      if (!THRD_START_PTTRNS.contains(start))
      {
        THRD_START_PTTRNS.add(start)
        addExtendedFilterPattern(start, qlfr)
      }
      else trace("Start exists")
    }

    def removeThreadStart(def start, def qlfr)
    {
      trace("removing thread start: " + start);
      THRD_START_PTTRNS.remove(start);
      removeExtendedFilterPattern(start);
    }

}
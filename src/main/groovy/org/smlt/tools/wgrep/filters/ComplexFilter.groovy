package org.smlt.tools.wgrep.filters

import java.util.regex.Matcher
import org.smlt.tools.wgrep.filters.enums.*

import groovy.util.logging.Slf4j;
import groovy.xml.dom.DOMCategory

@Slf4j
class ComplexFilter extends FilterBase {

    //Complex pattern processing and stuff
    def PATTERN = new StringBuilder("(?ms)") //for multiline support
    def EXTNDD_PTTRNS = []
    def EXTNDD_PTTRN_DICT = [:]

    def pt_tag = null
    def THRD_START_EXTRCTRS =[:]
    def THRD_START_PTTRNS = []
    def THRD_SKIP_END_PTTRNS = []
    def THRD_END_PTTRNS =[]

    ComplexFilter(FilterBase nextFilter_, def filterPtrn_, def pt_tag_)
    {
        super(nextFilter_, filterPtrn_)
        log.trace("Added on top of " + nextFilter.getClass().getCanonicalName())
        pt_tag = pt_tag_
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
        processExtendedPattern(filterPtrn)
    }

    def addExtendedFilterPattern(def val, def qualifier)
    {
      log.trace("adding complex pattern: val=" + val + " qual=" + qualifier)
      
      if (qualifier != null) PATTERN = PATTERN.append(Qualifier.valueOf(qualifier).getPattern())
      PATTERN = PATTERN.append(val)
      
      EXTNDD_PTTRNS.add(val)
      EXTNDD_PTTRN_DICT[val] = qualifier ? Qualifier.valueOf(qualifier) : null
      
      log.trace(EXTNDD_PTTRNS.toString())
      log.trace(EXTNDD_PTTRN_DICT.toString())
    }

    def removeExtendedFilterPattern(def val)
    {
      def qlfr = EXTNDD_PTTRN_DICT[val]
      def ptrn = (qlfr ? qlfr.getPattern() : '') + val
      def ptrnIndex = PATTERN.indexOf(ptrn)
      log.trace('to delete:/' + ptrn +'/ index:' + ptrnIndex)
      if (ptrnIndex != -1)
      {
        PATTERN = PATTERN.delete(ptrnIndex, ptrnIndex + ptrn.length())
        EXTNDD_PTTRNS.remove(val)
        EXTNDD_PTTRN_DICT.remove(val)
      }
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
            log.trace('Processing complex pattern')
            mtch = val.tokenize("%")
            def nextQualifier = null
            if (mtch != null)
            {
                qRegex = qRegex.replaceAll(/%/, "")
                for (grp in mtch)
                {
                    log.trace('Next group in match: ' + grp)
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
            log.trace('No extended pattern supplied, might be a preserve thread')
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
        setPattern(PATTERN.toString())

        def blockMtchr = blockData =~ filterPtrn
        if (blockMtchr.find())
        {
            if (isThreadPreserveEnabled())
            {
                extractThreadPatterns(blockData)
            }
            
            super.filter(blockData)
        }
        else 
        {
            log.trace("not passed")
        }
    }

    boolean isThreadPreserveEnabled()
    {
        return pt_tag != null
    }

    def extractThreadPatterns(def data)
    {
        if (searchThreadEnds(data)) 
        { 
            extractThreadStarts(data, "removeThreadStart")
        }
        else
        {
            log.trace("Thread continues. Keeping starts")
            extractThreadStarts(data, "addThreadStart")
        }
    }

    def extractThreadStarts(def data, def method)
    {
        THRD_START_EXTRCTRS.each
        {extrctr, qlfr -> 
            log.trace(extrctr);
            def srch = (data =~ extrctr);
            if (srch)
            {
                def start = srch[0]
                log.trace("extracted; " + start)
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
                log.trace("thrend ptrn: " + thrend);
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
            log.trace("skip ptrn: " + skip)
            data =~ skip
        }
        return decision != null
    }

    def addThreadStart(def start, def qlfr)
    {
      log.trace("adding thread start: " + start);
      if (!THRD_START_PTTRNS.contains(start))
      {
        THRD_START_PTTRNS.add(start)
        addExtendedFilterPattern(start, qlfr)
      }
      else log.trace("Start exists")
    }

    def removeThreadStart(def start, def qlfr)
    {
      log.trace("removing thread start: " + start);
      THRD_START_PTTRNS.remove(start);
      removeExtendedFilterPattern(start);
    }

}
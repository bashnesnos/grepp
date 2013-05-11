package org.smlt.tools.wgrep.filters.entry

import groovy.xml.dom.DOMCategory
import org.smlt.tools.wgrep.util.WgrepUtil;
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.smlt.tools.wgrep.config.WgrepConfig
import org.smlt.tools.wgrep.exceptions.FilteringIsInterruptedException;
import org.smlt.tools.wgrep.filters.enums.Event
import org.smlt.tools.wgrep.filters.enums.Qualifier
import org.smlt.tools.wgrep.filters.FilterBase

/**
 * Class which provide post filtering of passed entries <br>
 * Basically it extracts substrings from data matched by configured patterns <br>
 * It can simply extract substrings in a column-like way for each, can count number of substring that was matched; <br>
 * or it can group by particular substring and calculate and average value (which will always be a number) 
 * 
 * @author Alexander Semelit 
 */

class PostFilter extends FilterBase<String> {

    //Postprocessing stuff
    Pattern postFilterPattern = null
    def POST_PROCESS_SEP = null
    def POST_PROCESS_DICT = new LinkedHashMap()
    def POST_PROCESS_HEADER = null
    def POST_GROUPS = [:]
    def currentGroup = null
    def groupMethod = null
    def POST_GROUPS_METHODS = []
    def HEADER_PRINTED = false
    def result = null

    /**
    * Creates new PostFilter on top of supplied filter chain and fills in params from supplied config. <br>
    * Also it parses from config.xml post filter pattern configuration basing on fulfilled POST_PROCESSING parameter.
    *
    */
    PostFilter(FilterBase<String> nextFilter_, Map postFilterParams)
    {
        super(nextFilter_, PostFilter.class)
		POST_PROCESS_SEP = postFilterParams["POST_PROCESS_SEP"] //nulls allowed here, will be validated
		WgrepUtil.throwIllegalAEifNull(POST_PROCESS_SEP, "Post separator shouldn't be null")
		POST_PROCESS_DICT = postFilterParams["POST_PROCESS_DICT"]
		POST_PROCESS_HEADER = postFilterParams["POST_PROCESS_HEADER"]
		POST_GROUPS_METHODS = postFilterParams["POST_GROUPS_METHODS"]
        postFilterPattern = Pattern.compile(postFilterParams["PATTERN"])
        log.trace("postFilterPattern: {}", postFilterPattern)
    }


    /**
    * Tries to match all post processing patterns at the same time to received block data. <br>
    * If succeeds it will cumulatively build a result String for each pattern group and pass it further instead of recieved block. <br>
    * In the case of grouping, it won't pass anything until all files will be processed. I.e. it will accumulate all the results till that event happens. <br>
    * Since it matches all the post patterns at the same time, if any of them is not matched nothing will be returned/accumulated.
    *
    * 
    * @param blockData A String to be post processed.
    * @return true if it has accumulated result to pass
    */
    @Override
    public boolean check(String blockData)
    {
         result = null //invalidating result first
         Matcher postPPatternMatcher = postFilterPattern.matcher(blockData)
         if (postPPatternMatcher.find()) //bulk matching all patterns. If any of them won't be matched nothing will be returned
         {
            result = new StringBuilder("")
            int ptrnIndex = 1
            POST_PROCESS_DICT.each { ptrn, handler -> aggregatePostProcess(postPPatternMatcher, result, POST_PROCESS_SEP, handler, ptrnIndex++)} //TODO: new handlers model is needed
         }
         return result != null && result.size() > 0
    }

    /**
    * Passes further accumulated matched substrings instead of blockData received by <code>this.filter()</code> method.
    * @param blockData A String to be post processed. 
    * @return <code>super.passNext</code> result
    */

    @Override
    public void beforePassing(String blockData)
    {
        if (!HEADER_PRINTED) 
        {   
            HEADER_PRINTED = true
            passingVal = new StringBuilder(POST_PROCESS_HEADER).append(result).toString()
        }
		else
		{
			passingVal = result.toString()
		}
    }
	
	/**
	 * This method is used to extract and process matched groups from supplied data. <br>
	 * It is considered that actual matching is done prior to calling this method, and it's purpose is just to call appropriate handler method for appropriate group in Matcher <br>
	 * Handlers to group relation is defined during PostFilter initialization through config.xml parsing.
	 * 
	 * @param mtchr Matcher object which has matched all post patterns to data
	 * @param agg accumulator object which will gather matched substring
	 * @param sep column separator string
	 * @param method declared in PostFilter method which will be used to extract matched group value
	 * @param groupIdx index of a matched group which will be used to get substrings
	 * @return accumulator with appended substring for current group
	 */

    StringBuilder aggregatePostProcess(Matcher mtchr, StringBuilder agg, String sep, String method, Integer groupIdx)
    {
		log.trace("Aggregating post processing, agg={} method={} groupIdx={} \nmtch found", agg, method, groupIdx)
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
	
	/**
	 * Method handles value escaping with separator during appending to accumulator.
	 * 
	 * @param agg accumulator object
	 * @param sep column separator string
	 * @param val value to be appended. Is considered as something providing toString() method
	 * @return accumulator object containing appended value
	 */
	
    StringBuilder aggregatorAppend(StringBuilder agg, String sep, def val)
    {
        return (agg.size() > 0) ? agg.append(sep).append(val):agg.append(val)
    }

	/**
	 * Simply returns substring matched by a pattern.
	 * 
	 * @param mtchResults Matcher containing needed group
	 * @param groupIdx index of the group
	 * @return group value
	 */
	
    def processPostFilter(Matcher mtchResults, def groupIdx)
    {
        return mtchResults.group(groupIdx)
    }

	/**
	 * Counts number of substrings matched by a pattern.
	 * 
	 * @param mtchResults Matcher containing needed group
	 * @param groupIdx index of the group
	 * @return count of substrings matched by pattern
	 */
	
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

	/**
	 * Creates new/or fetches existing group map with key equal to matched substring. <br>
	 * Sets <code>currentGroup</code> to this group. 
	 * 
	 * @param mtchResults Matcher containing needed group
	 * @param groupIdx index of the group
	 * @return always returns null, as during grouping results will be returned when all the files will be processed.
	 */
	
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

	/**
	 * Attempts to convert matching substring to a number (if it is not a number, tries to count number of matched substrings by current group). <br>
	 * Stores acquired number value to currentGroup map with key "averageAgg"
	 * 
	 * @param mtchResults Matcher containing needed group
	 * @param groupIdx index of the group
	 * @return always returns null, as during grouping results will be returned when all the files will be processed.
	 */
	
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
        log.trace ("added new val: {}", newIntVal)
        return null
    }

	/**
	 * Method which will be used to calculate average. <br>
	 * Should have the same name as extracting method, but accept Map, which will mean that it is applied to actually do calculations.
	 * 
	 * @param group Map containing accumulated numbers to calculate average from
	 * @return average value
	 */
    def processPostAverage(Map group)
    {
        log.trace ("average group: {}", group)
        List<Integer> averageAgg = group["averageAgg"]
        if (averageAgg == null || averageAgg.size() == 0) return 0
        Integer sum = 0
        averageAgg.each { Integer val ->
            sum += val
        }
        return sum/averageAgg.size()
    }

	/**
	 * 
	 * Method iterates through all gathered groups and calls for each all configured in this session group methods. <br>
	 * Accumulates results of each group method in similar way as in smartPostProcess method. <br>
	 * When all groups are processed it passes result to next filter.
	 * 
	 */
    def processGroups()
    {
        passingVal = null //invalidating passingVal
        StringBuilder rslt = new StringBuilder( !HEADER_PRINTED ? POST_PROCESS_HEADER : "");
		POST_GROUPS.each { group ->
            rslt.append(group.getKey())
            POST_GROUPS_METHODS.each { method ->
                aggregatorAppend(rslt, POST_PROCESS_SEP, this."$method"(group.getValue()))
            }
			rslt.append("\n")
        }
		return passNext(rslt.toString())
    }

	/**
	 * Listens for ALL_FILES_PROCESSED event to trigger all groups processing.
	 * 
	 */
	@Override
	protected StringBuilder gatherPrintableState(Event event, StringBuilder agg) {
        switch (event)
        {
            case Event.ALL_FILES_PROCESSED:
                try {
					appendNotNull(agg, processGroups())
                }
            	catch (FilteringIsInterruptedException e) {
            		log.error("Filtering interrupted by", e);
            	}
				break;
            default:
                break
        }
        return super.gatherPrintableState(event, agg)
    }

}
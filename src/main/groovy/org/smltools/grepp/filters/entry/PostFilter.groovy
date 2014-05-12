package org.smltools.grepp.filters.entry

import groovy.xml.dom.DOMCategory
import org.smltools.grepp.util.GreppUtil;
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.smltools.grepp.exceptions.FilteringIsInterruptedException;
import org.smltools.grepp.filters.enums.Event
import org.smltools.grepp.filters.enums.Qualifier
import org.smltools.grepp.filters.FilterBase

/**
 * Class which provide post filtering of passed entries <br>
 * Basically it extracts substrings from data matched by configured patterns <br>
 * It can simply extract substrings in a column-like way for each, can count number of substring that was matched; <br>
 * or it can group by particular substring and calculate and average value (which will always be a number) 
 * 
 * @author Alexander Semelit 
 */

final class PostFilter extends StatefulFilterBase<String> {
    public static final String SEPARATOR_KEY = 'postProcessSeparator'
    public static final String SPOOL_EXTENSION_KEY = 'spoolFileExtension'
    public static final String VALUE_KEY = 'value'
    public static final String COLUMNS_KEY = 'postProcessColumns'
    public static final String COLUMN_NAME_KEY = 'colName'

    //Postprocessing stuff
    private Pattern postFilterPattern = null
    private String columnSeparator = null
    private Map<?,?> postProcessingDictionary = new LinkedHashMap()
    private String reportHeader = null
    private Map<?,?> groupMap = [:]
    private def currentGroup = null
    private def groupMethod = null
    private List<?> groupMethodsList = []
    private boolean isHeaderPrinted = false
    private StringBuilder result = new StringBuilder()
    private String spoolFileExtension

    /**
    * Creates new PostFilter on top of supplied filter chain and fills in params from supplied config. <br>
    * Also it parses from config.xml post filter pattern configuration basing on fulfilled POST_PROCESSING parameter.
    *
    */
    PostFilter(String postFilterPattern, String columnSeparator, Map<?,?> postProcessingDictionary, String reportHeader, List<?> groupMethodsList)
    {
        super(PostFilter.class, null)
        GreppUtil.throwIllegalAEifNull(columnSeparator, "Column separator shouldn't be null")
		this.columnSeparator = columnSeparator
		
        GreppUtil.throwIllegalAEifNull(postProcessingDictionary, "postProcessingDictionary shouldn't be null")
		this.postProcessingDictionary = postProcessingDictionary

		this.reportHeader = reportHeader

        GreppUtil.throwIllegalAEifNull(groupMethodsList, "groupMethodsList shouldn't be null")
		this.groupMethodsList = groupMethodsList

        GreppUtil.throwIllegalAEifNull(postFilterPattern, "postFilterPattern shouldn't be null")
        this.postFilterPattern = Pattern.compile(postFilterPattern)
        LOGGER.trace("postFilterPattern: {}", postFilterPattern)
    }


    /**
    * Creates PostFilter from config
    *
    */
    public PostFilter(Map<?, ?> config, String configId) {
        if (config == null || configId == null) {
            throw new IllegalArgumentException("All the constructor params shouldn't be null! " + (config != null) + ";" + (configId != null));
        }

        super(PostFilter.class, config);
        fillParamsByConfigIdInternal(configId);
    }

    public String getSpoolFileExtension() {
        return spoolFileExtension
    }

    @SuppressWarnings("unchecked")
    @Override
    protected boolean fillParamsByConfigIdInternal(String configId) {
        if (!PostFilter.configIdExists(config, configId)) {
            throw new ConfigNotExistsRuntimeException(configId);
        }

        StringBuilder postFilterPatternBuilder = new StringBuilder()
        //defaults come first
        columnSeparator = config.defaults.postProcessSeparator.value
        spoolFileExtension = config.defaults.postProcessSeparator.spoolFileExtension

        def handlers = config.postProcessColumns."$postProcessColumnId"
        def sortedHandlers = handlers.sort { it.value.order }
        def separatorProps = sortedHandlers.find { type, props -> type.equals(SEPARATOR_KEY) }
        if (separatorProps != null) {
            columnSeparator = separatorProps.value
            spoolFileExtension = separatorProps.spoolFileExtension
        }

        if (columnSeparator == null || spoolFileExtension == null || columnSeparator.size() < 1 || spoolFileExtension.size() < 1) {
            throw new PropertiesNotFoundRuntimeException("Both " + VALUE_KEY + " and " + SPOOL_EXTENSION_KEY + " should be filled either in defaults." + SEPARATOR_KEY + " or " + COLUMNS_KEY "." + configId + "." + SEPARATOR_KEY);
        }

        sortedHandlers.each { type, props -> 
            if (!type.equals(SEPARATOR_KEY)) {
                LOGGER.trace("postProcessColumn type: {}; props: {}", type, props.keySet())

                if (!props.containsKey(VALUE_KEY)) {
                    throw new PropertiesNotFoundRuntimeException(COLUMNS_KEY "." + configId + "." + type + "." + VALUE_KEY + " should be filled");
                }

                def curPtrn = props.value
                postFilterPatternBuilder = postFilterPatternBuilder.size() == 0 ? postFilterPatternBuilder.append("(?ms)").append(curPtrn) : postFilterPatternBuilder.append(Qualifier.and.getPattern()).append(curPtrn)
                switch (type) {
                    case "filter":
                        postProcessingDictionary[curPtrn] = 'processPostFilter'
                        break
                    case "counter":
                        postProcessingDictionary[curPtrn] = 'processPostCounter'
                        break
                    case "group":
                        postProcessingDictionary[curPtrn] = 'processPostGroup'
                        break
                    case "avg":
                        postProcessingDictionary[curPtrn] = 'processPostAverage'
                        groupMethodsList.add('processPostAverage')
                        break
                    default:
                        throw new IllegalArgumentException("Unknown handler type: " + type + " at config: " + COLUMNS_KEY + "." + configId)
                }
                
                if (props.containsKey(COLUMN_NAME_KEY)) {
                    reportHeader = (reportHeader != null) ? reportHeader + columnSeparator + props.colName : props.colName
                }
            }
        }
        if (reportHeader != null) reportHeader += "\n"

        return true
    }

    @SuppressWarnings("unchecked")
    public static boolean configIdExists(Map<?, ?> config, String configId) {
        return config.postProcessColumns.containsKey(postProcessColumnId)
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
    public String filter(String blockData) {
         result.setLength(0) //invalidating result first
         Matcher postPPatternMatcher = postFilterPattern.matcher(blockData)
         if (postPPatternMatcher.find()) {//bulk matching all patterns. If any of them won't be matched nothing will be returned
            if (reportHeader != null && !isHeaderPrinted) {   
                isHeaderPrinted = true
                result.append(reportHeader)
            }
            int ptrnIndex = 1
            postProcessingDictionary.each { ptrn, handler -> aggregatePostProcess(postPPatternMatcher, result, columnSeparator, handler, ptrnIndex++)} //TODO: new handlers model is needed
         }
        
        return (result != null && result.size() > 0) ? result.toString() : null
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

    private StringBuilder aggregatePostProcess(Matcher mtchr, StringBuilder agg, String sep, String method, Integer groupIdx)
    {
		LOGGER.trace("Aggregating post processing, agg={} method={} groupIdx={} \nmtch found", agg, method, groupIdx)
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
	
    private StringBuilder aggregatorAppend(StringBuilder agg, String sep, def val)
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
	
    private def processPostFilter(Matcher mtchResults, def groupIdx)
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
	
    private def processPostCounter(Matcher mtchResults, def groupIdx)
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
	
    private def processPostGroup(Matcher mtchResults, def groupIdx)
    {
        String newGroup = mtchResults.group(groupIdx)
        Map existingGroup = groupMap[newGroup]
        if (existingGroup == null)
        {
            groupMap[newGroup] = [:]
            existingGroup = groupMap[newGroup]
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
	
    private def processPostAverage(Matcher mtchResults, def groupIdx)
    {
        Integer newIntVal = 0
        try {
            newIntVal = Integer.valueOf(mtchResults.group(groupIdx))            
        }
        catch(NumberFormatException e) {
            LOGGER.trace("attempting to count current group")
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
        LOGGER.trace ("added new val: {}", newIntVal)
        return null
    }

	/**
	 * Method which will be used to calculate average. <br>
	 * Should have the same name as extracting method, but accept Map, which will mean that it is applied to actually do calculations.
	 * 
	 * @param group Map containing accumulated numbers to calculate average from
	 * @return average value
	 */
    private def processPostAverage(Map group) {
        LOGGER.trace ("average group: {}", group)
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
    private String processGroups() {
        StringBuilder rslt = new StringBuilder( reportHeader != null && !isHeaderPrinted ? reportHeader : "");
		groupMap.each { group ->
            rslt.append(group.getKey())
            groupMethodsList.each { method ->
                aggregatorAppend(rslt, columnSeparator, this."$method"(group.getValue()))
            }
			rslt.append("\n")
        }
		return rslt.toString()
    }

	/**
	 * Listens for ALL_CHUNKS_PROCESSED event to trigger all groups processing.
	 * 
	 */
	@Override
	protected String processEventInternal(Event event) {
        switch (event)
        {
            case Event.ALL_CHUNKS_PROCESSED:
				return processGroups()
            default:
                return null
        }
    }

}
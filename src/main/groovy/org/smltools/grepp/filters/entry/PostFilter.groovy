package org.smltools.grepp.filters.entry

import groovy.xml.dom.DOMCategory
import org.smltools.grepp.util.GreppUtil;
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.smltools.grepp.exceptions.ConfigNotExistsRuntimeException
import org.smltools.grepp.exceptions.FilteringIsInterruptedException;
import org.smltools.grepp.exceptions.PropertiesNotFoundRuntimeException
import org.smltools.grepp.exceptions.PropertiesNotFoundRuntimeException
import org.smltools.grepp.filters.enums.Event
import org.smltools.grepp.filters.enums.Qualifier
import org.smltools.grepp.filters.StatefulFilterBase
import org.smltools.grepp.filters.FilterParams
import org.smltools.grepp.filters.PostFilterMethod
import org.smltools.grepp.filters.PostFilterGroupMethod
import groovy.util.ConfigObject;

/**
 * Class which provide post filtering of passed entries <br>
 * Basically it extracts substrings from data matched by configured patterns <br>
 * It can simply extract substrings in a column-like way for each, can count number of substring that was matched; <br>
 * or it can group by particular substring and calculate and average value (which will always be a number) 
 * 
 * @author Alexander Semelit 
 */
@FilterParams(order = 20)
public final class PostFilter extends StatefulFilterBase<String> {
    public static final String SEPARATOR_KEY = 'postProcessSeparator'
    public static final String SPOOL_EXTENSION_KEY = 'spoolFileExtension'
    public static final String VALUE_KEY = 'value'
    public static final String COLUMNS_KEY = 'postProcessColumns'
    public static final String COLUMN_NAME_KEY = 'colName'

    //Postprocessing stuff
    private Pattern postFilterPattern = null
    String columnSeparator = null
    String reportHeader = null
    private GroupingMethod groupingMethod = null
    private List<String> methodTypeList = []
    boolean isHeaderPrinted = false
    private StringBuilder result = new StringBuilder()
    private String spoolFileExtension
    private List<? extends PostFilterMethod> filterMethods = []

    /**
    * Creates new PostFilter on top of supplied filter chain and fills in params from supplied config. <br>
    * Also it parses from config.xml post filter pattern configuration basing on fulfilled POST_PROCESSING parameter.
    *
    */
    public PostFilter(String postFilterPattern, String columnSeparator, String reportHeader, List<String> methodTypeList) {
        super(PostFilter.class)
		setPostFilterPattern(postFilterPattern)
        setColumnSeparator(columnSeparator)
        setReportHeader(reportHeader)
		setMethodTypeList(methodTypeList)
    }

    public PostFilter(Map<?, ?> config) {
        super(PostFilter.class, config);
    }

    public void setPostFilterPattern(String postFilterPattern) {
        GreppUtil.throwIllegalAEifNull(postFilterPattern, "postFilterPattern shouldn't be null")
        this.postFilterPattern = Pattern.compile(postFilterPattern)
        LOGGER.trace("postFilterPattern: {}", postFilterPattern)
    }

    public void setColumnSeparator(String columnSeparator) {
        GreppUtil.throwIllegalAEifNull(columnSeparator, "Column separator shouldn't be null")
        this.columnSeparator = columnSeparator
    }

    public void setReportHeader(String reportHeader) {
        this.reportHeader = reportHeader        
    }

    public void setMethodTypeList(List<String> methodTypeList) {
        GreppUtil.throwIllegalAEifNull(methodTypeList, "methodTypeList shouldn't be null")
        this.methodTypeList = methodTypeList
    }

    /**
    * Creates PostFilter from config
    *
    */
    public PostFilter(Map<?, ?> config, String configId) {
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

        def handlers = config.postProcessColumns."$configId"
        def sortedHandlers = handlers.sort { it.value.order }
        def separatorProps = sortedHandlers.find { type, props -> type.equals(SEPARATOR_KEY) }
        if (separatorProps != null) {
            columnSeparator = separatorProps.value
            spoolFileExtension = separatorProps.spoolFileExtension
            config.runtime.spoolFileExtension = spoolFileExtension
        }

        if (columnSeparator == null || spoolFileExtension == null || columnSeparator.size() < 1 || spoolFileExtension.size() < 1) {
            throw new PropertiesNotFoundRuntimeException("Both " + VALUE_KEY + " and " + SPOOL_EXTENSION_KEY + " should be filled either in defaults." + SEPARATOR_KEY + " or " + COLUMNS_KEY + "." + configId + "." + SEPARATOR_KEY);
        }

        sortedHandlers.each { type, props -> 
            if (!type.equals(SEPARATOR_KEY)) {
                LOGGER.trace("postProcessColumn type: {}; props: {}", type, props.keySet())

                if (!props.containsKey(VALUE_KEY)) {
                    throw new PropertiesNotFoundRuntimeException(COLUMNS_KEY + "." + configId + "." + type + "." + VALUE_KEY + " should be filled")
                }

                def curPtrn = props.value
                postFilterPatternBuilder = postFilterPatternBuilder.size() == 0 ? postFilterPatternBuilder.append("(?ms)").append(curPtrn) : postFilterPatternBuilder.append(Qualifier.and.getPattern()).append(curPtrn)
                methodTypeList.add(type)
                switch (type) {
                    case "filter":
                        filterMethods.add(new SimpleMatchingMethod())
                        break
                    case "counter":
                        filterMethods.add(new CountingMethod())
                        break
                    case "group":
                        groupingMethod = new GroupingMethod(this)
                        filterMethods.add(groupingMethod)
                        break
                    case "avg":
                        def method = new AveragingMethod()
                        if (groupingMethod != null) {
                            groupingMethod.addChildMethod(method)
                        }
                        else {
                            filterMethods.add(method)
                        }
                        break
                    default:
                        throw new IllegalArgumentException("Unknown postFilterMethod type: " + type + " at config: " + COLUMNS_KEY + "." + configId)
                }
                
                if (props.containsKey(COLUMN_NAME_KEY)) {
                    reportHeader = (reportHeader != null) ? reportHeader + columnSeparator + props.colName : props.colName
                }
            }
        }
        postFilterPattern = Pattern.compile(postFilterPatternBuilder.toString())
        return true
    }

    @Override
    public ConfigObject getAsConfig(String configId) {
        if (configId == null) {
            if (this.configId == null) {
                throw new IllegalArgumentException("Can't derive configId (none was supplied)");
            }
            else {
                configId = this.configId;
            }
        }
        //implement when PostFilter is more configurable
        return new HashMap<Object, Object>();
    }


    @SuppressWarnings("unchecked")
    public static boolean configIdExists(Map<?, ?> config, String configId) {
        if (config == null) {
            throw new IllegalArgumentException("Config can't be null!");
        }

        return config.postProcessColumns.containsKey(configId)
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
        if (postFilterPattern == null) {
            throw new IllegalStateException("postFilterPattern should be supplied via configId or explicitly!")
        }

         result.setLength(0) //invalidating result first
         Matcher postPPatternMatcher = postFilterPattern.matcher(blockData)
         if (postPPatternMatcher.find()) {//bulk matching all patterns. If any of them won't be matched nothing will be returned
            int ptrnIndex = 1
            filterMethods.each { method ->
               aggregatePostProcess(postPPatternMatcher, result, columnSeparator, method, ptrnIndex++)
            }
            if (reportHeader != null && !isHeaderPrinted && groupingMethod == null) {   
                isHeaderPrinted = true
                result.insert(0, reportHeader + "\n")
            }
         }
        
        return (result != null && result.size() > 0) ? result.toString() : null
    }

    
    @Override
    public void flush() {
        isHeaderPrinted = false
        if (groupingMethod != null) {
            groupingMethod.flush()
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

    private StringBuilder aggregatePostProcess(Matcher mtchr, StringBuilder agg, String sep, PostFilterMethod<String> method, Integer groupIdx)
    {
		LOGGER.trace("Aggregating post processing, agg={} method={} groupIdx={} \nmtch found", agg, method.getClass().getName(), groupIdx)
        String result = method.processMatchResults(mtchr, groupIdx)
        if (agg != null && result != null) //omitting printing since one of the results was null. Might be a grouping
        {
            return aggregatorAppend(agg, sep, result)
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
        return (agg.size() > 0) ? agg.append(sep).append(val) : agg.append(val)
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
                if (groupingMethod != null) {
				    return groupingMethod.processGroups()
                }
                else {
                    return null
                }
            default:
                return null
        }
    }

    private class GroupingMethod implements PostFilterMethod<String> {
        private Map<?,?> groupMap = [:]
        private Map<?,?> currentGroup = null
        private List<? extends PostFilterGroupMethod> methodsToGroup = []
        private PostFilter papa = papa

        public GroupingMethod(PostFilter papa) {
            this.papa = papa
        }

        public void addChildMethod(PostFilterGroupMethod method){
            methodsToGroup.add(method)
        }

        public void flush() {
            currentGroup = null
            groupMap.clear()
        }

        /**
         * Creates new/or fetches existing group map with key equal to matched substring. <br>
         * Sets <code>currentGroup</code> to this group. 
         * 
         * @param mtchResults Matcher containing needed group
         * @param groupIdx index of the group
         * @return always returns null, as during grouping results will be returned when all the files will be processed.
         */
        @Override
        public String processMatchResults(Matcher mtchResults, Integer groupIdx)
        {
            if (methodsToGroup.isEmpty()) {
                throw new IllegalStateException("At least one group method is expected if group is specified")
            }

            int initGroupIdx = groupIdx //need to increase it for each method
            String newGroup = mtchResults.group(initGroupIdx++)
            Map existingGroup = groupMap[newGroup]
            if (existingGroup == null)
            {
                groupMap[newGroup] = [:]
                existingGroup = groupMap[newGroup]
            }
            currentGroup = existingGroup
            methodsToGroup.each { method ->
                aggregateFilterResult(method.processMatchResults(mtchResults, initGroupIdx++), method.getAggregatorKey())
            }
            return null
        }

        private <T> void aggregateFilterResult(T result, String aggregatorKey) {
            if (aggregatorKey == null) {
                throw new IllegalArgumentException("Non-null aggregator key should be provided by a PostGroupMethod implementation")
            }

            if (result == null) {
                return
            }

            //aggregate children
            List<T> agg = currentGroup[aggregatorKey]
            if (agg != null)
            {
                agg.add(result)
            }
            else
            {
                currentGroup[aggregatorKey] = [result]
            }            

        }

        /**
         * 
         * Method iterates through all gathered groups and calls for each all configured in this session group methods. <br>
         * Accumulates results of each group method in similar way as in smartPostProcess method. <br>
         * When all groups are processed it passes result to next filter.
         * 
         */
        public String processGroups() {
            StringBuilder rslt = new StringBuilder( papa.reportHeader != null && !papa.isHeaderPrinted ? papa.reportHeader + "\n" : "");
            groupMap.each { groupName, groupValue ->
                rslt.append(groupName)
                methodsToGroup.each { method ->
                    papa.aggregatorAppend(rslt, papa.columnSeparator, method.processGroup(groupValue))
                }
                rslt.append("\n")
            }
            return rslt.toString()
        }

    }

}


class SimpleMatchingMethod implements PostFilterMethod<String> {
    /**
     * Simply returns substring matched by a pattern.
     * 
     * @param mtchResults Matcher containing needed group
     * @param groupIdx index of the group
     * @return group value
     */
    @Override
    public String processMatchResults(Matcher mtchResults, Integer groupIdx) {
        return mtchResults.group(groupIdx)
    }    
}

class CountingMethod implements PostFilterMethod<Integer> {
    /**
     * Counts number of substrings matched by a pattern.
     * 
     * @param mtchResults Matcher containing needed group
     * @param groupIdx index of the group
     * @return count of substrings matched by pattern
     */
    @Override
    public Integer processMatchResults(Matcher mtchResults, Integer groupIdx) {
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
}

class AveragingMethod implements PostFilterGroupMethod<Integer> {
    public static final String AVG_AGGREGATOR_KEY = "averageAgg"

    private CountingMethod internalCountingMethod = new CountingMethod()

    @Override
    public String getAggregatorKey() {
        return AVG_AGGREGATOR_KEY
    }
    /**
     * Attempts to convert matching substring to a number (if it is not a number, tries to count number of matched substrings by current group). <br>
     * Stores acquired number value to currentGroup map with key "averageAgg"
     * 
     * @param mtchResults Matcher containing needed group
     * @param groupIdx index of the group
     * @return always returns null, as during grouping results will be returned when all the files will be processed.
     */
    @Override
    public Integer processMatchResults(Matcher mtchResults, Integer groupIdx)
    {
        Integer newIntVal = 0
        try {
            newIntVal = Integer.valueOf(mtchResults.group(groupIdx))            
        }
        catch(NumberFormatException e) {
            newIntVal = internalCountingMethod.processMatchResults(mtchResults, groupIdx)
        }
        return newIntVal
    }

    /**
     * Method which will be used to calculate average. <br>
     * Should have the same name as extracting method, but accept Map, which will mean that it is applied to actually do calculations.
     * 
     * @param group Map containing accumulated numbers to calculate average from
     * @return average value
     */
    @Override
    public Integer processGroup(Map group) {
        List<Integer> averageAgg = group[AVG_AGGREGATOR_KEY]
        if (averageAgg == null || averageAgg.size() == 0) return 0

        Integer sum = 0
        averageAgg.each { Integer val ->
            sum += val
        }
        return sum/averageAgg.size()
    }
}
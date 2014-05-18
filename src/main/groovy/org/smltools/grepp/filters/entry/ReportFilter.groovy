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
import org.smltools.grepp.filters.ReportMethodParams
import org.smltools.grepp.filters.ReportMethod
import org.smltools.grepp.filters.ReportMethodBase
import org.smltools.grepp.filters.ReportGroupMethod
import static org.smltools.grepp.Constants.*
import groovy.util.ConfigObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Class which provide post filtering of passed entries <br>
 * Basically it extracts substrings from data matched by configured patterns <br>
 * It can simply extract substrings in a column-like way for each, can count number of substring that was matched; <br>
 * or it can group by particular substring and calculate and average value (which will always be a number) 
 * 
 * @author Alexander Semelit 
 */
@FilterParams(order = 20)
public final class ReportFilter extends StatefulFilterBase<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReportFilter.class);

    public static final String SEPARATOR_KEY = 'reportSeparator'
    public static final String SPOOL_EXTENSION_KEY = 'spoolFileExtension'
    public static final String VALUE_KEY = 'value'
    public static final String COLUMNS_KEY = 'reportColumns'
    public static final String COLUMN_NAME_KEY = 'colName'
    public static final String GREPP_REPORT_FILTER_PLUGIN_DIR = "/plugin/reportMethods";

    public static final String GROUP_RESERVED_TYPE_NAME = "group";

    private static final Map<String, Class<? extends ReportMethod>> ID_TO_FILTER_CLASS_MAP = new HashMap<String, Class<? extends ReportMethod>>()

    static {
        addIdToFilterClassMapping(null, SimpleMatchingMethod.class)
        addIdToFilterClassMapping(null, RepeatingSimpleMatchingMethod.class)
        addIdToFilterClassMapping(null, CountingMethod.class)
        addIdToFilterClassMapping(null, AveragingMethod.class)

        if (System.getProperty(GREPP_HOME_SYSTEM_OPTION) != null) {
            File pluginDir = new File(System.getProperty(GREPP_HOME_SYSTEM_OPTION), GREPP_REPORT_FILTER_PLUGIN_DIR);
            if (pluginDir.exists() && pluginDir.isDirectory()) {
                LOGGER.trace("Plugin dir {} exists; plugging in postFilterMethods enabled", GREPP_REPORT_FILTER_PLUGIN_DIR)
                for (File pluginFile: pluginDir.listFiles()) {
                    LOGGER.trace("Found file: {}", pluginFile.name)
                    Class<?> pluginClass = GreppUtil.loadGroovyClass(pluginFile);
                    if (pluginClass != null && ReportMethod.isAssignableFrom(pluginClass)) {
                        addIdToFilterClassMapping(null, pluginClass);
                    }
                    else {
                        LOGGER.error("{} was ignored class: {}", pluginFile.name, pluginClass)
                    }
                }
            }
            else {
                LOGGER.trace("Plugin dir {} doesn't exist; i.e. disabled", GREPP_REPORT_FILTER_PLUGIN_DIR)
            }
        }        
    }

    private static void addIdToFilterClassMapping(String filterId, Class<? extends ReportMethod> filterClass) {
        if (filterId == null) {
            ReportMethodParams reportMethodParams = filterClass.getAnnotation(ReportMethodParams.class)
            if (reportMethodParams != null) {
                filterId = reportMethodParams.id()
            }
            else {
                throw new IllegalArgumentException("Either filterId shouldn't be null, or " + filterClass.name + " should be annotated with ReportMethodParams")
            }
        }
        if (!ID_TO_FILTER_CLASS_MAP.containsKey(filterId)) {
            ID_TO_FILTER_CLASS_MAP.put(filterId, filterClass)
        }
        else {
            throw new IllegalArgumentException("Filter id " + filterId + " already registered!")
        }
    }


    //Postprocessing stuff
    private Pattern reportPattern = null
    private StringBuilder reportPatternBuilder = null
    String columnSeparator = null
    String reportHeader = null
    private GroupingMethod groupingMethod = null
    boolean isHeaderPrinted = false
    private StringBuilder result = new StringBuilder()
    private String spoolFileExtension
    private List<? extends ReportMethod> filterMethods = []

    public void setSpoolFileExtension(String spoolFileExtension) {
        this.spoolFileExtension = spoolFileExtension
    }

    public String getSpoolFileExtension() {
        return spoolFileExtension
    }

    public void setColumnSeparator(String columnSeparator) {
        GreppUtil.throwIllegalAEifNull(columnSeparator, "Column separator shouldn't be null")
        this.columnSeparator = columnSeparator
    }

    public void addFilterMethodByType(String type, String pattern, String colName) {
        GreppUtil.throwIllegalAEifNull("'type' and 'pattern' shouldn't be null!", type, pattern)
        addFilterByType(type, Pattern.compile(pattern), colName)
        appendFilterPattern(pattern)
        addColumnToHeader(colName)
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean fillParamsByConfigId(String configId) {
        if (!ReportFilter.configIdExists(config, configId)) {
            throw new ConfigNotExistsRuntimeException(configId);
        }

        reportPatternBuilder = new StringBuilder()
        //defaults come first
        columnSeparator = config.defaults."$SEPARATOR_KEY".value
        spoolFileExtension = config.defaults."$SEPARATOR_KEY".spoolFileExtension

        def sortedHandlers = config."$COLUMNS_KEY"."$configId"

        if (sortedHandlers.containsKey(GROUP_RESERVED_TYPE_NAME)) { //group comes first
            def tempHandlers = [:]
            tempHandlers.put(GROUP_RESERVED_TYPE_NAME, sortedHandlers.remove(GROUP_RESERVED_TYPE_NAME))
            tempHandlers.putAll(sortedHandlers)
            sortedHandlers = tempHandlers
            config."$COLUMNS_KEY"."$configId" = sortedHandlers            
        }

        def separatorProps = sortedHandlers.find { type, props -> type.equals(SEPARATOR_KEY) }
        if (separatorProps != null) {
            columnSeparator = separatorProps.value
            spoolFileExtension = separatorProps.spoolFileExtension
        }

        if (columnSeparator == null || spoolFileExtension == null || columnSeparator.size() < 1 || spoolFileExtension.size() < 1) {
            throw new PropertiesNotFoundRuntimeException("Both " + VALUE_KEY + " and " + SPOOL_EXTENSION_KEY + " should be filled either in defaults." + SEPARATOR_KEY + " or " + COLUMNS_KEY + "." + configId + "." + SEPARATOR_KEY);
        }

        sortedHandlers.each { type, props -> 
            if (!type.equals(SEPARATOR_KEY)) {
                LOGGER.trace("postProcessColumn type: {}; props: {}", type, props.values())

                if (!props.containsKey(VALUE_KEY)) {
                    throw new PropertiesNotFoundRuntimeException(COLUMNS_KEY + "." + configId + "." + type + "." + VALUE_KEY + " should be filled")
                }

                def curPtrn = props.value
                appendFilterPattern(curPtrn)
                addFilterByType(type, Pattern.compile(curPtrn), props.containsKey(COLUMN_NAME_KEY) ? props.colName : null)
                
                if (props.containsKey(COLUMN_NAME_KEY)) {
                    addColumnToHeader(props.colName)
                }
            }
        }
        return true
    }

    private void appendFilterPattern(String pattern) {
        if (reportPatternBuilder == null) {
            reportPatternBuilder = new StringBuilder()
        }
        reportPatternBuilder.size() == 0 ? reportPatternBuilder.append("(?ms)").append(pattern) : reportPatternBuilder.append(Qualifier.and.getPattern()).append(pattern)
    }

    private void addColumnToHeader(String colName) {
        GreppUtil.throwIllegalAEifNull(columnSeparator, "columnSeparator should be supplied either via configId or explicitly")
        if (colName != null) {
            reportHeader = (reportHeader != null) ? reportHeader + columnSeparator + colName : colName
        }
    }

    private void addFilterByType(String type, Pattern ptrn, String colName) {
        switch (type) {
            case GROUP_RESERVED_TYPE_NAME:
                groupingMethod = new GroupingMethod(this)
                groupingMethod.setPattern(ptrn)
                groupingMethod.setColName(colName)
                if (!filterMethods.isEmpty()) {
                    filterMethods.each {
                        groupingMethod.addChildMethod(it)
                    }
                }
                filterMethods = [groupingMethod]
                break
            default:
                Class<? extends ReportMethod> filterClass = ID_TO_FILTER_CLASS_MAP.get(type)
                if (filterClass != null) {
                    def method = filterClass.newInstance()
                    if (method instanceof ReportMethodBase) {
                        method.setPattern(ptrn)
                        method.setColName(colName)       
                    }
                    addFilterMethod(method)
                }
                else {
                    throw new IllegalArgumentException("Unknown postFilterMethod type: " + type)
                }
        }        
    }

    private void addFilterMethod(ReportMethod method) {
        if (groupingMethod != null) {
            groupingMethod.addChildMethod(method)
        }
        else {
            filterMethods.add(method)
        }
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

        ConfigObject root = new ConfigObject()
        if (groupingMethod != null) {
            root."$COLUMNS_KEY"."$configId".merge(groupingMethod.getAsConfig())
        }
        else {
            root."$COLUMNS_KEY"."$configId".merge(gatherConfigFromMethods(filterMethods))
        }
        return root
    }

    ConfigObject gatherConfigFromMethods(List<? extends ReportMethod> methodsList) {
        ConfigObject config = new ConfigObject()
        methodsList.each {
            if (it.class.isAnnotationPresent(ReportMethodParams.class)) {
                def typeId = it.class.getAnnotation(ReportMethodParams.class).id()
                config."$typeId".colName = it.getColName()
                config."$typeId".value = it.getPattern().pattern()
                
            }
        }

        return config
    }

    @SuppressWarnings("unchecked")
    public static boolean configIdExists(Map<?, ?> config, String configId) {
        if (config == null) {
            throw new IllegalArgumentException("Config can't be null!");
        }

        return config."$COLUMNS_KEY".containsKey(configId)
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
        if (reportPattern == null) {
            if (reportPatternBuilder == null) {
                throw new IllegalStateException("Either reportPattern or reportPatternBuilder should be supplied via configId or explicitly!")
            }
            else {
                reportPattern = Pattern.compile(reportPatternBuilder.toString())
                LOGGER.trace("Set pattern to {}", reportPatternBuilder)                
                reportPatternBuilder = null //i.e. it's not needed anymore
            }
        }

        if (filterMethods.isEmpty()) {
            throw new IllegalStateException("filterMethods should be supplied either via configId or explicitly!")
        }

         result.setLength(0) //invalidating result first
         Matcher postPPatternMatcher = reportPattern.matcher(blockData)
         if (postPPatternMatcher.find()) {//bulk matching all patterns. If any of them won't be matched nothing will be returned
            int groupIdx = 1
            filterMethods.each { method ->
                LOGGER.trace("Aggregating post processing, agg={} method={} groupIdx={} \nmtch found", result, method.getClass().getName(), groupIdx)
                def methodResult = method.processMatchResults(postPPatternMatcher, groupIdx++)
                if (methodResult != null) //omitting printing since one of the results was null. Might be a grouping
                {
                    if (methodResult instanceof List) {
                        methodResult.each {
                            aggregatorAppend(result, columnSeparator, methodResult)
                            result.append('\n')
                        }
                        result.deleteCharAt(result.length())
                    }
                    else {
                        aggregatorAppend(result, columnSeparator, methodResult)   
                    }
                }
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

    private class GroupingMethod extends ReportMethodBase<String> {
        private static final Logger LOGGER = LoggerFactory.getLogger(GroupingMethod.class)
        private Map<?,?> groupMap = [:]
        private Map<?,?> currentGroup = null
        private List<? extends ReportMethod> methodsToGroup = []
        private ReportFilter papa = papa

        public GroupingMethod(ReportFilter papa) {
            this.papa = papa
        }

        public void addChildMethod(ReportMethod method){
            LOGGER.trace("Added {} to group methods", method)
            methodsToGroup.add(method)
        }

        public ConfigObject getAsConfig() {
            ConfigObject config = new ConfigObject()
            config."$GROUP_RESERVED_TYPE_NAME".colName = colName
            config."$GROUP_RESERVED_TYPE_NAME".value = pattern.pattern()

            config.merge(papa.gatherConfigFromMethods(methodsToGroup))
            return config
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
            LOGGER.trace("Group at {}", groupIdx)
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
                LOGGER.trace("Next group method {} at {}", method, initGroupIdx)
                aggregateFilterResult(method.processMatchResults(mtchResults, initGroupIdx++), method instanceof ReportGroupMethod ? method.getAggregatorKey() : method.class.name)
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
                if (result instanceof List) {
                    agg.addAll(result)   
                }
                else {
                    agg.add(result)
                }
            }
            else
            {
                if (result instanceof List) {
                    currentGroup[aggregatorKey] = result
                }
                else {
                    currentGroup[aggregatorKey] = [result]   
                }
            }            

        }

        private <T> String defaultProcessGroup(List<T> aggregatedResults) {
            if (aggregatedResults == null) return ""
            switch (aggregatedResults[0]) {
                case String:
                    return aggregatedResults.join(';')
                default:
                    return aggregatedResults.sum()
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
                    if (method instanceof ReportGroupMethod) {
                        papa.aggregatorAppend(rslt, papa.columnSeparator, method.processGroup(groupValue))
                    }
                    else {
                        papa.aggregatorAppend(rslt, papa.columnSeparator, defaultProcessGroup(groupValue[method.getClass().getName()]))
                    }
                }
                rslt.append("\n")
            }
            return rslt.toString()
        }

    }

}

@ReportMethodParams(id="filter")
class SimpleMatchingMethod extends ReportMethodBase<String> {
    /**
     * Simply returns substring matched by a pattern. It would be just one result mathed last, even if there are more matches
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

@ReportMethodParams(id="rfilter")
class RepeatingSimpleMatchingMethod extends ReportMethodBase<List<String>> {
    /**
     * Returns substring matched by a pattern.
     * 
     * @param mtchResults Matcher containing needed group
     * @param groupIdx index of the group
     * @return group value
     */
    @Override
    public List<String> processMatchResults(Matcher mtchResults, Integer groupIdx) {
        List<String> result = []
        Matcher allMatches = pattern.matcher(mtchResults.group())
        while(allMatches.find()) {
            result.add(allMatches.group(1))
        }
        return result.isEmpty() ? null : result
    }    
}

@ReportMethodParams(id="counter")
class CountingMethod extends ReportMethodBase<Integer> {
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

@ReportMethodParams(id="avg")
class AveragingMethod extends CountingMethod implements ReportGroupMethod<Integer> {
    public static final String AVG_AGGREGATOR_KEY = "averageAgg"

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
            newIntVal = super.processMatchResults(mtchResults, groupIdx)
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
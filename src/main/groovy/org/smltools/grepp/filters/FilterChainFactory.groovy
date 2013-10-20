package org.smltools.grepp.filters

import groovy.util.logging.Slf4j;

import org.smltools.grepp.config.Param
import org.smltools.grepp.config.ParamsHolder
import org.smltools.grepp.filters.FilterBase;
import org.smltools.grepp.filters.entry.*;
import org.smltools.grepp.filters.logfile.*;

/**
 * Class which provide factory methods for filter chain creating. <br>
 * Currently the only sense is to create filters depending on initialized WgrepConfig, so there are not so many methods.
 * 
 * @author Alexander Semelit 
 */

@Slf4j
class FilterChainFactory {
	
	/**
	 * Creates filter chain for entries depending on fulfilled parameters in the  <br>
	 * <li> 0. {@link PrintFilter} is always instantiated as last in chain. </li>
	 * <li> 1. {@link PostFilter} </li>
	 * <li> 2. {@link EntryDateFilter} </li>
	 * <li> 3. {@link ComplexFilter} </li>
	 * <li> 4. {@link LogEntryFilter} </li>
	 *
	 * <br> By current requirements filter pattern should be supplied via options, or defined automatically so optional are 1,2 and 4.
	 *
	 * @param config Initialized WgrepConfig instance.
	 * @return appropriate to supplied config entry filter chain
	 */

	public static FilterBase createFilterChain(ParamsHolder paramsHolder) {
		FilterBase filterChain_ = null

		if (paramsHolder.checkParamIsEmpty(Param.PARSE_PROPERTIES)) {
			if (paramsHolder.check([Param.POST_PROCESSING], [Param.POST_PROCESS_PARAMS])) {
				filterChain_ = new PostFilter(filterChain_, paramsHolder.get(Param.POST_PROCESS_PARAMS))
			}
	
			if (paramsHolder.check([Param.DATE_TIME_FILTER, Param.LOG_DATE_PATTERN, Param.LOG_DATE_FORMAT], [Param.FROM_DATE, Param.TO_DATE])) {
					filterChain_ = new EntryDateFilter(filterChain_, paramsHolder.get(Param.LOG_DATE_PATTERN), paramsHolder.get(Param.LOG_DATE_FORMAT), paramsHolder.get(Param.FROM_DATE), paramsHolder.get(Param.TO_DATE))
			}
	
			if (paramsHolder.check([Param.FILTER_PATTERN], [Param.PRESERVE_THREAD, Param.PRESERVE_THREAD_PARAMS])) {
				filterChain_ = new ComplexFilter(filterChain_, paramsHolder.get(Param.FILTER_PATTERN), paramsHolder.get(Param.PRESERVE_THREAD_PARAMS))
			}
		}
		else {
			filterChain_ = new PropertiesFilter(filterChain_)
		}

		if (!paramsHolder.checkParamIsEmpty(Param.LOG_ENTRY_PATTERN)) {
			filterChain_ = new LogEntryFilter(filterChain_, paramsHolder.get(Param.LOG_ENTRY_PATTERN))
		}

		return filterChain_
	}


	/**
	 * Creates filter chain for log files depending on fulfilled parameters in the  <br>
	 * <li> 0. {@link FileSortFilter} is always instantiated as last in chain. </li>
	 * <li> 1. {@link FileDateFilter} </li>
	 * <li> 2. {@link FileNameFilter} </li>
	 *
	 * <br> Only #1 is optional, if date time filtering is not configured.
	 *
	 * @param config Initialized WgrepConfig instance.
	 * @return appropriate to supplied config entry filter chain
	 */

	public static FilterBase createFileFilterChain(ParamsHolder paramsHolder) {
		FilterBase filterChain_ = new FileSortFilter()

		if (paramsHolder.checkParamIsEmpty(Param.DATE_TIME_FILTER)) {
			if (paramsHolder.check([Param.FILE_DATE_FORMAT],[Param.FROM_DATE, Param.TO_DATE, Param.LOG_FILE_THRESHOLD]))
				filterChain_ = new FileDateFilter(filterChain_, paramsHolder.get(Param.FILE_DATE_FORMAT), paramsHolder.get(Param.FROM_DATE), paramsHolder.get(Param.TO_DATE), paramsHolder.get(Param.LOG_FILE_THRESHOLD))
		}
		return filterChain_
	}
}
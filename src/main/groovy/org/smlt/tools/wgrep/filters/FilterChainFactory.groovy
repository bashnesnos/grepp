package org.smlt.tools.wgrep.filters

import groovy.util.logging.Slf4j;

import org.smlt.tools.wgrep.config.ModuleBase;
import org.smlt.tools.wgrep.filters.entry.*;
import org.smlt.tools.wgrep.filters.logfile.*;

/**
 * Class which provide factory methods for filter chain creating. <br>
 * Currently the only sense is to create filters depending on initialized WgrepConfig, so there are not so many methods.
 * 
 * @author Alexander Semelit 
 */

@Slf4j
class FilterChainFactory extends ModuleBase {
	
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

	FilterBase createFilterChain() {
		FilterBase filterChain_ = null

		if (checkParamIsEmpty('PARSE_PROPERTIES')) {
			if (check(['POST_PROCESSING'], ['POST_PROCESS_PARAMS'])) {
				filterChain_ = new PostFilter(filterChain_, getParam('POST_PROCESS_PARAMS'))
			}
	
			if (check(['DATE_TIME_FILTER', 'LOG_DATE_PATTERN', 'LOG_DATE_FORMAT'], ['FROM_DATE', 'TO_DATE'])) {
					filterChain_ = new EntryDateFilter(filterChain_, getParam('LOG_DATE_PATTERN'), getParam('LOG_DATE_FORMAT'), getParam('FROM_DATE'), getParam('TO_DATE'))
			}
	
			if (check(['FILTER_PATTERN'], ['PRESERVE_THREAD', 'PRESERVE_THREAD_PARAMS'])) {
				filterChain_ = new ComplexFilter(filterChain_, getParam('FILTER_PATTERN'), getParam('PRESERVE_THREAD_PARAMS'))
			}
		}
		else {
			filterChain_ = new PropertiesFilter(filterChain_)
		}

		if (!checkParamIsEmpty('LOG_ENTRY_PATTERN')) {
			filterChain_ = new LogEntryFilter(filterChain_, getParam('LOG_ENTRY_PATTERN'))
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

	FilterBase createFileFilterChain() {
		FilterBase filterChain_ = new FileSortFilter()

		if (getParam('DATE_TIME_FILTER') != null) {
			if (check(['FILE_DATE_FORMAT'],['FROM_DATE', 'TO_DATE', 'LOG_FILE_THRESHOLD']))
				filterChain_ = new FileDateFilter(filterChain_, getParam('FILE_DATE_FORMAT'), getParam('FROM_DATE'), getParam('TO_DATE'), getParam('LOG_FILE_THRESHOLD'))
		}
		return filterChain_
	}
}
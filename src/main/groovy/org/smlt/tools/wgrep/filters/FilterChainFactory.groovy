package org.smlt.tools.wgrep.filters

import groovy.util.logging.Slf4j;

import org.smlt.tools.wgrep.config.WgrepConfig;
import org.smlt.tools.wgrep.filters.entry.ComplexFilter;
import org.smlt.tools.wgrep.filters.entry.EntryDateFilter;
import org.smlt.tools.wgrep.filters.entry.LogEntryFilter;
import org.smlt.tools.wgrep.filters.entry.PostFilter;
import org.smlt.tools.wgrep.filters.logfile.FileDateFilter;
import org.smlt.tools.wgrep.filters.logfile.FileSortFilter;
import org.smlt.tools.wgrep.filters.enums.Qualifier;
import org.w3c.dom.Element;

import groovy.xml.dom.DOMCategory;

/**
 * Class which provide factory methods for filter chain creating. <br>
 * Currently the only sense is to create filters depending on initialized WgrepConfig, so there are not so many methods.
 * 
 * @author Alexander Semelit 
 */

@Slf4j
class FilterChainFactory {
	
	/**
	 * Parses PostFilter configuration from config.xml 
	 * 
	 * @param pp_tag "tag" attribute associated with post processing config
	 * @param config WgrepConfig instance
	 * @return Mapping of params desired by PostFilter
	 */
	
	static Map parsePostFilterParams(String pp_tag, WgrepConfig config){
		def root = config.getParam("root")
		def POST_PROCESS_SEP = null
		def POST_PROCESS_DICT = new LinkedHashMap()
		def POST_GROUPS_METHODS = []
		def POST_PROCESS_HEADER = null
		def PATTERN = new StringBuilder()
		use(DOMCategory) {
			if (log.isTraceEnabled()) log.trace("Looking for splitters of type=" + pp_tag)
			def pttrns = root.custom.pp_splitters.splitter.findAll { it.'@tags' =~ pp_tag}
			if (log.isTraceEnabled()) log.trace("Patterns found=" + pttrns)
			if (pttrns != null) {
				pttrns.sort { it.'@order' }
				pttrns.each { ptrn_node ->
					String pttrn = config.getCDATA(ptrn_node)

					def sep_tag = ptrn_node.'@sep'

					if (sep_tag != null && POST_PROCESS_SEP == null) {

						if (sep_tag == '') {
							sep_tag = root.pp_config.'@default_sep'[0]
						}
						if (log.isTraceEnabled()) log.trace("Looking for separator=" + sep_tag)

						def sep = root.pp_config.pp_separators.separator.find { it.'@id' ==~ sep_tag}
						if (sep != null) {
							POST_PROCESS_SEP = sep.text()
							if (sep.'@spool' != null) config.setParam('SPOOLING_EXT', (sep.'@spool'))
						}
					}

					PATTERN = PATTERN.size() == 0 ? PATTERN.append("(?ms)").append(pttrn) : PATTERN.append(Qualifier.and.getPattern()).append(pttrn)
					def splitter_type = root.pp_config.pp_splitter_types.splitter_type.find { sp_type -> sp_type.'@id' ==~ ptrn_node.'@type' }
					def handler = splitter_type.'@handler'
					POST_PROCESS_DICT[pttrn] = handler
					if (splitter_type.'@handler_type' ==~ "group_method") {
						POST_GROUPS_METHODS.add(handler)
					}
					POST_PROCESS_HEADER = (POST_PROCESS_HEADER != null) ? POST_PROCESS_HEADER + POST_PROCESS_SEP + ptrn_node.'@col_name' : ptrn_node.'@col_name'
				}
				POST_PROCESS_HEADER += "\n"
			}
		}
		return ["POST_PROCESS_SEP":POST_PROCESS_SEP,
			"POST_PROCESS_DICT":POST_PROCESS_DICT,
			"POST_GROUPS_METHODS":POST_GROUPS_METHODS,
			"POST_PROCESS_HEADER":POST_PROCESS_HEADER,
			"PATTERN":PATTERN.toString()]
	}
	
	/**
	 * 
	 * Parses appropriate ComplexFilter params from config.xml
	 * 
	 * @param preserveTag "tag" attribute associated with thread preserving patterns in config.xml
	 * @param config Initialized WgrepConfig
	 * @return Mapping of ComplexFilter params
	 */
	static Map parseComplexFilterParams(String preserveTag, WgrepConfig config) {
		def root = config.getParam('root')
		def pt_tag = preserveTag
		def cfParams = [:]
		use(DOMCategory) {
			if (pt_tag != null) {
				cfParams['THRD_START_EXTRCTRS'] = root.custom.thread_configs.extractors.pattern.findAll { it.'@tags' =~ pt_tag }.collect{it.text()}
				cfParams['THRD_SKIP_END_PTTRNS'] = root.custom.thread_configs.skipends.pattern.findAll { it.'@tags' =~ pt_tag }.collect{it.text()}
				cfParams['THRD_END_PTTRNS'] = root.custom.thread_configs.ends.pattern.findAll { it.'@tags' =~ pt_tag }.collect{it.text()}
			}
		}
		cfParams['FILTER_PATTERN'] = config.getParam('FILTER_PATTERN') 
		return cfParams
	}
	
	/**
	 * Creates filter chain for entries depending on fulfilled parameters in the config. <br>
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

	static FilterBase createFilterChainByConfig(WgrepConfig config) {
		FilterBase filterChain_ = null

		if (config.getParam('POST_PROCESSING') != null) {
			filterChain_ = new PostFilter(filterChain_, parsePostFilterParams(config.getParam('POST_PROCESSING'), config))
		}

		if (config.getParam('DATE_TIME_FILTER') != null) {
			filterChain_ = new EntryDateFilter(filterChain_, config.getParam('LOG_DATE_PATTERN'), config.getParam('LOG_DATE_FORMAT'), config.getParam('FROM_DATE'), config.getParam('TO_DATE'))
		}

		if (config.getParam('FILTER_PATTERN') != null) {
			filterChain_ = new ComplexFilter(filterChain_,parseComplexFilterParams(config.getParam('PRESERVE_THREAD'), config))
		}

		if (config.getParam('LOG_ENTRY_PATTERN') != null) {
			filterChain_ = new LogEntryFilter(filterChain_, config.getParam('LOG_ENTRY_PATTERN'))
		}

		return filterChain_
	}


	/**
	 * Creates filter chain for log files depending on fulfilled parameters in the config. <br>
	 * <li> 0. {@link FileSortFilter} is always instantiated as last in chain. </li>
	 * <li> 1. {@link FileDateFilter} </li>
	 * <li> 2. {@link FileNameFilter} </li>
	 *
	 * <br> Only #1 is optional, if date time filtering is not configured.
	 *
	 * @param config Initialized WgrepConfig instance.
	 * @return appropriate to supplied config entry filter chain
	 */

	static FilterBase createFileFilterChainByConfig(WgrepConfig config) {
		FilterBase filterChain_ = new FileSortFilter()

		if (config.getParam('DATE_TIME_FILTER') != null) {
			filterChain_ = new FileDateFilter(filterChain_,  config)
		}
		return filterChain_
	}
}
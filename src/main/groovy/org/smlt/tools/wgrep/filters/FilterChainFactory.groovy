package org.smlt.tools.wgrep.filters

import groovy.util.logging.Slf4j;

import org.smlt.tools.wgrep.config.WgrepConfig;
import org.smlt.tools.wgrep.filters.entry.ComplexFilter;
import org.smlt.tools.wgrep.filters.entry.EntryDateFilter;
import org.smlt.tools.wgrep.filters.entry.LogEntryFilter;
import org.smlt.tools.wgrep.filters.entry.PostFilter;
import org.smlt.tools.wgrep.filters.entry.PrintFilter;
import org.smlt.tools.wgrep.filters.logfile.FileDateFilter;
import org.smlt.tools.wgrep.filters.logfile.FileNameFilter;
import org.smlt.tools.wgrep.filters.logfile.FileSortFilter;
import groovy.xml.dom.DOMCategory;

/**
 * Class which provide factory methods for filter chain creating. <br>
 * Currently the only sense is to create filters depending on initialized WgrepConfig, so there are not so many methods.
 * 
 * @author Alexander Semelit 
 */

@Slf4j
class FilterChainFactory
{

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

    static FilterBase createFilterChainByConfig(WgrepConfig config)
    {
        FilterBase filterChain_ = new PrintFilter()
		def root = config.getParam('root')
		
        if (config.getParam('POST_PROCESSING') != null)
        {
            filterChain_ = new PostFilter(filterChain_, config)
        } 
        
        if (config.getParam('DATE_TIME_FILTER') != null)
        {
            filterChain_ = new EntryDateFilter(filterChain_, config.getParam('LOG_DATE_PATTERN'), config.getParam('LOG_DATE_FORMAT'), config.getParam('FROM_DATE'), config.getParam('TO_DATE'))
        } 

        if (config.getParam('FILTER_PATTERN') != null)
        {
            def pt_tag = config.getParam('PRESERVE_THREAD')
            def preserveParams = [:]
            use(DOMCategory)
            {
                if (pt_tag != null)
                {
                    def extrctrs = root.custom.thread_configs.extractor.findAll { it.'@tags' =~ pt_tag }
                    def THRD_START_EXTRCTRS = [:]
                    extrctrs.each { THRD_START_EXTRCTRS[it.text()] = it.'@qlfr' }
                    preserveParams['THRD_START_EXTRCTRS'] = THRD_START_EXTRCTRS
                    def pttrns = root.custom.thread_configs.pattern.findAll { it.'@tags' =~ pt_tag }
                    pttrns.each { 
                        if (preserveParams[it.'@clct'] != null)
                        {
                            preserveParams[it.'@clct'].add(it.text())     
                        }
                        else
                        {
                            preserveParams[it.'@clct'] = [it.text()]   
                        }
                    }
                }
            }
            filterChain_ = new ComplexFilter(filterChain_, config.getParam('FILTER_PATTERN'), preserveParams)
        } 

        if (config.getParam('LOG_ENTRY_PATTERN') != null)
        {
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

    static FilterBase createFileFilterChainByConfig(WgrepConfig config)
    {
        FilterBase filterChain_ = new FileSortFilter()
        
        if (config.getParam('DATE_TIME_FILTER') != null)
        {
            filterChain_ = new FileDateFilter(filterChain_,  config)
        } 

        filterChain_ = new FileNameFilter(filterChain_, config.getParam('FOLDER_SEPARATOR'), config.getParam('CWD'))
        return filterChain_
    }

}
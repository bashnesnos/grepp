package org.smlt.tools.wgrep.filters

import groovy.util.logging.Slf4j;

import org.smlt.tools.wgrep.WgrepConfig

@Slf4j
class FilterChainFactory
{

    static FilterBase createFilterChainByConfig(WgrepConfig config)
    {
        FilterBase filterChain_ = new PrintFilter()
        if (config.getParam('POST_PROCESSING') != null)
        {
            filterChain_ = new PostFilter(filterChain_, config)
        } 
        
        if (config.getParam('DATE_TIME_FILTER') != null)
        {
            filterChain_ = new EntryDateFilter(filterChain_, config)
        } 

        if (config.getParam('FILTER_PATTERN'))
        {
            filterChain_ = new ComplexFilter(filterChain_, config)
        } 

        if (config.getParam('LOG_ENTRY_PATTERN'))
        {
            filterChain_ = new LogEntryFilter(filterChain_, config)
        }

        return filterChain_
    }

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
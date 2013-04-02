package org.smlt.tools.wgrep.filters

import org.smlt.tools.wgrep.WgrepFacade

class FilterChainFactory
{

    static FilterBase createFilterChainByFacade()
    {
        FilterBase filterChain_ = new PrintFilter()
        WgrepFacade facade = WgrepFacade.getInstance()
        if (facade.getParam('POST_PROCESSING') != null)
        {
            filterChain_ = new PostFilter(filterChain_)
        } 
        
        if (facade.getParam('DATE_TIME_FILTER') != null)
        {
            filterChain_ = new EntryDateFilter(filterChain_)
        } 

        if (facade.getParam('EXTNDD_PATTERN') != null || facade.getParam('PRESERVE_THREAD') != null)
        {
            filterChain_ = new ComplexFilter(filterChain_)
        } 
        else
        {
            filterChain_ = new BasicFilter(filterChain_)
        }

        if (facade.getParam('LOG_ENTRY_PATTERN'))
        {
            filterChain_ = new LogEntryFilter(filterChain_)
        }
        return filterChain_
    }

    static FilterBase createFileFilterChainByFacade()
    {
        FilterBase filterChain_ = new FileSortFilter()
        WgrepFacade facade = WgrepFacade.getInstance()
        
        if (facade.getParam('DATE_TIME_FILTER') != null)
        {
            filterChain_ = new FileDateFilter(filterChain_)
        } 

        filterChain_ = new FileNameFilter(filterChain_)   
        return filterChain_
    }

}
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
            filterChain_ = new PostFilter(filterChain_, facade.getParam('POST_PROCESSING'))
        } 
        
        if (facade.getParam('DATE_TIME_FILTER') != null)
        {
            filterChain_ = new EntryDateFilter(filterChain_, facade.getParam('FROM_DATE'), facade.getParam('TO_DATE'))
        } 

        if (facade.getParam('FILTER_PATTERN'))
        {
            filterChain_ = new ComplexFilter(filterChain_, facade.getParam('FILTER_PATTERN'), facade.getParam('PRESERVE_THREAD'))
        } 

        if (facade.getParam('LOG_ENTRY_PATTERN'))
        {
            filterChain_ = new LogEntryFilter(filterChain_, facade.getParam('LOG_ENTRY_PATTERN'))
        }

        return filterChain_
    }

    static FilterBase createFileFilterChainByFacade()
    {
        FilterBase filterChain_ = new FileSortFilter()
        WgrepFacade facade = WgrepFacade.getInstance()
        
        if (facade.getParam('DATE_TIME_FILTER') != null)
        {
            filterChain_ = new FileDateFilter(filterChain_,  facade.getParam('FILE_DATE_FORMAT'), facade.getParam('FROM_DATE'), facade.getParam('TO_DATE'))
        } 

        filterChain_ = new FileNameFilter(filterChain_, facade.getParam('FOLDER_SEPARATOR'), facade.getParam('CWD'))
        return filterChain_
    }

}
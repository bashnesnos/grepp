package org.smlt.tools.wgrep.filters

import java.util.regex.Matcher

class BasicFilter extends FilterBase{

	BasicFilter(FilterBase nextFilter_) {
		super(nextFilter_, getFacade().getParam('FILTER_PATTERN'))
        if (isTraceEnabled()) trace("Added on top of " + nextFilter.getClass().getCanonicalName())
	}

    /**
    * Basic filter method.
    * <p> 
    * Is called against each block. Current sequence is following:
    * <li>1. Checks if block contains {@link FILTER_PATTERN}</li>
    * <li>2. Passes block and matching result to {@link processComplexBlock} method</li>
    * <li>3. Passes block to {@link postProcessBlockData} method</li>
    * <li>4. Passes the result of step 3 to {@link printBlock} method</li>
    *
    * @param blockData A String to be filtered.
    */

    def filter(def blockData)
    {
        if (isTraceEnabled()) trace("Filtering with /" + filterPtrn + "/")
        Matcher blockMtchr = blockData =~ filterPtrn
        if (blockMtchr.find()) 
        {
        	if (nextFilter != null) 
        	{
        		if (isTraceEnabled()) trace("Passing to next filter")
        		nextFilter.filter(blockData)	
        	}
        	else 
        	{
        		throw new RuntimeException("shouldn't be the last in chain")
        	}
        }
        else
        {
        	if (isTraceEnabled()) trace("not passed")
        }  
    }
}
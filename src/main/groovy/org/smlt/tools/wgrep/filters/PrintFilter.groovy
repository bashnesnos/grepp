package org.smlt.tools.wgrep.filters

class PrintFilter extends FilterBase{

	PrintFilter(FilterBase nextFilter_) {
		super(nextFilter_, null)
	}

    /**
    * Default filter method. Prints passed data block.
    * <p> 
    * Is considered to be last in line.
    *
    * @param blockData A String to be filtered.
    */

    def filter(def blockData)
    {
        if (blockData != null) 
        {
            getFacade().printBlock(blockData)
            if (nextFilter != null)
            {
                nextFilter.filter(blockData)
            }
        }
        else
        {
            if (isTraceEnabled()) trace("PrintFilter not passed")
        }
    }

}
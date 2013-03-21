package org.smlt.tools.wgrep

class PrintFilter extends ModuleBase{
	private nextFilter

	PrintFilter(def nextOne) {
		nextFilter = nextOne
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
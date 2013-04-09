package org.smlt.tools.wgrep.filters

import groovy.util.logging.Slf4j

import java.util.regex.Matcher

@Slf4j
class BasicFilter extends FilterBase{

	BasicFilter(FilterBase nextFilter_, def filterPtrn_) {
		super(nextFilter_)
		setPattern(filterPtrn_)
		log.trace("Added on top of " + nextFilter.getClass().getCanonicalName())
	}

	/**
	 * Basic filter method.
	 * <p> 
	 * Is called against each block.
	 *
	 * @param data A String to be filtered.
	 */

	def filter(def data) {
		log.trace("Filtering with /" + filterPtrn + "/")
		Matcher blockMtchr = data =~ filterPtrn
		if (blockMtchr.find()) {
			if (nextFilter != null) {
				log.trace("Passing to next filter")
				nextFilter.filter(data)
			}
			else {
				throw new RuntimeException("shouldn't be the last in chain")
			}
		}
		else {
			log.trace("not passed")
		}
	}
}
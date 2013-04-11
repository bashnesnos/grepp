package org.smlt.tools.wgrep.filters.entry

import groovy.util.logging.Slf4j

import java.util.regex.Matcher
import org.smlt.tools.wgrep.filters.FilterBase

/**
 * Class which provides simple matching of a line to a pattern. <br> 
 * Is simple and do not requires instantiated WgrepConfig for any delegation.
 * 
 * @author Alexander Semelit
 *
 */
@Slf4j
class BasicFilter extends FilterBase{

	/**
	 * Creates new BasicFilter with pointer to a next filter in chain and a filter pattern.
	 * 
	 * @param nextFilter_ FilterBase instance
	 * @param filterPtrn_ String expected. Would be used as a regex pattern.
	 */
	BasicFilter(FilterBase nextFilter_, def filterPtrn_) {
		super(nextFilter_)
		setPattern(filterPtrn_)
		log.trace("Added on top of " + nextFilter.getClass().getCanonicalName())
	}

	/** 
	 * Simply matches supplied String to filterPtrn. 
	 *
	 * @throws IllegalArgumentException if blockData is not String
	 */
	@Override
	boolean check(def blockData) {
		if (!blockData instanceof String) throw new IllegalArgumentException("blockData should be String")
		log.trace("Filtering with /" + filterPtrn + "/")
		Matcher blockMtchr = blockData =~ filterPtrn
		return blockMtchr.find()
	}
}
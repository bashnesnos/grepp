package org.smlt.tools.wgrep.filters.logfile

import org.smlt.tools.wgrep.filters.FilterBase

/**
 * Provides file sorting. Sorts files ascending by last modified time. <br>
 * Is a simple filter, i.e. does not require WgrepConfig to work.
 * 
 * @author Alexander Semelit 
 */

class FileSortFilter extends FilterBase {

	protected List fileList = []

	FileSortFilter(FilterBase nextFilter_) {
		super(nextFilter_, FileSortFilter.class)
	}

	/**
	*
	* Passes sorted collection to next filter.
	*/
	@Override
	boolean check(def files) {
		if (! files instanceof List<File> ) throw new IllegalArgumentException("FileSortFilter accepts file list only")
		fileList = [] //invalidating fileList
		fileList = sortFiles(files)
		return fileList != null && fileList.size() > 0
	}

	@Override
	void beforePassing(def files) {
		passingVal = fileList
	}

	/**
	*
	* Does actual sorting. Return cloned and sorted collection.
	*/
	List<File> sortFiles(List<File> files) {
		if (files == null) return files
		if (files.size() < 2) return files
		def fileList = files.clone()
		fileList.sort { it.lastModified() }
		if (log.isTraceEnabled()) log.trace("FileList has been sorted. ")
		return fileList
	}
}
package org.smltools.grepp.filters.logfile

import org.smltools.grepp.filters.FilterBase

/**
 * Provides file sorting. Sorts files ascending by last modified time. <br>
 * Is a simple filter, i.e. does not require config to work.
 * 
 * @author Alexander Semelit 
 */

public class FileSortFilter extends Filter<List<File>> {
	protected final Logger LOGGER = LoggerFactory.getLogger(FileSortFilter.class);

	/**
	*
	* Passes sorted collection to next filter.
	*/
	@Override
	public List<File> filter(String files) {
		if (files == null) return files
		if (files.size() < 2) return files

		List<File> fileList = new ArrayList<File>() 
		fileList.addAll(files)
		fileList.sort { it.lastModified() }
		LOGGER.trace("FileList has been sorted.")
		return fileList
	}

}
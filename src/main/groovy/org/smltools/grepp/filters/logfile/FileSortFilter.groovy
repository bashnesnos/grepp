package org.smltools.grepp.filters.logfile

import groovy.util.logging.Slf4j
import org.smltools.grepp.filters.Filter
import org.smltools.grepp.filters.FilterParams

/**
 * Provides file sorting. Sorts files ascending by last modified time. <br>
 * Is a simple filter, i.e. does not require config to work.
 * 
 * @author Alexander Semelit 
 */
@Slf4j("LOGGER")
@FilterParams(order = 5)
public class FileSortFilter implements Filter<List<File>> {

	/**
	*
	* Passes sorted collection to next filter.
	*/
	@Override
	public List<File> filter(List<File> files) {
		if (files == null) return files
		if (files.size() < 2) return files

		List<File> fileList = new ArrayList<File>() 
		fileList.addAll(files)
		fileList.sort { it.lastModified() }
		LOGGER.trace("FileList has been sorted.")
		return fileList
	}

}
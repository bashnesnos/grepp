package org.smlt.tools.wgrep.filters

import groovy.util.logging.Slf4j;

@Slf4j
class FileSortFilter extends FilterBase {

	FileSortFilter(FilterBase nextFilter_) {
		super(nextFilter_)
	}

	def filter(def files) {
		if (! files instanceof List<File> ) throw new IllegalArgumentException("FileSortFilter accepts file list only")

		List<File> files_ = sortFiles(files)

		if (files_ != null) {
			if (nextFilter != null) {
				log.trace("Passing to next filter")
				nextFilter.filter(files_)
			}
			else {
				log.trace("passed")
				return files_
			}
		}
		else {
			log.trace("not passed")
			return Collections.emptyList()
		}
	}

	List<File> sortFiles(List<File> files) {
		if (files == null) return files
		if (files.size() < 2) return files
		def fileList = files.clone()
		fileList.sort { it.lastModified() }
		log.trace("FileList has been sorted. ")
		return fileList
	}
}
package org.smlt.tools.wgrep.config.varparsers

import java.io.File;
import java.util.List;

import groovy.util.logging.Slf4j;
import org.smlt.tools.wgrep.config.WgrepConfig

/**
 * Provides file name parameter parsing. <br>
 * Simply adds supplied argument to FILES parameter of WgrepConfig instance
 * 
 * @author Alexander Semelit
 *
 */
@Slf4j
class FileNameParser extends ParserBase {
	def fSeparator
	def curDir

	FileNameParser(WgrepConfig config) {
		super(config)
		fSeparator = getParam('FOLDER_SEPARATOR')
	}

	/**
	 * Never unsubscribes, since there could be supplied more than one filename.
	 * 
	 */
	@Override
	void parseVar(def arg) {
		log.trace("Parsing var: " + arg)
		analyzeFileName(arg)
		//not unsubsrcibing since there could be more than one file
	}

	void analyzeFileName(String fileName) {
		List<File> fileList = []

		if (log.isTraceEnabled()) log.trace("analyzing supplied file: " + fileName);
		if (fileName =~ /\*/) { //filename contains asterisk, should be a multi-file pattern
			String flname = fileName;
			if (fileName =~ fSeparator) {
				curDir = (fileName =~/.*(?=$fSeparator)/)[0]
				flname = (fileName =~ /.*$fSeparator(.*)/)[0][1]
			}
			List<File> files = new File(curDir).listFiles();
			if (log.isTraceEnabled()) log.trace("files found " + files)
			String ptrn = flname.replaceAll(/\*/) {it - '*' + '.*'};
			if (log.isTraceEnabled()) log.trace("matching ptrn " + ptrn)
			files.each { file ->
				if (file.name ==~ /$ptrn/) {
					if (log.isTraceEnabled()) log.trace("adding file " + file)
					fileList.add(file)
				}
			}
		}
		else { //all good seems to be a normal file, just adding it
			addFile(new File(fileName))
		}

		fileList.each{addFile(it)}
	}
}
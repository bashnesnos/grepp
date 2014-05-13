package org.smltools.grepp.config.varparsers

import java.io.File;
import java.util.List;
import java.util.Map;

import org.smltools.grepp.config.ConfigHolder;
import groovy.util.logging.Slf4j;

/**
 * Provides file name parameter parsing. <br>
 * Simply adds supplied argument to FILES parameter of WgrepConfig instance
 * 
 * @author Alexander Semelit
 *
 */
@Slf4j
class FileNameParser implements ParamParser<String> {
	private static final String FOLDER_SEPARATOR_KEY = "folderSeparator"
	private static final String FILES_KEY = "files"
	@Override
	public boolean parseVar(ConfigHolder config, String fileName) {
		return parseVarInternal(config, fileName)
	}

	private boolean parseVarInternal(ConfigHolder config, String fileName) {
		List<File> fileList = []
		def fSeparator = config.runtime."$FOLDER_SEPARATOR_KEY"
		def curDir = config.runtime.cwd

		log.trace("analyzing supplied file: {}", fileName)
		if (fileName =~ /\*/) {
			//filename contains asterisk, should be a multi-file pattern
			String flname = fileName
			if (fileName =~ fSeparator) {
				if (curDir == null) { 
					curDir = new File((fileName =~/.*(?=$fSeparator)/)[0])
				}
				else {
					log.debug("Directory is limited to {}", curDir.getAbsolutePath())
				}
				flname = (fileName =~ /.*$fSeparator(.*)/)[0][1]
			}
			List<File> files = curDir.listFiles()
			if (log.isTraceEnabled()) log.trace("files found " + files)
			String ptrn = flname.replaceAll(/\*/) {it - '*' + '.*'}
			if (log.isTraceEnabled()) log.trace("matching ptrn " + ptrn)
			files.each { file ->
				if (file.name ==~ /$ptrn/) {
					if (log.isTraceEnabled()) log.trace("adding file " + file)
					fileList.add(file)
				}
			}
		}
		else { //all good seems to be a normal file, just adding it
			if (curDir != null) {
				log.debug("Limiting directory to {}", curDir.getAbsolutePath())
				if (fileName =~ fSeparator) {
					fileName = (fileName =~ /.*$fSeparator(.*)/)[0][1]
				}
				fileName = "${curDir.getAbsolutePath()}$fSeparator$fileName"
			}

			fileList.add(new File(fileName))
		}
		
		List<File> files = config.runtime.data.containsKey(FILES_KEY) ? config.runtime.data.files : null
		if (files != null) {
			files.addAll(fileList)
		}
		else {
			config.runtime.data.files = fileList
		}
		
		// Never unsubscribes, since there could be supplied more than one filename.
		return false;
	}
}
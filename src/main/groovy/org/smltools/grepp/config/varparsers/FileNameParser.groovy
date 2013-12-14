package org.smltools.grepp.config.varparsers

import java.io.File;
import java.util.List;
import java.util.Map;

import org.smltools.grepp.config.ConfigHolder;
import org.smltools.grepp.config.Param;

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
	
	@Override
	public boolean parseVar(ConfigHolder config, Map<Param, ?> params,
			String fileName) {
		return parseVar_(params, fileName)
	}

	private boolean parseVar_(Map<Param, ?> params, String fileName) {
		List<File> fileList = []
		def fSeparator = params[Param.FOLDER_SEPARATOR]
		def curDir = params[Param.CWD]

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
		
		List<File> files = params[Param.FILES] 
		if (files != null) {
			files.addAll(fileList)
		}
		else {
			params[Param.FILES] = fileList
		}
		
		// Never unsubscribes, since there could be supplied more than one filename.
		return false;
	}
}
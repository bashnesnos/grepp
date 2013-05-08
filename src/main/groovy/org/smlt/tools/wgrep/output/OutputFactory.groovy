package org.smlt.tools.wgrep.output;

import org.smlt.tools.wgrep.config.*

class OutputFactory extends ModuleBase {
		
	public WgrepOutput getOutputInstance() {
		if (getParam('SPOOLING') != null) {
			return new FileOutput(new File(new File(getParam('HOME_DIR')), getParam('RESULTS_DIR')).getCanonicalPath(), getParam('FILTER_PATTERN').replaceAll("[^\\p{L}\\p{N}]", {""}) + getParam('SPOOLING_EXT'))
		}
		else
		{
			return new ConsoleOutput()
		}
	}
}
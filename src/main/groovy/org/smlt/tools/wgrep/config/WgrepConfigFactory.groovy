package org.smlt.tools.wgrep.config

import org.springframework.core.io.Resource
import org.springframework.beans.factory.annotation.Autowired

class WgrepConfigFactory {
	private Resource configFile
	private Resource configXSD

	Resource getConfigFile(Resource res) {
		configFile = res
	}

	Resource getConfigXSD(Resource res) {
		configXSD = res
	}

	void setConfigFile(Resource res) {
		configFile = res
	}

	void setConfigXSD(Resource res) {
		configXSD = res
	}

	public WgrepConfig getInstance()  {
		return new WgrepConfig(configFile.getFile().getCanonicalPath(), configXSD.getFile().getCanonicalPath())
	}
}

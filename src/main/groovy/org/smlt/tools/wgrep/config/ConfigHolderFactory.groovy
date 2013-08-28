package org.smlt.tools.wgrep.config

import org.springframework.core.io.Resource

public class ConfigHolderFactory {
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

	public ConfigHolder getInstance()  {
		if (configFile == null || configFile.getFile() == null) {
			throw new IllegalArgumentException("Config file should present in classpath or specified explicitly")
		}
		def configXSDFile = configXSD.getFile()
		return new ConfigHolder(configFile.getFile().getCanonicalPath(), configXSDFile != null ? configXSDFile.getCanonicalPath() : null)
	}
}
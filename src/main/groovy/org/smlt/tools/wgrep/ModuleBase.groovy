package org.smlt.tools.wgrep

import groovy.util.logging.Slf4j;

@Slf4j
class ModuleBase {

	static WgrepFacade getFacade() {
		return WgrepFacade.getInstance()
	}

	def getRoot() {
		return getFacade().getRoot()
	}

	def getCDATA(def node) {
		return getFacade().getCDATA(node)
	}
}
package org.smltools.grepp.util;

import groovy.util.ConfigObject

public class SavedConfigBuilder {
	private String id
	private String dateFormat
	private String dateRegex
	private String starter
	private Integer logThreshold
	private String pattern
	
	public SavedConfigBuilder setId(String id) {
		this.id = id
		return this
	}

	public SavedConfigBuilder setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat
		return this
	}

	public SavedConfigBuilder setDateRegex(String dateRegex) {
		this.dateRegex = dateRegex
		return this
	}
	
	public SavedConfigBuilder setStarter(String starter) {
		this.starter = starter
		return this
	}

	public SavedConfigBuilder setLogThreshold(Integer logThreshold) {
		this.logThreshold = logThreshold
		return this
	}
	
	public SavedConfigBuilder setPattern(String pattern) {
		this.pattern = pattern
		return this
	}

	public String buildSavedConfig() {
		if (id == null || id.equals("")) {//pointless to create if config doesn't have an identifier
			return null
		}

		def config = new ConfigObject()
		def props = config.savedConfigs."$id"
		if (dateFormat != null) {
			props.dateFormat.value = dateFormat
			config.logDateFormats."$id".value = props.dateFormat.value				
		}

		if (dateRegex != null) {
			props.dateFormat.regex = "(" + dateRegex + ")"
			config.logDateFormats."$id".regex = props.dateFormat.regex
		}
		
		if (starter != null && !starter.equals(dateRegex)) {
			props.starter = starter + (dateRegex != null ? '.*' : '')
		}

		if (logThreshold != null) {
			config.logThreshold = logThreshold
		}

		if (pattern != null) {
			props.pattern = pattern
		}

		def writer = new StringWriter()
		config.writeTo(writer)
		return GreppUtil.escapeRegexes(writer.toString())
	}
}
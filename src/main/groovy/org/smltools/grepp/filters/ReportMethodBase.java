package org.smltools.grepp.filters;

import java.util.regex.Pattern;

public abstract class ReportMethodBase<T> implements ReportMethod<T> {
    protected Pattern pattern;
    protected String colName;

    public Pattern getPattern() {
    	return pattern;
    }

    public void setPattern(Pattern pattern) {
    	this.pattern = pattern;
    }

    public String getColName() {
    	return colName;
    }

    public void setColName(String colName) {
    	this.colName = colName;
    }
}
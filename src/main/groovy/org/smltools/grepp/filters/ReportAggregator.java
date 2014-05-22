package org.smltools.grepp.filters;

public interface ReportAggregator {
	String getId();
    String getSpoolFileExtension();
    void addColumn(String columnName);
    ReportAggregator addRow();
    ReportAggregator addCell(String value);
    ReportAggregator addHeader();
    String buildRow();    
    String buildReport();
    void flush();
}

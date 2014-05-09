package org.smltools.grepp.filters;

public class StringAggregator implements Aggregator<String> {
	private StringBuilder internalAgg = new StringBuilder();

	public StringAggregator() {
		internalAgg.setLength(0);
	}

	public StringAggregator add(String data) {
		if (data != null) {
			internalAgg.append(data);
		}
	}

	public String aggregate() {
		try {
			return internalAgg.length() > 0 ? internalAgg.toString() : null;
		}
		finally {
			internalAgg.setLength(0);
		}
	}
}
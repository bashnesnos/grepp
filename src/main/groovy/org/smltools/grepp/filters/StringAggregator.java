package org.smltools.grepp.filters;

public class StringAggregator implements Aggregator<String> {
	private final StringBuilder internalAgg = new StringBuilder();

	public StringAggregator() {
		internalAgg.setLength(0);
	}

        @Override
	public StringAggregator add(String data) {
		if (data != null) {
			internalAgg.append(data);
		}
                return this;
	}

        @Override
        public String aggregate() {
		try {
			return internalAgg.length() > 0 ? internalAgg.toString() : null;
		}
		finally {
			internalAgg.setLength(0);
		}
	}
}
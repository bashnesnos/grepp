package org.smltools.grepp.filters;

import java.util.regex.Matcher;

public interface ReportMethod<T> {
    T processMatchResults(Matcher mtchResults, Integer groupIdx);
}
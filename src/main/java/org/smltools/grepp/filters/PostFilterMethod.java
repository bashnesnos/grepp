package org.smltools.grepp.filters;

import java.util.regex.Matcher;

public interface PostFilterMethod<T> {
    T processMatchResults(Matcher mtchResults, Integer groupIdx);
}
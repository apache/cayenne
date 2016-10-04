/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.dbsync.filter;

import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Provides name pattern matching functionality.
 * 
 * @since 1.2
 */
public class NamePatternMatcher implements NameFilter {

    public static final NameFilter EXCLUDE_ALL = new NameFilter() {

        @Override
        public boolean isIncluded(String obj) {
            return false;
        }
    };

    private static final String[] EMPTY_ARRAY = new String[0];
    private static final Pattern COMMA = Pattern.compile(",");

    private final Pattern[] itemIncludeFilters;
    private final Pattern[] itemExcludeFilters;

    public static NamePatternMatcher build(Log logger, String includePattern, String excludePattern) {
        return new NamePatternMatcher(createPatterns(logger, includePattern), createPatterns(logger, excludePattern));
    }

    public NamePatternMatcher(Pattern[] itemIncludeFilters, Pattern[] itemExcludeFilters) {
        this.itemIncludeFilters = itemIncludeFilters;
        this.itemExcludeFilters = itemExcludeFilters;
    }

    /**
     * Applies preconfigured list of filters to the list, removing entities that do not
     * pass the filter.
     * 
     * @deprecated since 3.0 still used by AntDataPortDelegate, which itself should
     *             probably be deprecated
     */
    @Deprecated
    public List<?> filter(List<?> items) {
        if (items == null || items.isEmpty()) {
            return items;
        }

        if (itemIncludeFilters.length == 0 && itemExcludeFilters.length == 0) {
            return items;
        }

        Iterator<?> it = items.iterator();
        while (it.hasNext()) {
            CayenneMapEntry entity = (CayenneMapEntry) it.next();

            if (!passedIncludeFilter(entity.getName())) {
                it.remove();
                continue;
            }

            if (!passedExcludeFilter(entity.getName())) {
                it.remove();
            }
        }

        return items;
    }

    /**
     * Returns an array of Patterns. Takes a comma-separated list of patterns, attempting
     * to convert them to the java.util.regex.Pattern syntax. E.g.
     * <p>
     * <code>"billing_*,user?"</code> will become an array of two expressions:
     * <p>
     * <code>^billing_.*$</code><br>
     * <code>^user.?$</code><br>
     */
    public static Pattern[] createPatterns(Log logger, String patternString) {
        if (patternString == null) {
            return new Pattern[0];
        }
        String[] patternStrings = tokenizePattern(patternString);
        List<Pattern> patterns = new ArrayList<Pattern>(patternStrings.length);

        for (String patternString1 : patternStrings) {

            // test the pattern
            try {
                patterns.add(Pattern.compile(patternString1));
            } catch (PatternSyntaxException e) {

                if (logger != null) {
                    logger.warn("Ignoring invalid pattern [" + patternString1 + "], reason: " + e.getMessage());
                }
            }
        }

        return patterns.toArray(new Pattern[patterns.size()]);
    }

    /**
     * Returns an array of valid regular expressions. Takes a comma-separated list of
     * patterns, attempting to convert them to the java.util.regex.Pattern syntax. E.g.
     * <p>
     * <code>"billing_*,user?"</code> will become an array of two expressions:
     * <p>
     * <code>^billing_.*$</code><br>
     * <code>^user.?$</code><br>
     */
    public static String[] tokenizePattern(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return EMPTY_ARRAY;
        }

        String[] patterns = COMMA.split(pattern);
        if (patterns.length == 0) {
            return EMPTY_ARRAY;
        }

        for (int i = 0; i < patterns.length; i++) {
            // convert * into regex syntax
            // e.g. abc*x becomes ^abc.*x$
            // or abc?x becomes ^abc.?x$
            patterns[i] = "^" + patterns[i].replaceAll("[*?]", ".$0") + "$";
        }

        return patterns;
    }

    /**
     * Returns true if a given object property satisfies the include/exclude patterns.
     * 
     * @since 3.0
     */
    @Override
    public boolean isIncluded(String string) {
        return passedIncludeFilter(string) && passedExcludeFilter(string);
    }

    /**
     * Returns true if an object matches any one of the "include" patterns, or if there is
     * no "include" patterns defined.
     * 
     * @since 3.0
     */
    private boolean passedIncludeFilter(String item) {
        if (itemIncludeFilters.length == 0) {
            return true;
        }

        for (Pattern itemIncludeFilter : itemIncludeFilters) {
            if (itemIncludeFilter.matcher(item).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if an object does not match any one of the "exclude" patterns, or if
     * there is no "exclude" patterns defined.
     * 
     * @since 3.0
     */
    private boolean passedExcludeFilter(String item) {
        if (itemExcludeFilters.length == 0) {
            return true;
        }

        for (Pattern itemExcludeFilter : itemExcludeFilters) {
            if (itemExcludeFilter.matcher(item).find()) {
                return false;
            }
        }

        return true;
    }

    public static String replaceWildcardInStringWithString(
            String wildcard,
            String pattern,
            String replacement) {

        if (pattern == null || wildcard == null) {
            return pattern;
        }

        StringBuilder buffer = new StringBuilder();
        int lastPos = 0;
        int wildCardPos = pattern.indexOf(wildcard);
        while (wildCardPos != -1) {
            if (lastPos != wildCardPos) {
                buffer.append(pattern.substring(lastPos, wildCardPos));
            }
            buffer.append(replacement);
            lastPos += wildCardPos + wildcard.length();
            wildCardPos = pattern.indexOf(wildcard, lastPos);
        }

        if (lastPos < pattern.length()) {
            buffer.append(pattern.substring(lastPos));
        }

        return buffer.toString();
    }
}

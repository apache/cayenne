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

package org.apache.cayenne.tools;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.commons.logging.Log;

/**
 * Provides name pattern matching functionality.
 * 
 * @since 1.2
 */
public class NamePatternMatcher {

    protected Log logger;

    protected Pattern[] itemIncludeFilters;
    protected Pattern[] itemExcludeFilters;

    public NamePatternMatcher(Log logger, String includePattern, String excludePattern) {
        this.logger = logger;
        this.itemIncludeFilters = createPatterns(includePattern);
        this.itemExcludeFilters = createPatterns(excludePattern);
    }

    /**
     * Applies preconfigured list of filters to the list, removing entities that do not
     * pass the filter.
     * 
     * @deprecated since 3.0 still used by AntDataPortDelegate, which itself should
     *             probably be deprecated
     */
    @Deprecated
    List<?> filter(List<?> items) {
        if (items == null || items.isEmpty()) {
            return items;
        }

        if ((itemIncludeFilters.length == 0) && (itemExcludeFilters.length == 0)) {
            return items;
        }

        Iterator<?> it = items.iterator();
        while (it.hasNext()) {
            CayenneMapEntry entity = (CayenneMapEntry) it.next();

            if (!passedIncludeFilter(entity)) {
                it.remove();
                continue;
            }

            if (!passedExcludeFilter(entity)) {
                it.remove();
            }
        }

        return items;
    }

    /**
     * Returns true if the entity matches any one of the "include" patterns, or if there
     * is no "include" patterns defined.
     * 
     * @deprecated since 3.0. still used by AntDataPortDelegate, which itself should
     *             probably be deprecated
     */
    @Deprecated
    private boolean passedIncludeFilter(CayenneMapEntry item) {
        if (itemIncludeFilters.length == 0) {
            return true;
        }

        String itemName = item.getName();
        for (Pattern itemIncludeFilter : itemIncludeFilters) {
            if (itemIncludeFilter.matcher(itemName).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if the entity does not match any one of the "exclude" patterns, or if
     * there is no "exclude" patterns defined.
     * 
     * @deprecated since 3.0
     */
    @Deprecated
    private boolean passedExcludeFilter(CayenneMapEntry item) {
        if (itemExcludeFilters.length == 0) {
            return true;
        }

        String itemName = item.getName();
        for (Pattern itemExcludeFilter : itemExcludeFilters) {
            if (itemExcludeFilter.matcher(itemName).find()) {
                return false;
            }
        }

        return true;
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
    public Pattern[] createPatterns(String patternString) {
        String[] patternStrings = tokenizePattern(patternString);
        List<Pattern> patterns = new ArrayList<Pattern>(patternStrings.length);

        for (int i = 0; i < patternStrings.length; i++) {

            // test the pattern
            try {
                patterns.add(Pattern.compile(patternStrings[i]));
            }
            catch (PatternSyntaxException e) {

                if (logger != null) {
                    logger.warn("Ignoring invalid pattern ["
                            + patternStrings[i]
                            + "], reason: "
                            + e.getMessage());
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
    public String[] tokenizePattern(String pattern) {
        if (pattern != null && pattern.length() > 0) {
            StringTokenizer toks = new StringTokenizer(pattern, ",");

            int len = toks.countTokens();
            if (len == 0) {
                return new String[0];
            }

            List<String> patterns = new ArrayList<String>(len);
            for (int i = 0; i < len; i++) {
                String nextPattern = toks.nextToken();
                StringBuilder buffer = new StringBuilder();

                // convert * into regex syntax
                // e.g. abc*x becomes ^abc.*x$
                // or abc?x becomes ^abc.?x$
                buffer.append("^");
                for (int j = 0; j < nextPattern.length(); j++) {
                    char nextChar = nextPattern.charAt(j);
                    if (nextChar == '*' || nextChar == '?') {
                        buffer.append('.');
                    }
                    buffer.append(nextChar);
                }
                buffer.append("$");
                patterns.add(buffer.toString());
            }

            return patterns.toArray(new String[patterns.size()]);
        }
        else {
            return new String[0];
        }
    }

    /**
     * Returns true if a given object property satisfies the include/exclude patterns.
     * 
     * @since 3.0
     */
    public boolean isIncluded(String string) {

        if ((itemIncludeFilters.length == 0) && (itemExcludeFilters.length == 0)) {
            return true;
        }

        if (!passedIncludeFilter(string)) {
            return false;
        }

        if (!passedExcludeFilter(string)) {
            return false;
        }

        return true;
    }

    /**
     * Returns true if an object matches any one of the "include" patterns, or if there is
     * no "include" patterns defined.
     * 
     * @since 3.0
     */
    boolean passedIncludeFilter(String item) {
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
    boolean passedExcludeFilter(String item) {
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
        if (null == pattern || null == wildcard)
            return pattern;

        StringBuilder buffer = new StringBuilder();
        int lastPos = 0;
        int wildCardPos = pattern.indexOf(wildcard);
        while (-1 != wildCardPos) {
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

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

/**
 * Provides name pattern matching functionality.
 * 
 * @author Andrus Adamchik, Mike Kienenberger
 * @since 1.2
 */
public class NamePatternMatcher {

    protected ILog logger;

    protected Pattern[] itemIncludeFilters;
    protected Pattern[] itemExcludeFilters;

    public NamePatternMatcher(ILog parentTask, String includePattern,
            String excludePattern) {
        this.logger = parentTask;
        this.itemIncludeFilters = createPatterns(includePattern);
        this.itemExcludeFilters = createPatterns(excludePattern);
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
        List patterns = new ArrayList(patternStrings.length);

        for (int i = 0; i < patternStrings.length; i++) {

            // test the pattern
            try {
                patterns.add(Pattern.compile(patternStrings[i]));
            }
            catch (PatternSyntaxException e) {

                if (logger != null) {
                    logger.log("Ignoring invalid pattern ["
                            + patternStrings[i]
                            + "], reason: "
                            + e.getMessage(), ILog.MSG_WARN);
                }
                continue;
            }
        }

        return (Pattern[]) patterns.toArray(new Pattern[patterns.size()]);
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

            List patterns = new ArrayList(len);
            for (int i = 0; i < len; i++) {
                String nextPattern = toks.nextToken();
                StringBuffer buffer = new StringBuffer();

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

            return (String[]) patterns.toArray(new String[patterns.size()]);
        }
        else {
            return new String[0];
        }
    }

    /**
     * Applies preconfigured list of filters to the list, removing entities that do not
     * pass the filter.
     */
    protected List filter(List items) {
        if (items == null || items.isEmpty()) {
            return items;
        }

        if ((itemIncludeFilters.length == 0) && (itemExcludeFilters.length == 0)) {
            return items;
        }

        Iterator it = items.iterator();
        while (it.hasNext()) {
            CayenneMapEntry entity = (CayenneMapEntry) it.next();

            if (!passedIncludeFilter(entity)) {
                it.remove();
                continue;
            }

            if (!passedExcludeFilter(entity)) {
                it.remove();
                continue;
            }
        }

        return items;
    }

    /**
     * Returns true if the entity matches any one of the "include" patterns, or if there
     * is no "include" patterns defined.
     */
    protected boolean passedIncludeFilter(CayenneMapEntry item) {
        if (itemIncludeFilters.length == 0) {
            return true;
        }

        String itemName = item.getName();
        for (int i = 0; i < itemIncludeFilters.length; i++) {
            if (itemIncludeFilters[i].matcher(itemName).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if the entity does not match any one of the "exclude" patterns, or if
     * there is no "exclude" patterns defined.
     */
    protected boolean passedExcludeFilter(CayenneMapEntry item) {
        if (itemExcludeFilters.length == 0) {
            return true;
        }

        String itemName = item.getName();
        for (int i = 0; i < itemExcludeFilters.length; i++) {
            if (itemExcludeFilters[i].matcher(itemName).find()) {
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

        StringBuffer buffer = new StringBuffer();
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

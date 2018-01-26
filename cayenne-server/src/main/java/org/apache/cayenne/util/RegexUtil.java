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

package org.apache.cayenne.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A collection of utility methods related to regular expressions processing.
 * 
 * @since 1.2
 */
class RegexUtil {

    static final Pattern BACKSLASH = Pattern.compile("\\\\");
    static final Pattern DOT = Pattern.compile("\\.");

    /**
     * Replaces all backslashes "\" with forward slashes "/". Convenience method to
     * convert path Strings to URI format.
     */
    static String substBackslashes(String string) {
        if (string == null) {
            return null;
        }

        Matcher matcher = BACKSLASH.matcher(string);
        return matcher.find() ? matcher.replaceAll("\\/") : string;
    }

    /**
     * Returns package name for the Java class as a path separated with forward slash
     * ("/"). Method is used to lookup resources that are located in package
     * subdirectories. For example, a String "a/b/c" will be returned for class name
     * "a.b.c.ClassName".
     */
    static String getPackagePath(String className) {
        if (className == null) {
            return "";
        }

        Matcher matcher = DOT.matcher(className);
        if (matcher.find()) {
            String path = matcher.replaceAll("\\/");
            return path.substring(0, path.lastIndexOf("/"));
        }
        else {
            return "";
        }
    }

    /**
     * Converts a SQL-style pattern to a valid Perl regular expression. E.g.:
     * <p>
     * <code>"billing_%"</code> will become <code>^billing_.*$</code>
     * <p>
     * <code>"user?"</code> will become <code>^user.?$</code>
     */
    static String sqlPatternToRegex(String pattern) {
        if (pattern == null) {
            throw new NullPointerException("Null pattern.");
        }

        if (pattern.length() == 0) {
            throw new IllegalArgumentException("Empty pattern.");
        }

        StringBuilder buffer = new StringBuilder();

        // convert * into regex syntax
        // e.g. abc*x becomes ^abc.*x$
        // or abc?x becomes ^abc.?x$
        buffer.append("^");
        for (int j = 0; j < pattern.length(); j++) {
            char nextChar = pattern.charAt(j);
            if (nextChar == '%') {
                nextChar = '*';
            }

            if (nextChar == '*' || nextChar == '?') {
                buffer.append('.');
            }
            // escape special chars
            else if (nextChar == '.'
                    || nextChar == '/'
                    || nextChar == '$'
                    || nextChar == '^') {
                buffer.append('\\');
            }

            buffer.append(nextChar);
        }

        buffer.append("$");
        return buffer.toString();
    }

    private RegexUtil() {
        super();
    }
}

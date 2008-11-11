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

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Utility class to convert from different naming styles to Java convention. For example
 * names like "ABCD_EFG" can be converted to "abcdEfg".
 * 
 */
public class NameConverter {

    private static final Map<String, String> SPECIAL_CHAR_TO_JAVA_MAPPING = new HashMap<String, String>();

    static {
        SPECIAL_CHAR_TO_JAVA_MAPPING.put("#", "pound");
    }

    /**
     * Converts a String name to a String forllowing java convention for the static final
     * variables. E.g. "abcXyz" will be converted to "ABC_XYZ".
     * 
     * @since 1.0.3
     */
    public static String javaToUnderscored(String name) {
        if (name == null) {
            return null;
        }

        // clear of non-java chars. While the method name implies that a passed identifier
        // is pure Java, it is used to build pk columns names and such, so extra safety
        // check is a good idea
        name = specialCharsToJava(name);

        char charArray[] = name.toCharArray();
        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < charArray.length; i++) {
            if ((Character.isUpperCase(charArray[i])) && (i != 0)) {

                char prevChar = charArray[i - 1];
                if ((Character.isLowerCase(prevChar))) {
                    buffer.append("_");
                }
            }

            buffer.append(Character.toUpperCase(charArray[i]));
        }

        return buffer.toString();
    }

    /**
     * Converts names like "ABCD_EFG_123" to Java-style names like "abcdEfg123". If
     * <code>capitalize</code> is true, returned name is capitalized (for instance if
     * this is a class name).
     * 
     * @since 1.2
     */
    public static String underscoredToJava(String name, boolean capitalize) {
        StringTokenizer st = new StringTokenizer(name, "_");
        StringBuilder buf = new StringBuilder();

        boolean first = true;
        while (st.hasMoreTokens()) {
            String token = st.nextToken();

            // clear of non-java chars
            token = specialCharsToJava(token);

            int len = token.length();
            if (len == 0) {
                continue;
            }

            // sniff mixed case vs. single case styles
            boolean hasLowerCase = false;
            boolean hasUpperCase = false;
            for (int i = 0; i < len && !(hasUpperCase && hasLowerCase); i++) {
                if (Character.isUpperCase(token.charAt(i))) {
                    hasUpperCase = true;
                }
                else if (Character.isLowerCase(token.charAt(i))) {
                    hasLowerCase = true;
                }
            }

            // if mixed case, preserve it, if all upper, convert to lower
            if (hasUpperCase && !hasLowerCase) {
                token = token.toLowerCase();
            }

            if (first) {
                // apply explicit capitalization rules, if this is the first token
                first = false;
                if (capitalize) {
                    buf.append(Character.toUpperCase(token.charAt(0)));
                }
                else {
                    buf.append(Character.toLowerCase(token.charAt(0)));
                }
            }
            else {
                buf.append(Character.toUpperCase(token.charAt(0)));
            }

            if (len > 1) {
                buf.append(token.substring(1, len));
            }
        }
        return buf.toString();
    }

    /**
     * Replaces special chars with human-readable and Java-id-compatible symbols.
     */
    public static String specialCharsToJava(String string) {
        int len = string.length();
        if (len == 0) {
            return string;
        }

        StringBuilder buffer = new StringBuilder(len);
        for (int i = 0; i < len; i++) {

            char c = string.charAt(i);
            if (Character.isJavaIdentifierPart(c)) {
                buffer.append(c);
            }
            else {
                Object word = SPECIAL_CHAR_TO_JAVA_MAPPING.get(String.valueOf(c));
                buffer.append(word != null ? word : "_");
            }
        }

        return buffer.toString();
    }
}

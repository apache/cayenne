/*
 *    Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */
package org.apache.cayenne.dbsync.naming;

/**
 * @since 4.0
 */
final class NameUtil {
    static String uncapitalize(String string) {
        int len;
        if (string == null || (len = string.length()) == 0) {
            return string;
        }

        final char firstChar = string.charAt(0);
        final char newChar = Character.toLowerCase(firstChar);
        if (firstChar == newChar) {
            // already capitalized
            return string;
        }

        char[] newChars = new char[len];
        newChars[0] = newChar;
        string.getChars(1, len, newChars, 1);

        return String.valueOf(newChars);
    }

    static String capitalize(String string) {
        int len;
        if (string == null || (len = string.length()) == 0) {
            return string;
        }

        final char firstChar = string.charAt(0);
        final char newChar = Character.toTitleCase(firstChar);
        if (firstChar == newChar) {
            // already capitalized
            return string;
        }

        char[] newChars = new char[len];
        newChars[0] = newChar;
        string.getChars(1, len, newChars, 1);

        return String.valueOf(newChars);
    }

}

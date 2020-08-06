/*
 *    Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */
package org.apache.cayenne.dbsync.naming;

import org.apache.cayenne.util.Util;

/**
 * @since 4.0
 */
final class NameUtil {

    private static String prepare(String string, boolean capitalize){
        if (Util.isEmptyString(string)) {
            return string;
        }

        final char firstChar = string.charAt(0);
        final char newChar = capitalize ? Character.toTitleCase(firstChar) : Character.toLowerCase(firstChar);
        if (firstChar == newChar) {
            // already capitalized
            return string;
        }

        int len = string.length();
        char[] newChars = new char[len];
        newChars[0] = newChar;
        string.getChars(1, len, newChars, 1);

        return String.valueOf(newChars);
    }

    static String uncapitalize(String string) {
        return prepare(string,false);
    }

    static String capitalize(String string) {
        return prepare(string,true);
    }

}

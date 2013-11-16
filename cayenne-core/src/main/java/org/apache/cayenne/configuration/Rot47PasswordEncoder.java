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

package org.apache.cayenne.configuration;

/**
 * The ROT-47 password encoder passes the text of the database password through a simple
 * Caesar cipher to obscure the password text. The ROT-47 cipher is similar to the ROT-13
 * cipher, but processes numbers and symbols as well. See the Wikipedia entry on <a
 * href="http://en.wikipedia.org/wiki/Rot-13">ROT13</a> for more information on this
 * topic.
 * 
 * @since 3.0
 */
public class Rot47PasswordEncoder implements PasswordEncoding {

    public String decodePassword(String encodedPassword, String key) {
        return rotate(encodedPassword);
    }

    public String encodePassword(String normalPassword, String key) {
        return rotate(normalPassword);
    }

    /**
     * Applies a ROT-47 Caesar cipher to the supplied value. Each letter in the supplied
     * value is substituted with a new value rotated by 47 places. See <a
     * href="http://en.wikipedia.org/wiki/ROT13">ROT13</a> for more information (there is
     * a subsection for ROT-47).
     * <p>
     * A Unix command to perform a ROT-47 cipher is:
     * 
     * <pre>
     * tr '!-~' 'P-~!-O'
     * </pre>
     * 
     * @param value The text to be rotated.
     * @return The rotated text.
     */
    public String rotate(String value) {
        if (value == null) {
            return null;
        }

        int length = value.length();
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < length; i++) {
            char c = value.charAt(i);

            // Process letters, numbers, and symbols -- ignore spaces.
            if (c != ' ') {
                // Add 47 (it is ROT-47, after all).
                c += 47;

                // If character is now above printable range, make it printable.
                // Range of printable characters is ! (33) to ~ (126). A value
                // of 127 (just above ~) would therefore get rotated down to a
                // 33 (the !). The value 94 comes from 127 - 33 = 94, which is
                // therefore the value that needs to be subtracted from the
                // non-printable character to put it into the correct printable
                // range.
                if (c > '~')
                    c -= 94;
            }

            result.append(c);
        }

        return result.toString();
    }

    /**
     * Small test program to run text through the ROT-47 cipher. This program can also be
     * run by hand to encode/decode values manually. The values passed on the command line
     * are printed to standard out.
     * 
     * @param args The array of text values (on the command-line) to be run through the
     *            ROT-47 cipher.
     */
    public static void main(String[] args) {
        Rot47PasswordEncoder encoder = new Rot47PasswordEncoder();

        for (String string : args) {
            System.out.println(encoder.rotate(string));
        }
    }
}

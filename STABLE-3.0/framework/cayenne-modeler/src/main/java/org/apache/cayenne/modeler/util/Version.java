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

package org.apache.cayenne.modeler.util;

import java.util.StringTokenizer;

/**
 * Helper class to deal with version strings.
 * 
 */
public class Version implements Comparable<Object> {

    protected String versionString;
    protected int[] versionParts;

    public Version(String versionString) throws NumberFormatException {
        if (versionString == null) {
            throw new IllegalArgumentException("Null version.");
        }

        if (versionString.trim().length() == 0) {
            throw new IllegalArgumentException("Empty version.");
        }

        this.versionString = versionString;

        StringTokenizer toks = new StringTokenizer(versionString, ".");
        versionParts = new int[toks.countTokens()];

        for (int i = 0; i < versionParts.length; i++) {
            versionParts[i] = Integer.parseInt(toks.nextToken());
        }
    }

    public int compareTo(Object o) {

        if (o instanceof CharSequence) {
            o = new Version(o.toString());
        }
        else if (!(o instanceof Version)) {
            throw new IllegalArgumentException(
                    "Can only compare to Versions and Strings, got: " + o);
        }

        int[] otherVersion = ((Version) o).versionParts;

        int len = Math.min(otherVersion.length, versionParts.length);
        for (int i = 0; i < len; i++) {

            int delta = versionParts[i] - otherVersion[i];
            if (delta != 0) {
                return delta;
            }
        }

        if (versionParts.length < otherVersion.length) {
            return -1;
        }
        else if (versionParts.length > otherVersion.length) {
            return 1;
        }
        else {
            return 0;
        }
    }

    public String getVersionString() {
        return versionString;
    }
}

/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/

package org.apache.cayenne.rop;

import java.util.Map;

public class ROPUtil {

    public static String getLogConnect(String url, String username, boolean password) {
        return getLogConnect(url, username, password, null);
    }

    public static String getLogConnect(String url, String username, boolean password, String sharedSessionName) {
        StringBuilder log = new StringBuilder("Connecting to [");
        if (username != null) {
            log.append(username);

            if (password) {
                log.append(":*******");
            }

            log.append("@");
        }

        log.append(url);
        log.append("]");

        if (sharedSessionName != null) {
            log.append(" - shared session '").append(sharedSessionName).append("'");
        } else {
            log.append(" - dedicated session.");
        }

        return log.toString();
    }

    public static String getLogDisconnect(String url, String username, boolean password) {
        StringBuilder log = new StringBuilder("Disconnecting from [");
        if (username != null) {
            log.append(username);

            if (password) {
                log.append(":*******");
            }

            log.append("@");
        }

        log.append(url);
        log.append("]");

        return log.toString();
    }

    public static String getParamsAsString(Map<String, String> params) {
        StringBuilder urlParams = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (urlParams.length() > 0) {
                urlParams.append('&');
            }

            urlParams.append(entry.getKey());
            urlParams.append('=');
            urlParams.append(entry.getValue());
        }

        return urlParams.toString();
    }

    public static String getBasicAuth(String username, String password) {
        if (username != null && password != null) {
            return "Basic " + base64(username + ":" + password);
        }

        return null;
    }

    /**
     * Creates the Base64 value.
     */
    public static String base64(String value) {
        StringBuffer cb = new StringBuffer();

        int i = 0;
        for (i = 0; i + 2 < value.length(); i += 3) {
            long chunk = (int) value.charAt(i);
            chunk = (chunk << 8) + (int) value.charAt(i + 1);
            chunk = (chunk << 8) + (int) value.charAt(i + 2);

            cb.append(encode(chunk >> 18));
            cb.append(encode(chunk >> 12));
            cb.append(encode(chunk >> 6));
            cb.append(encode(chunk));
        }

        if (i + 1 < value.length()) {
            long chunk = (int) value.charAt(i);
            chunk = (chunk << 8) + (int) value.charAt(i + 1);
            chunk <<= 8;

            cb.append(encode(chunk >> 18));
            cb.append(encode(chunk >> 12));
            cb.append(encode(chunk >> 6));
            cb.append('=');
        } else if (i < value.length()) {
            long chunk = (int) value.charAt(i);
            chunk <<= 16;

            cb.append(encode(chunk >> 18));
            cb.append(encode(chunk >> 12));
            cb.append('=');
            cb.append('=');
        }

        return cb.toString();
    }

    public static char encode(long d) {
        d &= 0x3f;
        if (d < 26) {
            return (char) (d + 'A');
        } else if (d < 52) {
            return (char) (d + 'a' - 26);
        } else if (d < 62) {
            return (char) (d + '0' - 52);
        } else if (d == 62) {
            return '+';
        } else {
            return '/';
        }
    }

}
/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.modeler.pref;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.prefs.Preferences;

/**
 * Encodes a file path into a single-segment {@link Preferences} node name. The id has
 * the form {@code <16-hex-sha256>-<sanitized-basename>} and stays under {@link #MAX_LEN}
 * characters, well below {@link Preferences#MAX_NAME_LENGTH}. The original absolute
 * path is meant to be stored alongside the node as a {@code path} value.
 */
public final class PreferenceNodeIds {

    static final int MAX_LEN = 60;
    static final int HASH_LEN = 16;

    private PreferenceNodeIds() {
    }

    /**
     * Returns a stable single-segment id for the given absolute path.
     */
    public static String idForPath(String absolutePath) {
        String canonical = canonicalize(absolutePath);
        String hash = sha256Hex(canonical).substring(0, HASH_LEN);
        String basename = basename(canonical);
        String sanitized = sanitize(basename);

        int budget = MAX_LEN - HASH_LEN - 1;
        if (sanitized.length() > budget) {
            sanitized = sanitized.substring(0, budget);
        }
        return sanitized.isEmpty() ? hash : hash + "-" + sanitized;
    }

    /**
     * Normalizes a raw path: replaces backslashes with forward slashes and resolves
     * to absolute form. Also strips a trailing {@code .xml} so that an unsaved project
     * and the same project after a Save As to a different name don't collide.
     */
    static String canonicalize(String path) {
        if (path == null) {
            return "";
        }
        String slashed = path.replace('\\', '/');
        File file = new File(slashed);
        String absolute = file.isAbsolute() ? slashed : file.getAbsoluteFile().getPath().replace('\\', '/');
        if (absolute.endsWith(".xml")) {
            absolute = absolute.substring(0, absolute.length() - 4);
        }
        return absolute;
    }

    static String basename(String canonicalPath) {
        int slash = canonicalPath.lastIndexOf('/');
        return slash < 0 ? canonicalPath : canonicalPath.substring(slash + 1);
    }

    static String sanitize(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '.' || c == '_' || c == '-') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

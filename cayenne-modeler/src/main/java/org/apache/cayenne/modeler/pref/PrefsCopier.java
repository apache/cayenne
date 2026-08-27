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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Recursive {@link Preferences} subtree copy and move helpers.
 */
public final class PrefsCopier {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrefsCopier.class);

    private PrefsCopier() {
    }

    /**
     * Copies all keys and child nodes from {@code src} to {@code dst}. {@code src}
     * is left untouched. Best-effort: errors at intermediate nodes are logged.
     */
    public static void copy(Preferences src, Preferences dst) {
        try {
            for (String key : src.keys()) {
                dst.put(key, src.get(key, ""));
            }
            for (String childName : src.childrenNames()) {
                copy(src.node(childName), dst.node(childName));
            }
        } catch (BackingStoreException e) {
            LOGGER.warn("Error copying preferences from '{}' to '{}'",
                    safePath(src), safePath(dst), e);
        }
    }

    /**
     * Copies {@code src} onto {@code dst} and then removes {@code src}. If src
     * doesn't exist or is identical to dst, this is a no-op.
     */
    public static void move(Preferences src, Preferences dst) {
        if (src == null || dst == null || samePath(src, dst)) {
            return;
        }
        copy(src, dst);
        try {
            src.removeNode();
        } catch (BackingStoreException | IllegalStateException e) {
            LOGGER.warn("Error removing source node after move '{}'", safePath(src), e);
        }
    }

    private static boolean samePath(Preferences a, Preferences b) {
        return safePath(a).equals(safePath(b));
    }

    private static String safePath(Preferences p) {
        try {
            return p.absolutePath();
        } catch (IllegalStateException e) {
            return "<removed>";
        }
    }
}

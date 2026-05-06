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

import java.util.Arrays;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

class PreferencesJsonExporter {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesJsonExporter.class);

    public static String exportAsJson(Preferences node) {
        StringBuilder sb = new StringBuilder();
        appendNode(sb, node, 0);
        return sb.toString();
    }

    private static void appendNode(StringBuilder sb, Preferences node, int indent) {
        String[] keys;
        String[] children;
        try {
            keys = node.keys();
            children = node.childrenNames();
        } catch (BackingStoreException e) {
            LOGGER.warn("Error reading preferences node '{}'", node.absolutePath(), e);
            sb.append("{}");
            return;
        }

        if (keys.length == 0 && children.length == 0) {
            sb.append("{}");
            return;
        }

        Arrays.sort(keys);
        Arrays.sort(children);

        sb.append("{\n");
        boolean first = true;
        for (String key : keys) {
            if (!first) {
                sb.append(",\n");
            }
            indent(sb, indent + 1);
            appendString(sb, key);
            sb.append(": ");
            appendString(sb, node.get(key, ""));
            first = false;
        }
        for (String childName : children) {
            if (!first) {
                sb.append(",\n");
            }
            indent(sb, indent + 1);
            appendString(sb, childName);
            sb.append(": ");
            appendNode(sb, node.node(childName), indent + 1);
            first = false;
        }
        sb.append('\n');
        indent(sb, indent);
        sb.append('}');
    }

    private static void appendString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    private static void indent(StringBuilder sb, int level) {
        sb.append("  ".repeat(Math.max(0, level)));
    }
}

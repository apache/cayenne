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
package org.apache.cayenne.access;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * QueryFormatter is utility class for formatting queries.
 */
public final class QueryFormatter {

    private QueryFormatter() {
        // no instances
    }

    private final static Map<String, String> KEY_WORDS = new HashMap<String, String>();

    static {
        KEY_WORDS.put(" select ", "SELECT");
        KEY_WORDS.put(" from ", "FROM");
        KEY_WORDS.put(" where ", "WHERE");
        KEY_WORDS.put(" order by ", "ORDER BY");
        KEY_WORDS.put(" group by ", "GROUP BY");
        KEY_WORDS.put(" update ", "UPDATE");
        KEY_WORDS.put(" exec ", "EXEC");
        KEY_WORDS.put(" set ", "SET");
        KEY_WORDS.put(" insert ", "INSERT");
        KEY_WORDS.put(" values ", "VALUES");
        KEY_WORDS.put(" delete ", "DELETE");
        KEY_WORDS.put(" declare ", "DECLARE");
        KEY_WORDS.put(" case ", "CASE");
    }

    public static String formatQuery(String sql) {
        Map<Integer, String> scanResult = scanQuery(sql);
        Iterator<Integer> iter = scanResult.keySet().iterator();
        int nextKeyIdx = (iter.hasNext()) ? iter.next() : -1;

        StringBuffer buffer = new StringBuffer();
        int apixCount = 0;
        int bufferPos = 0;
        for (int pos = 0; pos < sql.length(); pos++) {
            if (sql.charAt(pos) == '\'') {
                apixCount++;
                if (pos > 0 && sql.charAt(pos - 1) == '\'')
                    apixCount = apixCount - 2;
            }
            if (apixCount % 2 != 0) {
                continue;
            }
            if (pos == nextKeyIdx) {
                buffer.append(sql.substring(bufferPos, pos + 1));
                buffer.append("\n");
                String shiftedKeyWrd = scanResult.get(nextKeyIdx);
                nextKeyIdx = (iter.hasNext()) ? iter.next() : -1;
                buffer.append(shiftedKeyWrd);
                pos = pos + shiftedKeyWrd.length();
                bufferPos = pos + 1;
            }
            else if (sql.charAt(pos) == ','
                    || sql.charAt(pos) == ')'
                    || sql.charAt(pos) == '(') {
                buffer.append(sql.substring(bufferPos, pos + 1));
                buffer.append("\n\t");
                bufferPos = pos + 1;
            }
        }
        buffer.append(sql.substring(bufferPos));
        buffer.append("\n");
        String result = buffer.toString();
        while (result.contains("  ")) {
            result = result.replaceAll("  ", " ");
        }
        return result;
    }

    private static Map<Integer, String> scanQuery(String sql) {
        Map<Integer, String> result = new TreeMap<Integer, String>();
        String sql2Lower = sql.toLowerCase();
        for (String keyWrd : KEY_WORDS.keySet()) {
            int prevIdx = 0;
            while (true) {
                int idx = sql2Lower.indexOf(keyWrd, prevIdx);
                if (idx >= 0) {
                    result.put(idx, KEY_WORDS.get(keyWrd));
                    prevIdx = idx + 1;
                }
                else {
                    break;
                }
            }
        }
        return result;
    }
}

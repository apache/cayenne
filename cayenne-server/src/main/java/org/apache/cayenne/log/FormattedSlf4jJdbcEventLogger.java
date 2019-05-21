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
package org.apache.cayenne.log;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.di.Inject;

/**
 * A {@link Slf4jJdbcEventLogger} extension that provides pretty formatting of the logged SQL messages.
 * 
 * @since 3.1
 * @since 4.0 renamed from FormattedCommonsJdbcEventLogger to FormattedSlf4jJdbcEventLogger as part of migration to SLF4J
 */
public class FormattedSlf4jJdbcEventLogger extends Slf4jJdbcEventLogger {

    private final static Map<String, String> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put(" select ", "SELECT");
        KEYWORDS.put(" from ", "FROM");
        KEYWORDS.put(" where ", "WHERE");
        KEYWORDS.put(" order by ", "ORDER BY");
        KEYWORDS.put(" group by ", "GROUP BY");
        KEYWORDS.put(" update ", "UPDATE");
        KEYWORDS.put(" exec ", "EXEC");
        KEYWORDS.put(" set ", "SET");
        KEYWORDS.put(" insert ", "INSERT");
        KEYWORDS.put(" values ", "VALUES");
        KEYWORDS.put(" delete ", "DELETE");
        KEYWORDS.put(" declare ", "DECLARE");
        KEYWORDS.put(" case ", "CASE");
    }

    public FormattedSlf4jJdbcEventLogger(@Inject RuntimeProperties runtimeProperties) {
    	super(runtimeProperties);
    }
    
    private String formatQuery(String sql) {
        Map<Integer, String> scanResult = scanQuery(sql);
        Iterator<Integer> iter = scanResult.keySet().iterator();
        int nextKeyIdx = (iter.hasNext()) ? iter.next() : -1;

        StringBuilder buffer = new StringBuilder();
        int apixCount = 0;
        int bufferPos = 0;
        for (int pos = 0; pos < sql.length(); pos++) {
            if (sql.charAt(pos) == '\'') {
                apixCount++;
                if (pos > 0 && sql.charAt(pos - 1) == '\'') {
                    apixCount = apixCount - 2;
                }
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

    private Map<Integer, String> scanQuery(String sql) {
        Map<Integer, String> result = new TreeMap<>();
        String sql2Lower = sql.toLowerCase();
        for (String keyWrd : KEYWORDS.keySet()) {
            int prevIdx = 0;
            while (true) {
                int idx = sql2Lower.indexOf(keyWrd, prevIdx);
                if (idx >= 0) {
                    result.put(idx, KEYWORDS.get(keyWrd));
                    prevIdx = idx + 1;
                } else {
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public void logQuery(String sql, ParameterBinding[] bindings) {
        if (isLoggable()) {
            super.logQuery(formatQuery(sql), bindings);
        }
    }
}

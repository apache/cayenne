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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.cayenne.access.translator.DbAttributeBinding;
import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;

/**
 * @since 4.1
 */
public class CompactSlf4jJdbcEventLogger extends Slf4jJdbcEventLogger {

    private static final String UNION  = "UNION";
    private static final String SELECT = "SELECT";
    private static final String FROM   = "FROM";
    private static final char   SPACE  = ' ';

    private static final Pattern UNION_PATTERN = Pattern.compile(UNION, Pattern.CASE_INSENSITIVE);

    public CompactSlf4jJdbcEventLogger(@Inject RuntimeProperties runtimeProperties) {
        super(runtimeProperties);
    }

    @Override
    public void logQuery(String sql, ParameterBinding[] bindings) {
        if (!isLoggable()) {
            return;
        }

        String str;
        if (UNION_PATTERN.matcher(sql).find()) {
            str = processUnionSql(sql);
        } else {
            str = trimSqlSelectColumns(sql);
        }

        super.logQuery(str, bindings);
    }

    protected String processUnionSql(String sql) {
        String modified = UNION_PATTERN.matcher(sql)
                .replaceAll(UNION);
        String[] queries = modified.split(UNION);
        return Arrays.stream(queries)
                .map(this::trimSqlSelectColumns)
                .collect(Collectors.joining(SPACE + UNION));
    }

    protected String trimSqlSelectColumns(String sql) {
        String str = sql.toUpperCase();
        int selectIndex = str.indexOf(SELECT);
        if (selectIndex == -1) {
            return sql;
        }
        selectIndex += SELECT.length();
        int fromIndex = str.indexOf(FROM);
        String columns = sql.substring(selectIndex, fromIndex);
        String[] columnsArray = columns.split(",");
        if (columnsArray.length <= 3) {
            return sql;
        }

        columns = "(" + columnsArray.length + " columns)";
        return new StringBuilder(sql.substring(0, selectIndex))
                .append(SPACE)
                .append(columns)
                .append(SPACE)
                .append(sql, fromIndex, sql.length())
                .toString();
    }

    @Override
    protected void appendParameters(StringBuilder buffer, String label, ParameterBinding[] bindings) {
        int bindingLength = bindings.length;
        if (bindingLength == 0) {
            return;
        }

        buildBinding(buffer, label, collectBindings(bindings));
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> collectBindings(ParameterBinding[] bindings) {
        Map<String, List<String>> bindingsMap = new HashMap<>();

        String key = null;
        String value;
        for (ParameterBinding b : bindings) {
            if (b.isExcluded()) {
                continue;
            }

            if (b instanceof DbAttributeBinding) {
                DbAttribute attribute = ((DbAttributeBinding) b).getAttribute();
                if (attribute != null) {
                    key = attribute.getName();
                }
            }

            if (b.getExtendedType() != null) {
                value = b.getExtendedType().toString(b.getValue());
            } else if (b.getValue() == null) {
                value = "NULL";
            } else {
                value = b.getValue().getClass().getName() +
                        "@" +
                        System.identityHashCode(b.getValue());
            }

            List<String> objects = bindingsMap.computeIfAbsent(key, k -> new ArrayList<>());
            objects.add(value);
        }

        return bindingsMap;
    }

    private void buildBinding(StringBuilder buffer, String label, Map<String, List<String>> bindingsMap) {
        int j = 1;
        boolean hasIncluded = false;
        for (String k : bindingsMap.keySet()) {
            if (!hasIncluded) {
                hasIncluded = true;
                buffer.append("[").append(label).append(": ");
            } else {
                buffer.append(", ");
            }
            buffer.append(j).append("->").append(k).append(": ");

            List<String> bindingsList = bindingsMap.get(k);
            if (bindingsList.size() == 1 ) {
                buffer.append(bindingsList.get(0));
            } else {
                buffer.append("{");
                boolean wasAdded = false;
                for (Object val : bindingsList) {
                    if (wasAdded) {
                        buffer.append(", ");
                    } else {
                        wasAdded = true;
                    }
                    buffer.append(val);
                }
                buffer.append("}");
            }
            j++;
        }

        if (hasIncluded) {
            buffer.append("]");
        }
    }

    @Override
    public void logBeginTransaction(String transactionLabel) {
    }

    @Override
    public void logCommitTransaction(String transactionLabel) {
    }
}

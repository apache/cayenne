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
package org.apache.cayenne.log;

import org.apache.cayenne.access.translator.DbAttributeBinding;
import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @since 4.1
 */
public class CompactSl4jJdbcEventLogger extends Slf4jJdbcEventLogger {

    private static final String UNION = "UNION";
    private static final String SELECT = "SELECT";
    private static final String FROM = "FROM";
    private static final String SPACE = " ";

    public CompactSl4jJdbcEventLogger(@Inject RuntimeProperties runtimeProperties) {
        super(runtimeProperties);
    }

    @Override
    public void logQuery(String sql, ParameterBinding[] bindings) {
        if (!isLoggable()) {
            return;
        }

        String str;
        if (sql.toUpperCase().contains(UNION)) {
            str = processUnionSql(sql);
        } else {
            str = formatSqlSelectColumns(sql);
        }

        StringBuilder stringBuilder = new StringBuilder(str);
        appendParameters(stringBuilder, "bind", bindings);
        if (stringBuilder.length() < 0) {
            return;
        }

        super.logQuery(stringBuilder.toString(), new ParameterBinding[0]);
    }

    private String processUnionSql(String sql) {

        String modified = Pattern.compile(UNION.toLowerCase(), Pattern.CASE_INSENSITIVE).matcher(sql).replaceAll(UNION);
        String[] queries = modified.split(
                UNION);
        List<String> formattedQueries = Arrays.stream(queries).map(this::formatSqlSelectColumns).collect(Collectors.toList());
        StringBuilder buffer = new StringBuilder();
        boolean used =  false;
        for (String q: formattedQueries) {
            if(!used){
                used = true;
            } else {
                buffer.append(SPACE).append(UNION);
            }
            buffer.append(q);
        }
        return buffer.toString();
    }

    private String formatSqlSelectColumns(String sql) {
        int selectIndex = sql.toUpperCase().indexOf(SELECT);
        if (selectIndex == -1) {
            return sql;
        }
        selectIndex += SELECT.length();
        int fromIndex = sql.toUpperCase().indexOf(FROM);
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

    private void appendParameters(StringBuilder buffer, String label, ParameterBinding[] bindings) {
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
        for (int i = 0; i < bindings.length; i++) {
            ParameterBinding b = bindings[i];

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
            } else if(b.getValue() == null) {
                value = "NULL";
            } else {
                value = new StringBuilder(b.getValue().getClass().getName())
                        .append("@")
                        .append(System.identityHashCode(b.getValue())).toString();
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
                buffer.append(" [").append(label).append(": ");
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

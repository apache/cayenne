/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cayenne.query.sqldsl;

import de.jexp.jequel.expression.ParamExpression;
import de.jexp.jequel.processor.ParameterCollectorProcessor;
import de.jexp.jequel.sql.Sql;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SqlDslDefaultExecutableParams implements SqlDslExecutableParams {
    private final Map<String, Object> namedParams = new LinkedHashMap<String, Object>();
    private final List<Object> params = new LinkedList<Object>();

    public SqlDslDefaultExecutableParams(SqlDslExecutableParams params) {
        this.namedParams.putAll(params.getNamedParams());
        this.params.addAll(params.getParamValues());
    }

    public SqlDslDefaultExecutableParams(Map<String, Object> namedParams) {
        this.namedParams.putAll(namedParams);
    }

    public SqlDslDefaultExecutableParams(Collection<?> params) {
        this.params.addAll(params);
    }

    protected void addParams(Collection<?> params) {
        int i = 0;
        for (Object param : params) {
            if (i >= this.params.size()) {
                break; // this.params.add(param);
            }

            this.params.set(i, param);
            i++;
        }
    }

    public SqlDslDefaultExecutableParams() {
    }

    public void addParam(Object value) {
        this.params.add(value);
    }

    public void addParam(String name, Object value) {
        this.namedParams.put(name, value);
    }

    public void addParams(SqlDslExecutableParams executableParams) {
        for (Map.Entry<String, Object> namedParam : executableParams.getNamedParams().entrySet()) {
            addParam(namedParam.getKey(), namedParam.getValue());
        }
        addParams(executableParams.getParamValues());
    }

    public boolean hasParams() {
        return getParamCount() > 0;
    }

    public boolean hasOnlyNamed() {
        return params.isEmpty();
    }

    public int getParamCount() {
        return namedParams.size() + params.size();
    }

    public List<Object> getParamValues() {
        return params;
    }

    public Collection<String> getParamNames() {
        return namedParams.keySet();
    }

    public Map<String, Object> getNamedParams() {
        return namedParams;
    }

    public static SqlDslDefaultExecutableParams extractParams(Sql sql) {
        ParameterCollectorProcessor paramsCollector = new ParameterCollectorProcessor();
        paramsCollector.process(sql);

        SqlDslDefaultExecutableParams executableParams = new SqlDslDefaultExecutableParams();
        for (ParamExpression namedExpression : paramsCollector.getNamedExpressions()) {
            executableParams.addParam(namedExpression.getLiteral(), namedExpression.getValue());
        }
        for (ParamExpression paramExpression : paramsCollector.getParamExpressions()) {
            executableParams.addParam(paramExpression.getValue());
        }
        return executableParams;
    }

    public static SqlDslExecutableParams createParams(Object[] params) {
        return new SqlDslDefaultExecutableParams(Arrays.asList(params));
    }

    public static SqlDslExecutableParams createNamedParams(Object[] params) {
        if (params.length % 2 != 0) {
            return createParams(params);
        }
        SqlDslDefaultExecutableParams namedParams = new SqlDslDefaultExecutableParams();
        for (int i = 0; i < params.length; i += 2) {
            if (params[i] instanceof String) {
                namedParams.addParam((String) params[i], params[i + 1]);
            } else {
                return createParams(params);
            }
        }
        return namedParams;
    }

    public static SqlDslExecutableParams createParams(Collection<?> params) {
        return new SqlDslDefaultExecutableParams(params);
    }

    public static SqlDslExecutableParams createParams(Map<String, Object> params) {
        return new SqlDslDefaultExecutableParams(params);
    }
}

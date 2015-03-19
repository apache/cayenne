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

import de.jexp.jequel.sql.Sql;
import org.apache.cayenne.access.QueryEngine;
import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.jdbc.ParameterBinding;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.dba.JdbcActionBuilder;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.BaseQueryMetadata;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.QueryRouter;
import org.apache.cayenne.query.SQLActionVisitor;

import java.util.List;
import java.util.Map;

/**
 * @param <R> query return type
 *
 * @since 4.0
 */
public class SqlQuery<R> implements Query {

    private final Sql sql;
    private final String name;

    private final QueryMetadata metadata = new BaseQueryMetadata();

    /* @Lazy */
    private SqlDslExecutableParams params;

    public SqlQuery(Sql sql, String name) {
        this.sql = sql;
        this.name = name;
    }

    public SqlQuery(Sql sql) {
        this(sql, null);
    }

    public Sql getSql() {
        return sql;
    }

    @Override
    public QueryMetadata getMetaData(EntityResolver resolver) {
        return metadata;
    }

    @Override
    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
        // TODO copy + past from SqlTemplate
        DataMap map = getMetaData(resolver).getDataMap();

        QueryEngine engine;
        if (map != null) {
            engine = router.engineForDataMap(map);
        } else {
            engine = router.engineForName(null /*getDataNodeName()*/);
        }

        router.route(engine, this, substitutedQuery);
    }

    @Override
    public SqlDslAction<R> createSQLAction(SQLActionVisitor visitor) {
        return new SqlDslAction<R>(this, ((JdbcActionBuilder) visitor).getDataNode());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public DataMap getDataMap() {
        return null;
    }

    @Override
    public <T> T acceptVisitor(ConfigurationNodeVisitor<T> visitor) {
        return null;
    }

    public int getParamsCount() {
        return getParams().getParamCount();
    }

    public Map<String, Object> getNamedParams() {
        return getParams().getNamedParams();
    }

    public List<Object> getPositionalParams() {
        return getParams().getParamValues();
    }

    private SqlDslExecutableParams getParams() {
        if (this.params == null) {
            this.params = SqlDslDefaultExecutableParams.extractParams(sql);
        }

        return this.params;
    }

    public ColumnDescriptor[] getResultColumns() {
        ColumnDescriptor[] descriptors = new ColumnDescriptor[]{};



        return descriptors;
    }

    public ParameterBinding[] getParametersBindings() {
        ParameterBinding[] bindings = new ParameterBinding[getParamsCount()];
        List<Object> params = getPositionalParams();
        for (int i = 0; i < bindings.length; i++) {
            Object value = params.get(i);
            bindings[i] = new ParameterBinding(value, TypesMapping.getSqlTypeByJava(value.getClass()), -1);
        }
        return bindings;
    }
}

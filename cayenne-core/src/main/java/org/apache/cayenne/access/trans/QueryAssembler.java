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

package org.apache.cayenne.access.trans;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.log.NoopJdbcEventLogger;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.JoinType;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;

/**
 * Abstract superclass of Query translators.
 */
public abstract class QueryAssembler {

    protected Query query;
    protected QueryMetadata queryMetadata;
    protected String cachedSqlString;
    protected Connection connection;
    protected DbAdapter adapter;
    protected EntityResolver entityResolver;
    protected JdbcEventLogger logger;

    /**
     * Holds PreparedStatement values.
     */
    protected List<Object> values = new ArrayList<Object>();

    /**
     * PreparedStatement attributes matching entries in <code>values</code> list.
     */
    protected List<DbAttribute> attributes = new ArrayList<DbAttribute>();

    /**
     * The index parameter will be inserted at in parameter list
     */
    protected int parameterIndex;
    
    public QueryAssembler() {
        this.logger = NoopJdbcEventLogger.getInstance();
    }

    /**
     * Returns aliases for the path splits defined in the query.
     * 
     * @since 3.0
     */
    protected Map<String, String> getPathAliases() {
        return query.getMetaData(entityResolver).getPathSplitAliases();
    }

    public EntityResolver getEntityResolver() {
        return entityResolver;
    }

    public DbAdapter getAdapter() {
        return adapter;
    }

    /**
     * Returns query object being processed.
     */
    public Query getQuery() {
        return query;
    }

    public QueryMetadata getQueryMetadata() {
        return queryMetadata;
    }

    public void setQuery(Query query) {
        this.query = query;
        refreshMetadata();
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public void setAdapter(DbAdapter adapter) {
        this.adapter = adapter;
    }
    
    /**
     * @since 3.1
     */
    public void setJdbcEventLogger(JdbcEventLogger logger) {
        this.logger = logger;
    }
    
    /**
     * @since 3.1
     */
    public JdbcEventLogger getJdbcEventLogger() {
        return logger;
    }

    public void setEntityResolver(EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
        refreshMetadata();
    }

    private void refreshMetadata() {
        if (query != null && entityResolver != null) {
            queryMetadata = query.getMetaData(entityResolver);
        }
        else {
            queryMetadata = null;
        }
        cachedSqlString = null;
    }

    public DbEntity getRootDbEntity() {
        return queryMetadata.getDbEntity();
    }

    public ObjEntity getRootEntity() {
        return queryMetadata.getObjEntity();
    }

    /**
     * A callback invoked by a child qualifier or ordering processor allowing query
     * assembler to reset its join stack.
     * 
     * @since 3.0
     */
    public abstract void resetJoinStack();

    /**
     * Returns an alias of the table which is currently at the top of the join stack.
     * 
     * @since 3.0
     */
    public abstract String getCurrentAlias();

    /**
     * Appends a join with given semantics to the query.
     * 
     * @since 3.0
     */
    public abstract void dbRelationshipAdded(
            DbRelationship relationship,
            JoinType joinType,
            String joinSplitAlias);

    /**
     * Translates query into sql string. This is a workhorse method of QueryAssembler. It
     * is called internally from <code>createStatement</code>. Usually there is no need to
     * invoke it explicitly.
     */
    public abstract String createSqlString() throws Exception;

    /**
     * Returns <code>true</code> if table aliases are supported. Default implementation
     * returns false.
     */
    public boolean supportsTableAliases() {
        return false;
    }

    /**
     * Registers <code>anObject</code> as a PreparedStatement parameter.
     * 
     * @param anObject object that represents a value of DbAttribute
     * @param dbAttr DbAttribute being processed.
     */
    public void addToParamList(DbAttribute dbAttr, Object anObject) {
        attributes.add(parameterIndex, dbAttr);
        values.add(parameterIndex++, anObject);
    }

    /**
     * Translates internal query into PreparedStatement.
     */
    public PreparedStatement createStatement() throws Exception {
        long t1 = System.currentTimeMillis();
        String sqlStr = createSqlString();
        logger.logQuery(sqlStr, attributes, values, System.currentTimeMillis() - t1);
        PreparedStatement stmt = connection.prepareStatement(sqlStr);
        initStatement(stmt);
        return stmt;
    }

    /**
     * Initializes prepared statements with collected parameters. Called internally from
     * "createStatement". Cayenne users shouldn't normally call it directly.
     */
    protected void initStatement(PreparedStatement stmt) throws Exception {
        if (values != null && values.size() > 0) {
            int len = values.size();
            for (int i = 0; i < len; i++) {
                Object val = values.get(i);

                DbAttribute attr = attributes.get(i);

                // null DbAttributes are a result of inferior qualifier processing
                // (qualifier can't map parameters to DbAttributes and therefore
                // only supports standard java types now)
                // hence, a special moronic case here:
                if (attr == null) {
                    stmt.setObject(i + 1, val);
                }
                else {
                    adapter.bindParameter(stmt, val, i + 1, attr.getType(), attr
                            .getScale());
                }
            }
        }

        if (queryMetadata.getStatementFetchSize() != 0) {
            stmt.setFetchSize(queryMetadata.getStatementFetchSize());
        }
    }
}

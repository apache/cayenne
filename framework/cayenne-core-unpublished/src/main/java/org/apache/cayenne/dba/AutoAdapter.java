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

package org.apache.cayenne.dba;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.trans.QualifierTranslator;
import org.apache.cayenne.access.trans.QueryAssembler;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.merge.MergerFactory;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;

/**
 * A DbAdapter that automatically detects the kind of database it is running on and
 * instantiates an appropriate DB-specific adapter, delegating all subsequent method calls
 * to this adapter.
 * 
 * @since 1.2
 */
public class AutoAdapter implements DbAdapter {

    protected Provider<DbAdapter> adapterProvider;
    protected PkGenerator pkGenerator;
    protected JdbcEventLogger logger;

    /**
     * The actual adapter that is delegated methods execution.
     */
    volatile DbAdapter adapter;

    /**
     * Creates an {@link AutoAdapter} based on a delegate adapter obtained via
     * "adapterProvider".
     * 
     * @since 3.1
     */
    public AutoAdapter(Provider<DbAdapter> adapterProvider, JdbcEventLogger logger) {

        if (adapterProvider == null) {
            throw new CayenneRuntimeException("Null adapterProvider");
        }

        this.adapterProvider = adapterProvider;
        this.logger = logger;
    }

    /**
     * Returns a proxied DbAdapter, lazily creating it on first invocation.
     */
    protected DbAdapter getAdapter() {
        if (adapter == null) {
            synchronized (this) {
                if (adapter == null) {
                    this.adapter = loadAdapter();
                }
            }
        }

        return adapter;
    }

    /**
     * Loads underlying DbAdapter delegate.
     */
    protected DbAdapter loadAdapter() {
        return adapterProvider.get();
    }

    public String getBatchTerminator() {
        return getAdapter().getBatchTerminator();
    }

    public QualifierTranslator getQualifierTranslator(QueryAssembler queryAssembler) {
        return getAdapter().getQualifierTranslator(queryAssembler);
    }

    public SQLAction getAction(Query query, DataNode node) {
        return getAdapter().getAction(query, node);
    }

    public boolean supportsUniqueConstraints() {
        return getAdapter().supportsUniqueConstraints();
    }

    public boolean supportsGeneratedKeys() {
        return getAdapter().supportsGeneratedKeys();
    }

    public boolean supportsBatchUpdates() {
        return getAdapter().supportsBatchUpdates();
    }

    public Collection<String> dropTableStatements(DbEntity table) {
        return getAdapter().dropTableStatements(table);
    }

    public String createTable(DbEntity entity) {
        return getAdapter().createTable(entity);
    }

    public String createUniqueConstraint(DbEntity source, Collection<DbAttribute> columns) {
        return getAdapter().createUniqueConstraint(source, columns);
    }

    public String createFkConstraint(DbRelationship rel) {
        return getAdapter().createFkConstraint(rel);
    }

    public String[] externalTypesForJdbcType(int type) {
        return getAdapter().externalTypesForJdbcType(type);
    }

    public ExtendedTypeMap getExtendedTypes() {
        return getAdapter().getExtendedTypes();
    }

    /**
     * Returns a primary key generator.
     */
    public PkGenerator getPkGenerator() {
        return (pkGenerator != null) ? pkGenerator : getAdapter().getPkGenerator();
    }

    /**
     * Sets a PK generator override. If set to non-null value, such PK generator will be
     * used instead of the one provided by wrapped adapter.
     */
    public void setPkGenerator(PkGenerator pkGenerator) {
        this.pkGenerator = pkGenerator;
    }

    public DbAttribute buildAttribute(
            String name,
            String typeName,
            int type,
            int size,
            int precision,
            boolean allowNulls) {

        return getAdapter().buildAttribute(
                name,
                typeName,
                type,
                size,
                precision,
                allowNulls);
    }

    public void bindParameter(
            PreparedStatement statement,
            Object object,
            int pos,
            int sqlType,
            int precision) throws SQLException, Exception {
        getAdapter().bindParameter(statement, object, pos, sqlType, precision);
    }

    public String tableTypeForTable() {
        return getAdapter().tableTypeForTable();
    }

    public String tableTypeForView() {
        return getAdapter().tableTypeForView();
    }

    public MergerFactory mergerFactory() {
        return getAdapter().mergerFactory();
    }

    public void createTableAppendColumn(StringBuffer sqlBuffer, DbAttribute column) {
        getAdapter().createTableAppendColumn(sqlBuffer, column);
    }

    /**
     * @deprecated since 3.2
     */
    @Deprecated
    public QuotingStrategy getQuotingStrategy(boolean isQuoteStrategy) {
        return getAdapter().getQuotingStrategy(isQuoteStrategy);
    }
    
    /**
     * @since 3.2
     */
    public QuotingStrategy getQuotingStrategy() {
        return getAdapter().getQuotingStrategy();
    }
}

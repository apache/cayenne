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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.trans.QualifierTranslator;
import org.apache.cayenne.access.trans.QueryAssembler;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.merge.MergerFactory;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;

/**
 */
public class MockDbAdapter implements DbAdapter {

    public MockDbAdapter() {
        super();
    }

    public String getBatchTerminator() {
        return null;
    }

    public QualifierTranslator getQualifierTranslator(QueryAssembler queryAssembler) {
        return null;
    }

    public SQLAction getAction(Query query, DataNode node) {
        return null;
    }

    public boolean supportsFkConstraints() {
        return false;
    }

    public boolean supportsUniqueConstraints() {
        return false;
    }

    public boolean supportsGeneratedKeys() {
        return false;
    }

    public boolean supportsBatchUpdates() {
        return false;
    }

    public String dropTable(DbEntity ent) {
        return null;
    }

    public Collection<String> dropTableStatements(DbEntity table) {
        return null;
    }

    public String createTable(DbEntity ent) {
        return null;
    }

    public String createUniqueConstraint(DbEntity source, Collection columns) {
        return null;
    }

    public String createFkConstraint(DbRelationship rel) {
        return null;
    }

    public String[] externalTypesForJdbcType(int type) {
        return null;
    }

    public ExtendedTypeMap getExtendedTypes() {
        return null;
    }

    public PkGenerator getPkGenerator() {
        return null;
    }

    public DbAttribute buildAttribute(
            String name,
            String typeName,
            int type,
            int size,
            int precision,
            boolean allowNulls) {
        return null;
    }

    public void bindParameter(
            PreparedStatement statement,
            Object object,
            int pos,
            int sqlType,
            int precision) throws SQLException, Exception {
    }

    public String tableTypeForTable() {
        return null;
    }

    public String tableTypeForView() {
        return null;
    }

    public boolean shouldRunBatchQuery(
            DataNode node,
            Connection con,
            BatchQuery query,
            OperationObserver delegate) throws SQLException, Exception {
        return false;
    }

    public MergerFactory mergerFactory() {
        return null;
    }

    public void createTableAppendColumn(StringBuffer sqlBuffer, DbAttribute column) {
    }
    
    public String getIdentifiersStartQuote(){
        return "\"";
    }
    public String getIdentifiersEndQuote(){
        return "\"";
    }
    
    public QuotingStrategy getQuotingStrategy(boolean isQuoteStrategy) {
        return null;
    }
}

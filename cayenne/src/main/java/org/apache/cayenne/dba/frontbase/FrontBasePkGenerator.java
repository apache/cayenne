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

package org.apache.cayenne.dba.frontbase;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.QueryResult;
import org.apache.cayenne.SelectResult;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.StatementContext;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.JdbcPkGenerator;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.SQLTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 1.2
 */
public class FrontBasePkGenerator extends JdbcPkGenerator {

    public FrontBasePkGenerator(JdbcAdapter adapter) {
        super(adapter);
        pkStartValue = 1000000;
    }

    /**
     * Returns zero as PK caching is not supported by FrontBaseAdapter.
     */
    @Override
    public int getPkCacheSize() {
        return 0;
    }

    @Override
    public void createAutoPk(DataNode node, List<DbEntity> dbEntities) {
        // For each entity (re)set the unique counter
        for (DbEntity entity : dbEntities) {
            runUpdate(node, pkCreateString(entity.getName()));
        }
    }

    @Override
    public List<String> createAutoPkStatements(List<DbEntity> dbEntities) {
        List<String> list = new ArrayList<>();
        for (DbEntity entity : dbEntities) {
            list.add(pkCreateString(entity.getName()));
        }
        return list;
    }

    @Override
    public void dropAutoPk(DataNode node, List<DbEntity> dbEntities) {
    }

    @Override
    protected String pkTableCreateString() {
        return "";
    }

    @Override
    protected String pkDeleteString(List<DbEntity> dbEntities) {
        return "-- The 'Drop Primary Key Support' option is unavailable";
    }

    @Override
    protected String pkCreateString(String entName) {
        return "SET UNIQUE = " + pkStartValue + " FOR \"" + entName + "\"";
    }

    @Override
    protected String pkSelectString(String entName) {
        return "SELECT UNIQUE FROM \"" + entName + "\"";
    }

    @Override
    protected String pkUpdateString(String entName) {
        return "";
    }

    @Override
    protected String dropAutoPkString() {
        return "";
    }

    /**
     * @since 3.0
     */
    @Override
    protected long longPkFromDatabase(DataNode node, DbEntity entity) {

        String template = "SELECT #result('UNIQUE' 'long') FROM " + entity.getName();

        SQLTemplate<DataRow> query = SQLTemplate.dataRowQuery(template);
        StatementContext statementContext = new StatementContext(query, query.getMetaData(node.getEntityResolver()));
        node.execute(List.of(statementContext));

        long pk = 0;
        for (QueryResult result : statementContext.getResults()) {
            if (result instanceof SelectResult<?>(List<?> rows)) {
                if (rows.size() != 1) {
                    throw new CayenneRuntimeException("Error fetching PK. Expected one row, got %d", rows.size());
                }

                DataRow row = (DataRow) rows.getFirst();
                pk = ((Number) row.get("UNIQUE")).longValue();
            }
        }
        return pk;
    }
}

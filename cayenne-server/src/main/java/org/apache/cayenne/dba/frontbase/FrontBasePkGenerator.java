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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.util.DoNothingOperationObserver;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.JdbcPkGenerator;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLTemplate;

/**
 * @since 1.2
 */
public class FrontBasePkGenerator extends JdbcPkGenerator {

    /**
     * Used by DI
     * @since 4.1
     */
    public FrontBasePkGenerator() {
        super();
    }

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
    public void createAutoPk(DataNode node, List<DbEntity> dbEntities) throws Exception {
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
    public void dropAutoPk(DataNode node, List<DbEntity> dbEntities) throws Exception {
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
        StringBuilder buf = new StringBuilder();
        buf.append("SET UNIQUE = ").append(pkStartValue).append(" FOR \"").append(entName).append("\"");
        return buf.toString();
    }

    @Override
    protected String pkSelectString(String entName) {
        StringBuilder buf = new StringBuilder();
        buf.append("SELECT UNIQUE FROM \"").append(entName).append("\"");
        return buf.toString();
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
    protected long longPkFromDatabase(DataNode node, DbEntity entity) throws Exception {

        String template = "SELECT #result('UNIQUE' 'long') FROM " + entity.getName();

        final long[] pkHolder = new long[1];

        SQLTemplate query = new SQLTemplate(entity, template);
        OperationObserver observer = new DoNothingOperationObserver() {

            @Override
            public void nextRows(Query query, List<?> dataRows) {
                if (dataRows.size() != 1) {
                    throw new CayenneRuntimeException("Error fetching PK. Expected one row, got %d", dataRows.size());
                }

                DataRow row = (DataRow) dataRows.get(0);
                Number pk = (Number) row.get("UNIQUE");
                pkHolder[0] = pk.longValue();
            }
        };

        node.performQueries(Collections.singleton((Query) query), observer);
        return pkHolder[0];
    }
}

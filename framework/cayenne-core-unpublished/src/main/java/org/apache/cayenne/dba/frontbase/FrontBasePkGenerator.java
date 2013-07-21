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

package org.apache.cayenne.dba.frontbase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.QueryResult;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.JdbcPkGenerator;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLTemplate;

/**
 * @since 1.2
 */
public class FrontBasePkGenerator extends JdbcPkGenerator {

    public FrontBasePkGenerator(JdbcAdapter adapter) {
        super(adapter);
    }

    /**
     * Retruns zero as PK caching is not supported by FrontBaseAdapter.
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
        List<String> list = new ArrayList<String>();
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
        buf.append("SET UNIQUE = 1000000 FOR \"").append(entName).append("\"");
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

        SQLTemplate query = new SQLTemplate(entity, template);
        QueryResult observer = new QueryResult();
        node.performQueries(Collections.singleton((Query) query), observer);

        List results = observer.getFirstRows(query);
        if (results.size() != 1) {
            throw new CayenneRuntimeException("Error fetching PK. Expected one row, got "
                    + results.size());
        }

        Map row = (Map) results.get(0);
        Number pk = (Number) row.get("UNIQUE");
        return pk.longValue();
    }
}

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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.QueryResult;
import org.apache.cayenne.dba.JdbcPkGenerator;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.SQLTemplate;

/**
 * @since 1.2
 * @author Andrus Adamchik
 */
public class FrontBasePkGenerator extends JdbcPkGenerator {

    public FrontBasePkGenerator() {
        super();
    }

    /**
     * Retruns zero as PK caching is not supported by FrontBaseAdapter.
     */
    public int getPkCacheSize() {
        return 0;
    }

    public void createAutoPk(DataNode node, List dbEntities) throws Exception {
        // For each entity (re)set the unique counter
        Iterator it = dbEntities.iterator();
        while (it.hasNext()) {
            DbEntity ent = (DbEntity) it.next();
            runUpdate(node, pkCreateString(ent.getName()));
        }
    }

    public List createAutoPkStatements(List dbEntities) {
        List list = new ArrayList();

        Iterator it = dbEntities.iterator();
        while (it.hasNext()) {
            DbEntity ent = (DbEntity) it.next();
            list.add(pkCreateString(ent.getName()));
        }

        return list;
    }

    public void dropAutoPk(DataNode node, List dbEntities) throws Exception {
    }

    protected String pkTableCreateString() {
        return "";
    }

    protected String pkDeleteString(List dbEntities) {
        return "-- The 'Drop Primary Key Support' option is unavailable";
    }

    protected String pkCreateString(String entName) {
        StringBuffer buf = new StringBuffer();
        buf.append("SET UNIQUE = 1000000 FOR \"" + entName + "\"");
        return buf.toString();
    }

    protected String pkSelectString(String entName) {
        StringBuffer buf = new StringBuffer();
        buf.append("SELECT UNIQUE FROM \"" + entName + "\"");
        return buf.toString();
    }

    protected String pkUpdateString(String entName) {
        return "";
    }

    protected String dropAutoPkString() {
        return "";
    }

    protected int pkFromDatabase(DataNode node, DbEntity entity) throws Exception {
        String template = "SELECT #result('UNIQUE' 'int') FROM " + entity.getName();

        SQLTemplate query = new SQLTemplate(entity, template);
        QueryResult observer = new QueryResult();
        node.performQueries(Collections.singleton(query), observer);

        List results = observer.getFirstRows(query);
        if (results.size() != 1) {
            throw new CayenneRuntimeException("Error fetching PK. Expected one row, got "
                    + results.size());
        }

        Map row = (Map) results.get(0);
        Number pk = (Number) row.get("UNIQUE");
        return pk.intValue();
    }
}

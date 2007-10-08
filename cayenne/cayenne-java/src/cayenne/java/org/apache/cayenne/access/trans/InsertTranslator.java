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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;

/**
 * Class implements default translation mechanism of
 * org.apache.cayenne.query.InsertQuery objects to SQL INSERT statements. Note that
 * in order for this query to execute successfully, ObjectId contained within InsertQuery
 * must be fully initialized.
 * 
 * @author Andrei Adamchik
 * @deprecated since 1.2 Object InsertQuery is not needed anymore. It shouldn't be used
 *             directly anyway, but in cases where one might want to have access to it,
 *             InsertBatchQuery is a reasonable substitute.
 */
public class InsertTranslator extends QueryAssembler {

    protected List columnList = new ArrayList();

    public String aliasForTable(DbEntity dbEnt) {
        throw new RuntimeException("aliases not supported");
    }

    public void dbRelationshipAdded(DbRelationship dbRel) {
        throw new RuntimeException("db relationships not supported");
    }

    /** Method that converts an insert query into SQL string */
    public String createSqlString() throws Exception {
        prepareLists();
        StringBuffer queryBuf = new StringBuffer("INSERT INTO ");
        DbEntity dbE = getRootDbEntity();
        queryBuf.append(dbE.getFullyQualifiedName()).append(" (");

        int len = columnList.size();

        // 1. Append column names

        // unroll the loop to avoid condition checking in the loop
        queryBuf.append(columnList.get(0)); // assume we have at least 1 column
        for (int i = 1; i < len; i++) {
            queryBuf.append(", ").append(columnList.get(i));
        }

        // 2. Append values ('?' in place of actual parameters)
        queryBuf.append(") VALUES (");
        if (len > 0) {
            queryBuf.append('?');
            for (int i = 1; i < len; i++) {
                queryBuf.append(", ?");
            }
        }

        queryBuf.append(')');
        return queryBuf.toString();
    }

    public org.apache.cayenne.query.InsertQuery insertQuery() {
        return (org.apache.cayenne.query.InsertQuery) query;
    }

    /** Creates 2 matching lists: columns names and values */
    protected void prepareLists() throws Exception {
        DbEntity dbE = getRootDbEntity();
        ObjectId oid = insertQuery().getObjectId();
        Map id = (oid != null) ? oid.getIdSnapshot() : null;

        if (id != null) {
            Iterator idIt = id.entrySet().iterator();
            while (idIt.hasNext()) {
            	Map.Entry entry = (Map.Entry) idIt.next();
                String attrName = (String) entry.getKey();
                DbAttribute attr = (DbAttribute) dbE.getAttribute(attrName);
                Object attrValue = entry.getValue();
                columnList.add(attrName);

                addToParamList(attr, attrValue);
            }
        }

        Map snapshot = insertQuery().getObjectSnapshot();
        Iterator columnsIt = snapshot.entrySet().iterator();
        while (columnsIt.hasNext()) {
        	Map.Entry entry = (Map.Entry) columnsIt.next();
            String attrName = (String) entry.getKey();

            // values taken from ObjectId take precedence.
            if (id != null && id.get(attrName) != null)
                continue;

            DbAttribute attr = (DbAttribute) dbE.getAttribute(attrName);
            Object attrValue = entry.getValue();
            columnList.add(attrName);
            addToParamList(attr, attrValue);
        }
    }
}

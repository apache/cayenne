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

import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.JoinType;
import org.apache.cayenne.query.UpdateQuery;

/**
 * Class implements default translation mechanism of org.apache.cayenne.query.UpdateQuery
 * objects to SQL UPDATE statements.
 * 
 * @deprecated since 3.0 use EJBQL or SQLTemplate
 */
public class UpdateTranslator extends QueryAssembler {

    @Override
    public void dbRelationshipAdded(
            DbRelationship relationship,
            JoinType joinType,
            String joinSplitAlias) {
        throw new UnsupportedOperationException("db relationships not supported");
    }

    @Override
    public String getCurrentAlias() {
        throw new UnsupportedOperationException("aliases not supported");
    }

    @Override
    public void resetJoinStack() {
        // noop - path processing is not supported.
    }

    /** Method that converts an update query into SQL string */
    @Override
    public String createSqlString() throws Exception {
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append("UPDATE ");

        // 1. append table name
        DbEntity dbEnt = getRootEntity().getDbEntity();
        queryBuf.append(dbEnt.getFullyQualifiedName());

        // 2. build "set ..." clause
        buildSetClause(queryBuf, (UpdateQuery) query);

        // 3. build qualifier
        StringBuilder qualifier = new StringBuilder();
        adapter.getQualifierTranslator(this).appendPart(qualifier);
        if (qualifier.length() > 0)
            queryBuf.append(" WHERE ").append(qualifier);

        return queryBuf.toString();
    }

    /**
     * Translate updated values and relationships into "SET ATTR1 = Val1, ..." SQL
     * statement.
     */
    private void buildSetClause(StringBuffer queryBuf, UpdateQuery query) {
        Map updAttrs = query.getUpdAttributes();
        // set of keys.. each key is supposed to be ObjAttribute
        Iterator attrIt = updAttrs.entrySet().iterator();

        if (!attrIt.hasNext())
            throw new CayenneRuntimeException("Nothing to update.");

        DbEntity dbEnt = getRootEntity().getDbEntity();
        queryBuf.append(" SET ");

        // append updated attribute values
        boolean appendedSomething = false;

        // now process other attrs in the loop
        while (attrIt.hasNext()) {
            Map.Entry entry = (Map.Entry) attrIt.next();
            String nextKey = (String) entry.getKey();
            Object attrVal = entry.getValue();

            if (appendedSomething)
                queryBuf.append(", ");

            queryBuf.append(nextKey).append(" = ?");
            super.addToParamList((DbAttribute) dbEnt.getAttribute(nextKey), attrVal);
            appendedSomething = true;
        }
    }
}

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

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.JoinType;
import org.apache.cayenne.query.DeleteQuery;

/**
 * Class implements default translation mechanism of org.apache.cayenne.query.DeleteQuery
 * objects to SQL DELETE statements.
 * 
 * @deprecated since 3.0 since {@link DeleteQuery} is deprecated.
 */
public class DeleteTranslator extends QueryAssembler {

    @Override
    public void dbRelationshipAdded(
            DbRelationship relationship,
            JoinType joinType,
            String joinAplitAlias) {
        throw new UnsupportedOperationException("db relationships not supported");
    }

    @Override
    public String getCurrentAlias() {
        throw new UnsupportedOperationException("aliases not supported");
    }

    @Override
    public void resetJoinStack() {
        // noop - joins are not supported
    }

    /**
     * Main method of DeleteTranslator class. Translates DeleteQuery into a JDBC
     * PreparedStatement
     */
    @Override
    public String createSqlString() throws Exception {
        StringBuilder queryBuf = new StringBuilder("DELETE FROM ");

        // 1. append table name
        DbEntity dbEnt = getRootEntity().getDbEntity();
        queryBuf.append(dbEnt.getFullyQualifiedName());

        // 2. build qualifier
        StringBuilder qualifier = new StringBuilder();
        adapter.getQualifierTranslator(this).appendPart(qualifier);
        if (qualifier.length() > 0)
            queryBuf.append(" WHERE ").append(qualifier);

        return queryBuf.toString();
    }
}

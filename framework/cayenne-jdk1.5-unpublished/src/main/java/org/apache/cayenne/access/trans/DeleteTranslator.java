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
import org.apache.cayenne.query.DeleteQuery;

/** Class implements default translation mechanism of org.apache.cayenne.query.DeleteQuery
 *  objects to SQL DELETE statements.
 *
 *  @author Andrus Adamchik
 *  @deprecated since 3.0 since {@link DeleteQuery} is deprecated.
 */
public class DeleteTranslator extends QueryAssembler {

    public String aliasForTable(DbEntity dbEnt) {
        throw new RuntimeException("aliases not supported");
    }

    public void dbRelationshipAdded(DbRelationship dbRel) {
        throw new RuntimeException("db relationships not supported");
    }

    /** Main method of DeleteTranslator class. Translates DeleteQuery
     *  into a JDBC PreparedStatement
     */
    public String createSqlString() throws Exception {
        StringBuffer queryBuf = new StringBuffer("DELETE FROM ");

        // 1. append table name
        DbEntity dbEnt = getRootDbEntity();
        queryBuf.append(dbEnt.getFullyQualifiedName());

        // 2. build qualifier
        String qualifierStr =
            adapter.getQualifierTranslator(this).doTranslation();
        if (qualifierStr != null)
            queryBuf.append(" WHERE ").append(qualifierStr);

        return queryBuf.toString();
    }
}

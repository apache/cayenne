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
package org.apache.cayenne.dbsync.merge;

import java.util.Collections;
import java.util.List;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

public class DropColumnToDb extends AbstractToDbToken.EntityAndColumn {

    public DropColumnToDb(DbEntity entity, DbAttribute column) {
        super("Drop Column", entity, column);
    }

    @Override
    public List<String> createSql(DbAdapter adapter) {
        StringBuilder sqlBuffer = new StringBuilder();
        QuotingStrategy context = adapter.getQuotingStrategy();
        sqlBuffer.append("ALTER TABLE ");
        sqlBuffer.append(context.quotedFullyQualifiedName(getEntity()));
        sqlBuffer.append(" DROP COLUMN ");
        sqlBuffer.append(context.quotedName(getColumn()));

        return Collections.singletonList(sqlBuffer.toString());
    }

    public MergerToken createReverse(MergerTokenFactory factory) {
        return factory.createAddColumnToModel(getEntity(), getColumn());
    }

    @Override
    public int compareTo(MergerToken o) {
        // add all AddRelationshipToDb to the end.
        if (o instanceof DropRelationshipToDb) {
            return 1;
        }
        return super.compareTo(o);
    }

}

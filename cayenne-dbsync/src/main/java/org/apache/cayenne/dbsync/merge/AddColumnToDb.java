/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */
package org.apache.cayenne.dbsync.merge;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

import java.util.Collections;
import java.util.List;

public class AddColumnToDb extends AbstractToDbToken.EntityAndColumn {

    public AddColumnToDb(DbEntity entity, DbAttribute column) {
        super("Add Column", entity, column);
    }

    /**
     * append the part of the token before the actual column data type
     */
    protected void appendPrefix(StringBuffer sqlBuffer, QuotingStrategy context) {

        sqlBuffer.append("ALTER TABLE ");
        sqlBuffer.append(context.quotedFullyQualifiedName(getEntity()));
        sqlBuffer.append(" ADD COLUMN ");
        sqlBuffer.append(context.quotedName(getColumn()));
        sqlBuffer.append(" ");
    }

    @Override
    public List<String> createSql(DbAdapter adapter) {
        StringBuffer sqlBuffer = new StringBuffer();
        QuotingStrategy context = adapter.getQuotingStrategy();
        appendPrefix(sqlBuffer, context);

        sqlBuffer.append(JdbcAdapter.getType(adapter, getColumn()));
        sqlBuffer.append(JdbcAdapter.sizeAndPrecision(adapter, getColumn()));

        return Collections.singletonList(sqlBuffer.toString());
    }

    @Override
    public MergerToken createReverse(MergerTokenFactory factory) {
        return factory.createDropColumnToModel(getEntity(), getColumn());
    }

    @Override
    public int compareTo(MergerToken o) {
        // add all AddRelationshipToDb to the end.
        if (o instanceof AddRelationshipToDb) {
            return -1;
        }
        return super.compareTo(o);
    }
}

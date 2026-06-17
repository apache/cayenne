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

package org.apache.cayenne.dbsync.merge.token.db;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class SetPrimaryKeyToDb extends AbstractToDbToken.Entity {

    private Collection<DbAttribute> primaryKeyOriginal;
    private Collection<DbAttribute> primaryKeyNew;
    private String detectedPrimaryKeyName;

    public SetPrimaryKeyToDb(DbEntity entity, Collection<DbAttribute> primaryKeyOriginal,
            Collection<DbAttribute> primaryKeyNew, String detectedPrimaryKeyName) {
        super("Set Primary Key", 100, entity);

        this.primaryKeyOriginal = primaryKeyOriginal;
        this.primaryKeyNew = primaryKeyNew;
        this.detectedPrimaryKeyName = detectedPrimaryKeyName;
    }

    @Override
    public List<String> createSql(DbAdapter adapter) {
        List<String> sqls = new ArrayList<>();
        if (!primaryKeyOriginal.isEmpty()) {
            appendDropOriginalPrimaryKeySQL(adapter, sqls);
        }
        appendAddNewPrimaryKeySQL(adapter, sqls);
        return sqls;
    }

    protected QuotingStrategy resolveQuotes(DbAdapter adapter) {
        return adapter.getQuotingStrategy(getEntity());
    }

    protected void appendDropOriginalPrimaryKeySQL(DbAdapter adapter, List<String> sqls) {
        if (detectedPrimaryKeyName == null) {
            return;
        }
        QuotingStrategy quotes = resolveQuotes(adapter);
        StringBuilder sql = new StringBuilder("ALTER TABLE ");
        quotes.appendFQN(sql, getEntity().getCatalog(), getEntity().getSchema(), getEntity().getName());
        sql.append(" DROP CONSTRAINT ").append(detectedPrimaryKeyName);
        sqls.add(sql.toString());
    }

    protected void appendAddNewPrimaryKeySQL(DbAdapter adapter, List<String> sqls) {
        QuotingStrategy quotes = resolveQuotes(adapter);

        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ");
        quotes.appendFQN(sql, getEntity().getCatalog(), getEntity().getSchema(), getEntity().getName());
        sql.append(" ADD PRIMARY KEY (");
        for (Iterator<DbAttribute> it = primaryKeyNew.iterator(); it.hasNext();) {
            DbAttribute pk = it.next();
            quotes.appendStart(sql);
            sql.append(pk.getName());
            quotes.appendEnd(sql);
            if (it.hasNext()) {
                sql.append(", ");
            }
        }
        sql.append(")");
        sqls.add(sql.toString());
    }

    @Override
    public MergerToken createReverse(MergerTokenFactory factory) {
        return factory.createSetPrimaryKeyToModel(getEntity(), primaryKeyNew, primaryKeyOriginal,
                detectedPrimaryKeyName);
    }
}

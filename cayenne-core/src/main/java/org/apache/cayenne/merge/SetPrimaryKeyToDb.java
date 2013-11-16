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
package org.apache.cayenne.merge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

public class SetPrimaryKeyToDb extends AbstractToDbToken.Entity {

    private Collection<DbAttribute> primaryKeyOriginal;
    private Collection<DbAttribute> primaryKeyNew;
    private String detectedPrimaryKeyName;

    public SetPrimaryKeyToDb(DbEntity entity, Collection<DbAttribute> primaryKeyOriginal,
            Collection<DbAttribute> primaryKeyNew, String detectedPrimaryKeyName) {
        super(entity);

        this.primaryKeyOriginal = primaryKeyOriginal;
        this.primaryKeyNew = primaryKeyNew;
        this.detectedPrimaryKeyName = detectedPrimaryKeyName;
    }

    @Override
    public List<String> createSql(DbAdapter adapter) {
        List<String> sqls = new ArrayList<String>();
        if (!primaryKeyOriginal.isEmpty()) {
            appendDropOriginalPrimaryKeySQL(adapter, sqls);
        }
        appendAddNewPrimaryKeySQL(adapter, sqls);
        return sqls;
    }

    protected void appendDropOriginalPrimaryKeySQL(DbAdapter adapter, List<String> sqls) {
        if (detectedPrimaryKeyName == null) {
            return;
        }
        sqls.add("ALTER TABLE " + adapter.getQuotingStrategy().quotedFullyQualifiedName(getEntity())
                + " DROP CONSTRAINT " + detectedPrimaryKeyName);
    }

    protected void appendAddNewPrimaryKeySQL(DbAdapter adapter, List<String> sqls) {
        QuotingStrategy quotingStrategy = adapter.getQuotingStrategy();

        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ");
        sql.append(quotingStrategy.quotedFullyQualifiedName(getEntity()));
        sql.append(" ADD PRIMARY KEY (");
        for (Iterator<DbAttribute> it = primaryKeyNew.iterator(); it.hasNext();) {
            sql.append(quotingStrategy.quotedName(it.next()));
            if (it.hasNext()) {
                sql.append(", ");
            }
        }
        sql.append(")");
        sqls.add(sql.toString());
    }

    public MergerToken createReverse(MergerFactory factory) {
        return factory.createSetPrimaryKeyToModel(getEntity(), primaryKeyNew, primaryKeyOriginal,
                detectedPrimaryKeyName);
    }

    public String getTokenName() {
        return "Set Primary Key";
    }

}

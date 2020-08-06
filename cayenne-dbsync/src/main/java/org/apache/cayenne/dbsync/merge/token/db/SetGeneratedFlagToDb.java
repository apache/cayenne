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

import java.util.Collections;
import java.util.List;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

/**
 * @since 4.0
 */
public class SetGeneratedFlagToDb extends AbstractToDbToken.EntityAndColumn {

    private final boolean isGenerated;

    public SetGeneratedFlagToDb(DbEntity entity, DbAttribute column, boolean isGenerated) {
        // drop generated attribute must go first
        super("Set Is Generated", isGenerated ? 111 : 109, entity, column);
        this.isGenerated = isGenerated;
    }

    @Override
    public MergerToken createReverse(MergerTokenFactory factory) {
        return factory.createSetGeneratedFlagToModel(getEntity(), getColumn(), !isGenerated);
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    protected void appendAutoIncrement(DbAdapter adapter, StringBuffer builder) {
        throw new UnsupportedOperationException("Not supported on generic DB");
    }

    protected void appendDropAutoIncrement(DbAdapter adapter, StringBuffer builder) {
        throw new UnsupportedOperationException("Not supported on generic DB");
    }

    protected void appendAlterColumnClause(DbAdapter adapter, StringBuffer builder) {
        QuotingStrategy context = adapter.getQuotingStrategy();
        builder.append(" ALTER COLUMN ").append(context.quotedName(getColumn())).append(" ");
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> createSql(DbAdapter adapter) {
        if(!adapter.supportsGeneratedKeys()) {
            return (List<String>)Collections.EMPTY_LIST;
        }

        QuotingStrategy context = adapter.getQuotingStrategy();

        StringBuffer builder = new StringBuffer();
        builder.append("ALTER TABLE ").append(context.quotedFullyQualifiedName(getEntity()));
        appendAlterColumnClause(adapter, builder);
        if(isGenerated) {
            appendAutoIncrement(adapter, builder);
        } else {
            appendDropAutoIncrement(adapter, builder);
        }

        return Collections.singletonList(builder.toString());
    }
}

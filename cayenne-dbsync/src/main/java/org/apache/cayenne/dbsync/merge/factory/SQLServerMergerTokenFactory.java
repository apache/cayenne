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
package org.apache.cayenne.dbsync.merge.factory;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.dbsync.merge.token.db.AddColumnToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetAllowNullToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetColumnTypeToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetGeneratedFlagToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetNotNullToDb;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

import java.util.Collections;
import java.util.List;

public class SQLServerMergerTokenFactory extends DefaultMergerTokenFactory {

    @Override
    public MergerToken createSetColumnTypeToDb(
            final DbEntity entity,
            DbAttribute columnOriginal,
            final DbAttribute columnNew) {

        return new SetColumnTypeToDb(entity, columnOriginal, columnNew) {

            @Override
            protected void appendPrefix(StringBuffer sqlBuffer, QuotingStrategy context) {
                // http://msdn2.microsoft.com/en-us/library/ms190273.aspx
                sqlBuffer.append("ALTER TABLE ");
                sqlBuffer.append(context.quotedFullyQualifiedName(entity));
                sqlBuffer.append(" ALTER COLUMN ");
                sqlBuffer.append(context.quotedName(columnNew));
                sqlBuffer.append(" ");
            }
        };
    }

    @Override
    public MergerToken createAddColumnToDb(final DbEntity entity, final DbAttribute column) {
        return new AddColumnToDb(entity, column) {

            @Override
            protected void appendPrefix(StringBuffer sqlBuffer, QuotingStrategy context) {
                // http://msdn2.microsoft.com/en-us/library/ms190273.aspx
                sqlBuffer.append("ALTER TABLE ");
                sqlBuffer.append(context.quotedFullyQualifiedName(entity));
                sqlBuffer.append(" ADD ");
                sqlBuffer.append(context.quotedName(column));
                sqlBuffer.append(" ");
            }
        };
    }

    @Override
    public MergerToken createSetAllowNullToDb(DbEntity entity, final DbAttribute column) {
        return new SetAllowNullToDb(entity, column) {

            @Override
            public List<String> createSql(DbAdapter adapter) {
                StringBuffer sqlBuffer = new StringBuffer();

                sqlBuffer.append("ALTER TABLE ");
                sqlBuffer.append(adapter.getQuotingStrategy().quotedFullyQualifiedName(getEntity()));
                sqlBuffer.append(" ALTER COLUMN ");

                adapter.createTableAppendColumn(sqlBuffer, column);

                return Collections.singletonList(sqlBuffer.toString());
            }

        };
    }

    @Override
    public MergerToken createSetNotNullToDb(DbEntity entity, final DbAttribute column) {

        return new SetNotNullToDb(entity, column) {

            @Override
            public List<String> createSql(DbAdapter adapter) {
                StringBuffer sqlBuffer = new StringBuffer();

                sqlBuffer.append("ALTER TABLE ");
                sqlBuffer.append(adapter.getQuotingStrategy().quotedFullyQualifiedName(getEntity()));
                sqlBuffer.append(" ALTER COLUMN ");

                adapter.createTableAppendColumn(sqlBuffer, column);

                return Collections.singletonList(sqlBuffer.toString());
            }

        };
    }

    @Override
    public MergerToken createSetGeneratedFlagToDb(DbEntity entity, DbAttribute column, boolean isGenerated) {
        return new SetGeneratedFlagToDb(entity, column, isGenerated) {
            @Override
            protected void appendAutoIncrement(DbAdapter adapter, StringBuffer builder) {
                throw new UnsupportedOperationException("Can't automatically alter column to IDENTITY in SQLServer database. You should do this manually.");
            }

            @Override
            protected void appendDropAutoIncrement(DbAdapter adapter, StringBuffer builder) {
                throw new UnsupportedOperationException("Can't automatically alter column to drop IDENTITY in SQLServer database. You should do this manually.");
            }

            @Override
            public boolean isEmpty() {
                return false;
            }
        };
    }
}

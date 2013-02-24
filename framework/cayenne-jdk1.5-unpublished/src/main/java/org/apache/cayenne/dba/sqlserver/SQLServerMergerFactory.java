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
package org.apache.cayenne.dba.sqlserver;

import java.util.Collections;
import java.util.List;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.merge.AddColumnToDb;
import org.apache.cayenne.merge.MergerFactory;
import org.apache.cayenne.merge.MergerToken;
import org.apache.cayenne.merge.SetAllowNullToDb;
import org.apache.cayenne.merge.SetColumnTypeToDb;
import org.apache.cayenne.merge.SetNotNullToDb;

public class SQLServerMergerFactory extends MergerFactory {

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

}

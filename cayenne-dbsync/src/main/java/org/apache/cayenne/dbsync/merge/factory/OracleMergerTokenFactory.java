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
package org.apache.cayenne.dbsync.merge.factory;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.dbsync.merge.token.db.AddColumnToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetAllowNullToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetColumnTypeToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetNotNullToDb;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

import java.util.Collections;
import java.util.List;


public class OracleMergerTokenFactory extends DefaultMergerTokenFactory {

    @Override
    public MergerToken createAddColumnToDb(final DbEntity entity, final DbAttribute column) {
        return new AddColumnToDb(entity, column) {

            @Override
            protected void appendPrefix(StringBuffer sqlBuffer, QuotingStrategy quotes) {
                sqlBuffer.append("ALTER TABLE ");
                quotes.appendFQN(sqlBuffer, entity.getCatalog(), entity.getSchema(), entity.getName());
                sqlBuffer.append(" ADD ");
                quotes.appendStart(sqlBuffer);
                sqlBuffer.append(column.getName());
                quotes.appendEnd(sqlBuffer);
                sqlBuffer.append(" ");
            }
        };
    }
    
    @Override
    public MergerToken createSetColumnTypeToDb(
            final DbEntity entity,
            DbAttribute columnOriginal,
            final DbAttribute columnNew) {

        return new SetColumnTypeToDb(entity, columnOriginal, columnNew) {

            @Override
            protected void appendPrefix(StringBuffer sqlBuffer, QuotingStrategy quotes) {
                sqlBuffer.append("ALTER TABLE ");
                quotes.appendFQN(sqlBuffer, entity.getCatalog(), entity.getSchema(), entity.getName());
                sqlBuffer.append(" MODIFY ");
                quotes.appendStart(sqlBuffer);
                sqlBuffer.append(columnNew.getName());
                quotes.appendEnd(sqlBuffer);
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

                DataMap dataMap = getEntity().getDataMap();
                QuotingStrategy quotes = dataMap != null && dataMap.isQuotingSQLIdentifiers()
                        ? adapter.getQuotingStrategy() : QuotingStrategy.NONE;
                sqlBuffer.append("ALTER TABLE ");
                quotes.appendFQN(sqlBuffer, getEntity().getCatalog(), getEntity().getSchema(), getEntity().getName());
                sqlBuffer.append(" MODIFY ");

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

                DataMap dataMap = getEntity().getDataMap();
                QuotingStrategy quotes = dataMap != null && dataMap.isQuotingSQLIdentifiers()
                        ? adapter.getQuotingStrategy() : QuotingStrategy.NONE;
                sqlBuffer.append("ALTER TABLE ");
                quotes.appendFQN(sqlBuffer, getEntity().getCatalog(), getEntity().getSchema(), getEntity().getName());
                sqlBuffer.append(" MODIFY ");

                adapter.createTableAppendColumn(sqlBuffer, column);

                return Collections.singletonList(sqlBuffer.toString());
            }

        };
    }
}

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
import org.apache.cayenne.dbsync.merge.token.db.DropColumnToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetAllowNullToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetNotNullToDb;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

import java.util.Collections;
import java.util.List;

public class FirebirdMergerTokenFactory extends DefaultMergerTokenFactory {

    @Override
    public MergerToken createDropColumnToDb(DbEntity entity, DbAttribute column) {
        return new DropColumnToDb(entity, column) {
            public List<String> createSql(DbAdapter adapter) {
                DataMap dataMap = getEntity().getDataMap();
                QuotingStrategy quotes = dataMap != null && dataMap.isQuotingSQLIdentifiers()
                        ? adapter.getQuotingStrategy() : QuotingStrategy.NONE;
                StringBuilder sql = new StringBuilder("ALTER TABLE ");
                quotes.appendFQN(sql, getEntity().getCatalog(), getEntity().getSchema(), getEntity().getName());
                sql.append(" DROP ");
                quotes.appendStart(sql);
                sql.append(getColumn().getName());
                quotes.appendEnd(sql);
                return Collections.singletonList(sql.toString());
            }
        };
    }
    
    @Override
    public MergerToken createSetNotNullToDb(DbEntity entity, DbAttribute column) {
        return new SetNotNullToDb(entity, column) {
            public List<String> createSql(DbAdapter adapter) {
                DataMap dataMap = getEntity().getDataMap();
                QuotingStrategy quotes = dataMap != null && dataMap.isQuotingSQLIdentifiers()
                        ? adapter.getQuotingStrategy() : QuotingStrategy.NONE;
                String entityName = quotes.quotedFQN(getEntity().getCatalog(), getEntity().getSchema(),
                        getEntity().getName());
                String columnName = quotes.quoted(getColumn().getName());
                // Firebird doesn't support ALTER TABLE table_name ALTER column_name SET NOT NULL
                // but this might be achived by modyfication of system tables
                return Collections.singletonList(String.format("UPDATE RDB$RELATION_FIELDS SET RDB$NULL_FLAG = 1 "+
                "WHERE RDB$FIELD_NAME = '%s' AND RDB$RELATION_NAME = '%s'", columnName, entityName));
            }
        };
    }
    
    @Override
    public MergerToken createSetAllowNullToDb(DbEntity entity, DbAttribute column) {
        return new SetAllowNullToDb(entity, column) {
            public List<String> createSql(DbAdapter adapter) {
                DataMap dataMap = getEntity().getDataMap();
                QuotingStrategy quotes = dataMap != null && dataMap.isQuotingSQLIdentifiers()
                        ? adapter.getQuotingStrategy() : QuotingStrategy.NONE;
                String entityName = quotes.quotedFQN(getEntity().getCatalog(), getEntity().getSchema(),
                        getEntity().getName());
                String columnName = quotes.quoted(getColumn().getName());
                // Firebird doesn't support ALTER TABLE table_name ALTER column_name DROP NOT NULL
                // but this might be achived by modyfication system tables
                return Collections.singletonList(String.format("UPDATE RDB$RELATION_FIELDS SET RDB$NULL_FLAG = NULL "+
                " WHERE RDB$FIELD_NAME = '%s' AND RDB$RELATION_NAME = '%s'", columnName, entityName));
            }
        };
    }

    @Override
    public MergerToken createAddColumnToDb(DbEntity entity, DbAttribute column) {
        return new AddColumnToDb(entity, column) {
            protected void appendPrefix(StringBuffer sqlBuffer, QuotingStrategy quotes) {
                sqlBuffer.append("ALTER TABLE ");
                quotes.appendFQN(sqlBuffer, getEntity().getCatalog(), getEntity().getSchema(), getEntity().getName());
                sqlBuffer.append(" ADD ");
                quotes.appendStart(sqlBuffer);
                sqlBuffer.append(getColumn().getName());
                quotes.appendEnd(sqlBuffer);
                sqlBuffer.append(" ");
            }
        };
    }

}

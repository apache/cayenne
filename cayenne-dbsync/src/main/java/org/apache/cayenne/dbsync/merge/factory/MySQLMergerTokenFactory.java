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
import org.apache.cayenne.dbsync.merge.token.db.DropRelationshipToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetAllowNullToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetColumnTypeToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetGeneratedFlagToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetNotNullToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetPrimaryKeyToDb;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;

import java.sql.Types;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MySQLMergerTokenFactory extends DefaultMergerTokenFactory {

    @Override
    public MergerToken createSetNotNullToDb(
            final DbEntity entity,
            final DbAttribute column) {
        return new SetNotNullToDb(entity, column) {

            @Override
            public List<String> createSql(DbAdapter adapter) {
                StringBuffer sqlBuffer = new StringBuffer();

                DataMap dataMap = getEntity().getDataMap();
                QuotingStrategy quotes = dataMap != null && dataMap.isQuotingSQLIdentifiers()
                        ? adapter.getQuotingStrategy() : QuotingStrategy.NONE;

                sqlBuffer.append("ALTER TABLE ");
                quotes.appendFQN(sqlBuffer, getEntity().getCatalog(), getEntity().getSchema(), getEntity().getName());
                sqlBuffer.append(" CHANGE ");
                quotes.appendStart(sqlBuffer);
                sqlBuffer.append(getColumn().getName());
                quotes.appendEnd(sqlBuffer);
                sqlBuffer.append(" ");
                adapter.createTableAppendColumn(sqlBuffer, column);

                return Collections.singletonList(sqlBuffer.toString());
            }

        };
    }

    @Override
    public MergerToken createSetAllowNullToDb(
            final DbEntity entity,
            final DbAttribute column) {
        return new SetAllowNullToDb(entity, column) {

            @Override
            public List<String> createSql(DbAdapter adapter) {
                StringBuffer sqlBuffer = new StringBuffer();

                DataMap dataMap = getEntity().getDataMap();
                QuotingStrategy quotes = dataMap != null && dataMap.isQuotingSQLIdentifiers()
                        ? adapter.getQuotingStrategy() : QuotingStrategy.NONE;

                sqlBuffer.append("ALTER TABLE ");
                quotes.appendFQN(sqlBuffer, getEntity().getCatalog(), getEntity().getSchema(), getEntity().getName());
                sqlBuffer.append(" CHANGE ");
                quotes.appendStart(sqlBuffer);
                sqlBuffer.append(getColumn().getName());
                quotes.appendEnd(sqlBuffer);
                sqlBuffer.append(" ");
                adapter.createTableAppendColumn(sqlBuffer, column);

                return Collections.singletonList(sqlBuffer.toString());
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
                // http://dev.mysql.com/tech-resources/articles/mysql-cluster-50.html
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
    public MergerToken createDropRelationshipToDb(
            final DbEntity entity,
            DbRelationship rel) {

        return new DropRelationshipToDb(entity, rel) {

            @Override
            public List<String> createSql(DbAdapter adapter) {
                String fkName = getFkName();

                if (fkName == null) {
                    return Collections.emptyList();
                }
                DataMap dataMap = entity.getDataMap();
                QuotingStrategy quotes = dataMap != null && dataMap.isQuotingSQLIdentifiers()
                        ? adapter.getQuotingStrategy() : QuotingStrategy.NONE;

                // http://dev.mysql.com/tech-resources/articles/mysql-cluster-50.html
                StringBuilder sql = new StringBuilder("ALTER TABLE ");
                quotes.appendFQN(sql, entity.getCatalog(), entity.getSchema(), entity.getName());
                sql.append(" DROP FOREIGN KEY ").append(fkName);
                return Collections.singletonList(sql.toString());
            }
        };
    }
    
    @Override
    public MergerToken createSetPrimaryKeyToDb(
            DbEntity entity,
            Collection<DbAttribute> primaryKeyOriginal,
            Collection<DbAttribute> primaryKeyNew,
            String detectedPrimaryKeyName) {
        return new SetPrimaryKeyToDb(
                entity,
                primaryKeyOriginal,
                primaryKeyNew,
                detectedPrimaryKeyName) {

            @Override
            protected void appendDropOriginalPrimaryKeySQL(
                    DbAdapter adapter,
                    List<String> sqls) {
                QuotingStrategy quotes = resolveQuotes(adapter);
                StringBuilder sql = new StringBuilder("ALTER TABLE ");
                quotes.appendFQN(sql, getEntity().getCatalog(), getEntity().getSchema(), getEntity().getName());
                sql.append(" DROP PRIMARY KEY");
                sqls.add(sql.toString());
            }

        };
    }

    @Override
    public MergerToken createSetGeneratedFlagToDb(DbEntity entity, DbAttribute column, boolean isGenerated) {
        return new SetGeneratedFlagToDb(entity, column, isGenerated) {
            protected void appendAlterColumnClause(DbAdapter adapter, StringBuffer builder) {
                builder.append(" MODIFY ");
            }

            @Override
            protected void appendAutoIncrement(DbAdapter adapter, StringBuffer builder) {
                adapter.createTableAppendColumn(builder, this.getColumn());
            }

            /**
             * To drop AUTO_INCREMENT flag update column with all information but w/o AUTO_INCREMENT
             */
            @Override
            protected void appendDropAutoIncrement(DbAdapter adapter, StringBuffer builder) {
                adapter.createTableAppendColumn(builder, this.getColumn());
            }

            @Override
            public boolean isEmpty() {
                return false;
            }
        };
    }

    @Override
    public boolean needUpdateSpecificType(DbAttribute columnOriginal, DbAttribute columnNew) {
        if ( (columnOriginal.getType() == Types.BOOLEAN && columnNew.getType() == Types.BIT) ||
                (columnOriginal.getType() == Types.BLOB && columnNew.getType() == Types.LONGVARBINARY)) {
            return false;
        }
        return true;
    }
}

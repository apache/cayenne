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
package org.apache.cayenne.dba.mysql;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.merge.DropRelationshipToDb;
import org.apache.cayenne.merge.MergerFactory;
import org.apache.cayenne.merge.MergerToken;
import org.apache.cayenne.merge.SetAllowNullToDb;
import org.apache.cayenne.merge.SetColumnTypeToDb;
import org.apache.cayenne.merge.SetNotNullToDb;
import org.apache.cayenne.merge.SetPrimaryKeyToDb;

public class MySQLMergerFactory extends MergerFactory {

    @Override
    public MergerToken createSetNotNullToDb(
            final DbEntity entity,
            final DbAttribute column) {
        return new SetNotNullToDb(entity, column) {

            @Override
            public List<String> createSql(DbAdapter adapter) {
                StringBuffer sqlBuffer = new StringBuffer();

                QuotingStrategy context = adapter.getQuotingStrategy();

                sqlBuffer.append("ALTER TABLE ");
                sqlBuffer.append(context.quotedFullyQualifiedName(getEntity()));
                sqlBuffer.append(" CHANGE ");
                sqlBuffer.append(context.quotedName(getColumn()));
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

                QuotingStrategy context = adapter.getQuotingStrategy();

                sqlBuffer.append("ALTER TABLE ");
                sqlBuffer.append(context.quotedFullyQualifiedName(getEntity()));
                sqlBuffer.append(" CHANGE ");
                sqlBuffer.append(context.quotedName(getColumn()));
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
            protected void appendPrefix(StringBuffer sqlBuffer, QuotingStrategy context) {
                // http://dev.mysql.com/tech-resources/articles/mysql-cluster-50.html
                sqlBuffer.append("ALTER TABLE ");
                sqlBuffer.append(context.quotedFullyQualifiedName(entity));
                sqlBuffer.append(" MODIFY ");
                sqlBuffer.append(context.quotedName(columnNew));
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
                QuotingStrategy context = adapter.getQuotingStrategy();
                StringBuilder buf = new StringBuilder();
                // http://dev.mysql.com/tech-resources/articles/mysql-cluster-50.html
                buf.append("ALTER TABLE ");
                buf.append(context.quotedFullyQualifiedName(entity));
                buf.append(" DROP FOREIGN KEY ");
                buf.append(fkName);

                return Collections.singletonList(buf.toString());
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
                sqls.add("ALTER TABLE "
                        + adapter.getQuotingStrategy()
                                .quotedFullyQualifiedName(getEntity())
                        + " DROP PRIMARY KEY");
            }

        };
    }
}

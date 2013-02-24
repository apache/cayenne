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
package org.apache.cayenne.dba.sybase;

import java.util.Collections;
import java.util.List;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.merge.AddColumnToDb;
import org.apache.cayenne.merge.DropColumnToDb;
import org.apache.cayenne.merge.MergerFactory;
import org.apache.cayenne.merge.MergerToken;
import org.apache.cayenne.merge.SetAllowNullToDb;
import org.apache.cayenne.merge.SetColumnTypeToDb;
import org.apache.cayenne.merge.SetNotNullToDb;

/**
 * @since 3.0
 */
public class SybaseMergerFactory extends MergerFactory {

    /**
     * @since 3.0
     */
    @Override
    public MergerToken createAddColumnToDb(DbEntity entity, final DbAttribute column) {
        return new AddColumnToDb(entity, column) {

            @Override
            public List<String> createSql(DbAdapter adapter) {

                StringBuffer sqlBuffer = new StringBuffer();
                QuotingStrategy context = adapter.getQuotingStrategy();
                sqlBuffer.append("ALTER TABLE ");
                sqlBuffer.append(context.quotedFullyQualifiedName(getEntity()));
                sqlBuffer.append(" ADD ");
                boolean magnatory = column.isMandatory();
                column.setMandatory(false);
                adapter.createTableAppendColumn(sqlBuffer, column);
                if(magnatory){
                    column.setMandatory(magnatory);
                }
                return Collections.singletonList(sqlBuffer.toString());
            }

        };
    }

    /**
     * @since 3.0
     */
    @Override
    public MergerToken createDropColumnToDb(DbEntity entity, DbAttribute column) {
        return new DropColumnToDb(entity, column) {

            @Override
            public List<String> createSql(DbAdapter adapter) {
                StringBuilder sqlBuffer = new StringBuilder();
                QuotingStrategy context = adapter.getQuotingStrategy();
                sqlBuffer.append("ALTER TABLE ");
                sqlBuffer.append(context.quotedFullyQualifiedName(getEntity()));
                sqlBuffer.append(" DROP ");
                sqlBuffer.append(context.quotedName(getColumn()));

                return Collections.singletonList(sqlBuffer.toString());
            }

        };
    }

    /**
     * @since 3.0
     */
    @Override
    public MergerToken createSetNotNullToDb(DbEntity entity, DbAttribute column) {
        return new SetNotNullToDb(entity, column) {

            @Override
            public List<String> createSql(DbAdapter adapter) {

                StringBuffer sqlBuffer = createStringQuery(
                        adapter,
                        getEntity(),
                        getColumn());

                return Collections.singletonList(sqlBuffer.toString());
            }

        };
    }

    /**
     * @since 3.0
     */
    @Override
    public MergerToken createSetAllowNullToDb(DbEntity entity, DbAttribute column) {
        return new SetAllowNullToDb(entity, column) {

            @Override
            public List<String> createSql(DbAdapter adapter) {
                StringBuffer sqlBuffer = createStringQuery(
                        adapter,
                        getEntity(),
                        getColumn());
                return Collections.singletonList(sqlBuffer.toString());
            }
        };
    }

    /**
     * @since 3.0
     */
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

    private static StringBuffer createStringQuery(
            DbAdapter adapter,
            DbEntity entity,
            DbAttribute column) {
        StringBuffer sqlBuffer = new StringBuffer();
        QuotingStrategy context = adapter.getQuotingStrategy();
        sqlBuffer.append("ALTER TABLE ");
        sqlBuffer.append(context.quotedFullyQualifiedName(entity));
        sqlBuffer.append(" MODIFY ");
        adapter.createTableAppendColumn(sqlBuffer, column);

        return sqlBuffer;
    }

}

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
package org.apache.cayenne.dba.openbase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.merge.CreateTableToDb;
import org.apache.cayenne.merge.DropRelationshipToDb;
import org.apache.cayenne.merge.MergerFactory;
import org.apache.cayenne.merge.MergerToken;
import org.apache.cayenne.merge.SetAllowNullToDb;
import org.apache.cayenne.merge.SetColumnTypeToDb;
import org.apache.cayenne.merge.SetNotNullToDb;

public class OpenBaseMergerFactory extends MergerFactory {

    @Override
    public MergerToken createCreateTableToDb(DbEntity entity) {
        return new CreateTableToDb(entity) {

            @Override
            public List<String> createSql(DbAdapter adapter) {
                List<String> sqls = new ArrayList<String>();
                // create table first as OpenBase adapter created primary key in its
                // getPkGenerator().createAutoPkStatements
                sqls.add(adapter.createTable(getEntity()));
                sqls.addAll(adapter.getPkGenerator().createAutoPkStatements(
                        Collections.singletonList(getEntity())));
                return sqls;
            }

        };
    }

    @Override
    public MergerToken createDropRelationshipToDb(
            final DbEntity entity,
            final DbRelationship rel) {
        return new DropRelationshipToDb(entity, rel) {

            @Override
            public List<String> createSql(DbAdapter adapter) {

                StringBuilder buf = new StringBuilder();

                buf.append("delete from _SYS_RELATIONSHIP where ");

                // FK_NAME form jdbc metadata seem to be wrong. It contain a column name
                // and not the 'relationshipName'
                // TODO: tell openbase developer mail list

                DbEntity source = getEntity();
                DbEntity dest = (DbEntity) rel.getTargetEntity();

                // only use the first. See adapter
                // TODO: can we be sure this is the first and same as used by the adapter?
                DbJoin join = rel.getJoins().get(0);

                // see comment in adapter for why source and dest is switched around..

                buf.append(" source_table = '");
                buf.append(dest.getFullyQualifiedName());
                buf.append("'");

                buf.append(" and source_column = '");
                buf.append(join.getTargetName());
                buf.append("'");

                buf.append(" and dest_table = '");
                buf.append(source.getFullyQualifiedName());
                buf.append("'");

                buf.append(" and dest_column = '");
                buf.append(join.getSourceName());
                buf.append("'");

                return Collections.singletonList(buf.toString());
            }

        };
    }

    @Override
    public MergerToken createSetColumnTypeToDb(
            final DbEntity entity,
            final DbAttribute columnOriginal,
            final DbAttribute columnNew) {
        return new SetColumnTypeToDb(entity, columnOriginal, columnNew) {

            @Override
            public List<String> createSql(DbAdapter adapter) {
                List<String> sqls = new ArrayList<String>();

                if (columnOriginal.getMaxLength() != columnNew.getMaxLength()) {
                    sqls.add("ALTER TABLE "
                            + entity.getFullyQualifiedName()
                            + " COLUMN "
                            + columnNew.getName()
                            + " SET LENGTH "
                            + columnNew.getMaxLength());
                }

                return sqls;
            }

        };
    }

    @Override
    public MergerToken createSetNotNullToDb(DbEntity entity, DbAttribute column) {
        return new SetNotNullToDb(entity, column) {

            @Override
            public List<String> createSql(DbAdapter adapter) {
                StringBuilder sqlBuffer = new StringBuilder();

                sqlBuffer.append("ALTER TABLE ");
                sqlBuffer.append(getEntity().getFullyQualifiedName());
                sqlBuffer.append(" COLUMN ");
                sqlBuffer.append(getColumn().getName());
                sqlBuffer.append(" SET NOT NULL");

                return Collections.singletonList(sqlBuffer.toString());
            }

        };
    }

    @Override
    public MergerToken createSetAllowNullToDb(DbEntity entity, DbAttribute column) {
        return new SetAllowNullToDb(entity, column) {

            @Override
            public List<String> createSql(DbAdapter adapter) {
                StringBuilder sqlBuffer = new StringBuilder();

                sqlBuffer.append("ALTER TABLE ");
                sqlBuffer.append(getEntity().getFullyQualifiedName());
                sqlBuffer.append(" COLUMN ");
                sqlBuffer.append(getColumn().getName());
                sqlBuffer.append(" SET NULL");

                return Collections.singletonList(sqlBuffer.toString());
            }

        };
    }
}

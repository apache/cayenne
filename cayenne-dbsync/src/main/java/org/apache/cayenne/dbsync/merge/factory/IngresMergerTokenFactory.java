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
import org.apache.cayenne.dbsync.merge.token.db.AddRelationshipToDb;
import org.apache.cayenne.dbsync.merge.token.db.DropColumnToDb;
import org.apache.cayenne.dbsync.merge.token.db.DropRelationshipToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetAllowNullToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetColumnTypeToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetGeneratedFlagToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetNotNullToDb;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;

import java.util.Collections;
import java.util.List;

public class IngresMergerTokenFactory extends DefaultMergerTokenFactory {

    @Override
    public MergerToken createSetColumnTypeToDb(final DbEntity entity, DbAttribute columnOriginal,
            final DbAttribute columnNew) {

        return new SetColumnTypeToDb(entity, columnOriginal, columnNew) {

            @Override
            protected void appendPrefix(StringBuffer sqlBuffer, QuotingStrategy context) {
                sqlBuffer.append("ALTER TABLE ");
                sqlBuffer.append(context.quotedFullyQualifiedName(entity));
                sqlBuffer.append(" ALTER COLUMN ");
                sqlBuffer.append(context.quotedName(columnNew));
                sqlBuffer.append(" ");
            }
        };
    }

    @Override
    public MergerToken createDropColumnToDb(DbEntity entity, DbAttribute column) {
        return new DropColumnToDb(entity, column) {

            @Override
            public List<String> createSql(DbAdapter adapter) {
                StringBuilder buf = new StringBuilder();
                QuotingStrategy context = adapter.getQuotingStrategy();
                buf.append("ALTER TABLE ");
                buf.append(context.quotedFullyQualifiedName(getEntity()));
                buf.append(" DROP COLUMN ");
                buf.append(context.quotedName(getColumn()));
                buf.append(" RESTRICT ");

                return Collections.singletonList(buf.toString());
            }

        };
    }

    @Override
    public MergerToken createAddRelationshipToDb(DbEntity entity, final DbRelationship rel) {
        return new AddRelationshipToDb(entity, rel) {
            @Override
            public List<String> createSql(DbAdapter adapter) {
                if (rel.isToMasterPK()) {

                    DbEntity source = (DbEntity) rel.getSourceEntity();
                    QuotingStrategy context = adapter.getQuotingStrategy();
                    StringBuilder buf = new StringBuilder();
                    StringBuilder refBuf = new StringBuilder();

                    buf.append("ALTER TABLE ");
                    buf.append(context.quotedFullyQualifiedName(source));

                    // requires the ADD CONSTRAINT statement
                    buf.append(" ADD CONSTRAINT ");
                    String name = "U_" + rel.getSourceEntity().getName() + "_"
                            + (long) (System.currentTimeMillis() / (Math.random() * 100000));

                    buf.append(context.quotedIdentifier(rel.getSourceEntity(), name));
                    buf.append(" FOREIGN KEY (");

                    boolean first = true;
                    for (DbJoin join : rel.getJoins()) {
                        if (!first) {
                            buf.append(", ");
                            refBuf.append(", ");
                        } else {
                            first = false;
                        }

                        buf.append(context.quotedSourceName(join));
                        refBuf.append(context.quotedTargetName(join));
                    }

                    buf.append(") REFERENCES ");
                    buf.append(context.quotedFullyQualifiedName((DbEntity) rel.getTargetEntity()));
                    buf.append(" (");
                    buf.append(refBuf.toString());
                    buf.append(')');

                    // also make sure we delete dependent FKs
                    buf.append(" ON DELETE CASCADE");

                    return Collections.singletonList(buf.toString());
                }

                return Collections.emptyList();

            }
        };
    }

    @Override
    public MergerToken createSetNotNullToDb(DbEntity entity, DbAttribute column) {
        return new SetNotNullToDb(entity, column) {

            @Override
            public List<String> createSql(DbAdapter adapter) {

                /*
                 * TODO: we generate this query as in ingres db documentation,
                 * but unfortunately ingres don't support it
                 */

                StringBuilder sqlBuffer = new StringBuilder();

                QuotingStrategy context = adapter.getQuotingStrategy();

                sqlBuffer.append("ALTER TABLE ");
                sqlBuffer.append(getEntity().getFullyQualifiedName());
                sqlBuffer.append(" ALTER COLUMN ");
                sqlBuffer.append(context.quotedName(getColumn()));
                sqlBuffer.append(" ");
                sqlBuffer.append(adapter.externalTypesForJdbcType(getColumn().getType())[0]);

                if (adapter.typeSupportsLength(getColumn().getType()) && getColumn().getMaxLength() > 0) {
                    sqlBuffer.append("(");
                    sqlBuffer.append(getColumn().getMaxLength());
                    sqlBuffer.append(")");
                }

                sqlBuffer.append(" NOT NULL");

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
                QuotingStrategy context = adapter.getQuotingStrategy();
                sqlBuffer.append("ALTER TABLE ");
                sqlBuffer.append(context.quotedFullyQualifiedName(getEntity()));
                sqlBuffer.append(" ALTER COLUMN ");
                sqlBuffer.append(context.quotedName(getColumn()));
                sqlBuffer.append(" ");
                sqlBuffer.append(adapter.externalTypesForJdbcType(getColumn().getType())[0]);

                if (adapter.typeSupportsLength(getColumn().getType()) && getColumn().getMaxLength() > 0) {
                    sqlBuffer.append("(");
                    sqlBuffer.append(getColumn().getMaxLength());
                    sqlBuffer.append(")");
                }

                sqlBuffer.append(" WITH NULL");

                return Collections.singletonList(sqlBuffer.toString());
            }

        };
    }

    @Override
    public MergerToken createDropRelationshipToDb(final DbEntity entity, DbRelationship rel) {

        return new DropRelationshipToDb(entity, rel) {

            @Override
            public List<String> createSql(DbAdapter adapter) {
                String fkName = getFkName();

                if (fkName == null) {
                    return Collections.emptyList();
                }
                
                StringBuilder buf = new StringBuilder();
                buf.append("ALTER TABLE ");
                buf.append(adapter.getQuotingStrategy().quotedFullyQualifiedName(getEntity()));
                buf.append(" DROP CONSTRAINT ");
                buf.append(fkName);
                buf.append(" CASCADE ");

                return Collections.singletonList(buf.toString());
            }
        };
    }

    @Override
    public MergerToken createSetGeneratedFlagToDb(DbEntity entity, DbAttribute column, boolean isGenerated) {
        return new SetGeneratedFlagToDb(entity, column, isGenerated) {
            @Override
            protected void appendAutoIncrement(DbAdapter adapter, StringBuffer builder) {
                throw new UnsupportedOperationException("Can't automatically alter column to IDENTITY in Ingres database. You should do this manually.");
            }

            @Override
            protected void appendDropAutoIncrement(DbAdapter adapter, StringBuffer builder) {
                builder.append("DROP IDENTITY");
            }

            @Override
            public boolean isEmpty() {
                return false;
            }
        };
    }
}

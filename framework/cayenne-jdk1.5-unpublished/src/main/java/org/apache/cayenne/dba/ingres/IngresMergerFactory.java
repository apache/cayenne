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
package org.apache.cayenne.dba.ingres;

import java.util.Collections;
import java.util.List;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.merge.DropColumnToDb;
import org.apache.cayenne.merge.DropRelationshipToDb;
import org.apache.cayenne.merge.MergerFactory;
import org.apache.cayenne.merge.MergerToken;
import org.apache.cayenne.merge.SetColumnTypeToDb;


public class IngresMergerFactory extends MergerFactory {
    
    @Override
    public MergerToken createSetColumnTypeToDb(
            final DbEntity entity,
            DbAttribute columnOriginal,
            final DbAttribute columnNew) {

        return new SetColumnTypeToDb(entity, columnOriginal, columnNew) {

            @Override
            protected void appendPrefix(StringBuffer sqlBuffer, QuotingStrategy context) {
                sqlBuffer.append("ALTER TABLE ");
                sqlBuffer.append(context.quoteFullyQualifiedName(entity));
                sqlBuffer.append(" ALTER COLUMN ");
                sqlBuffer.append(context.quoteString(columnNew.getName()));
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
                QuotingStrategy context = adapter.getQuotingStrategy(getEntity()
                        .getDataMap()
                        .isQuotingSQLIdentifiers());
                buf.append("ALTER TABLE ");
                buf.append(context.quoteFullyQualifiedName(getEntity()));
                buf.append(" DROP COLUMN ");
                buf.append(context.quoteString(getColumn().getName()));
                buf.append(" RESTRICT ");

                return Collections.singletonList(buf.toString());
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
                QuotingStrategy context = adapter.getQuotingStrategy(getEntity()
                        .getDataMap()
                        .isQuotingSQLIdentifiers());
                StringBuilder buf = new StringBuilder();
                buf.append("ALTER TABLE ");
                buf.append(context.quoteFullyQualifiedName(getEntity()));
                buf.append(" DROP CONSTRAINT ");
                buf.append(fkName);
                buf.append(" RESTRICT ");

                return Collections.singletonList(buf.toString());
            }
        };
    }
}

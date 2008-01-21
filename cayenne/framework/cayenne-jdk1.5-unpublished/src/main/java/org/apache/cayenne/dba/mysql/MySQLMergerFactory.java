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

import java.util.Collections;
import java.util.List;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.merge.DropRelationshipToDb;
import org.apache.cayenne.merge.MergerFactory;
import org.apache.cayenne.merge.MergerToken;
import org.apache.cayenne.merge.SetColumnTypeToDb;

public class MySQLMergerFactory extends MergerFactory {

    @Override
    public MergerToken createSetColumnTypeToDb(
            final DbEntity entity,
            DbAttribute columnOriginal,
            final DbAttribute columnNew) {

        return new SetColumnTypeToDb(entity, columnOriginal, columnNew) {

            @Override
            protected void appendPrefix(StringBuffer sqlBuffer) {
                // http://dev.mysql.com/tech-resources/articles/mysql-cluster-50.html
                sqlBuffer.append("ALTER TABLE ");
                sqlBuffer.append(entity.getFullyQualifiedName());
                sqlBuffer.append(" MODIFY ");
                sqlBuffer.append(columnNew.getName());
                sqlBuffer.append(" ");
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
                // http://dev.mysql.com/tech-resources/articles/mysql-cluster-50.html
                buf.append("ALTER TABLE ");
                buf.append(entity.getFullyQualifiedName());
                buf.append(" DROP FOREIGN KEY ");
                buf.append(fkName);

                return Collections.singletonList(buf.toString());
            }
        };
    }
}

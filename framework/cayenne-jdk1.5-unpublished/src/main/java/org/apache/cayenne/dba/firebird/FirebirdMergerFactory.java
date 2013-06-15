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
 
package org.apache.cayenne.dba.firebird;

import java.util.Collections;
import java.util.List;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.merge.MergerFactory;
import org.apache.cayenne.merge.MergerToken;
import org.apache.cayenne.merge.AddColumnToDb;
import org.apache.cayenne.merge.DropColumnToDb;
import org.apache.cayenne.merge.SetNotNullToDb;
import org.apache.cayenne.merge.SetAllowNullToDb;

public class FirebirdMergerFactory extends MergerFactory {
    
    public MergerToken createDropColumnToDb(DbEntity entity, DbAttribute column) {
        return new DropColumnToDb(entity, column) {
            public List<String> createSql(DbAdapter adapter) {
                QuotingStrategy quoting = adapter.getQuotingStrategy();
                StringBuilder builder = new StringBuilder("ALTER TABLE ");
                builder.append(quoting.quotedFullyQualifiedName(getEntity()));
                builder.append(" DROP ").append(quoting.quotedName(getColumn()));
                return Collections.singletonList(builder.toString());
            }
        };
    }
    
    @Override
    public MergerToken createSetNotNullToDb(DbEntity entity, DbAttribute column) {
        return new SetNotNullToDb(entity, column) {
            public List<String> createSql(DbAdapter adapter) {
                QuotingStrategy context = adapter.getQuotingStrategy();
                String entityName = context.quotedFullyQualifiedName(getEntity()) ;
                String columnName = context.quotedName(getColumn());
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
                QuotingStrategy context = adapter.getQuotingStrategy();
                String entityName = context.quotedFullyQualifiedName(getEntity()) ;
                String columnName = context.quotedName(getColumn()); 
                // Firebird doesn't support ALTER TABLE table_name ALTER column_name DROP NOT NULL
                // but this might be achived by modyfication system tables 
                return Collections.singletonList(String.format("UPDATE RDB$RELATION_FIELDS SET RDB$NULL_FLAG = NULL "+
                " WHERE RDB$FIELD_NAME = '%s' AND RDB$RELATION_NAME = '%s'", columnName, entityName));
            }
        };
    }
    
    
    public MergerToken createAddColumnToDb(DbEntity entity, DbAttribute column) {
        return new AddColumnToDb(entity, column) {
            protected void appendPrefix(StringBuffer sqlBuffer, QuotingStrategy context) {
                sqlBuffer.append("ALTER TABLE ");
                sqlBuffer.append(context.quotedFullyQualifiedName(getEntity()));
                sqlBuffer.append(" ADD ");
                sqlBuffer.append(context.quotedName(getColumn()));
                sqlBuffer.append(" ");
            }
        };
    }

}

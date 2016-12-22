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

package org.apache.cayenne.dbsync.merge.token.db;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

import java.util.Collections;
import java.util.List;

/**
 * An {@link MergerToken} to use to set type, length and precision.
 */
public class SetColumnTypeToDb extends AbstractToDbToken.Entity {

    private DbAttribute columnOriginal;
    private DbAttribute columnNew;

    public SetColumnTypeToDb(DbEntity entity, DbAttribute columnOriginal, DbAttribute columnNew) {
        super("Set Column Type", 60, entity);
        this.columnOriginal = columnOriginal;
        this.columnNew = columnNew;
    }
    
    /**
     * append the part of the token before the actual column data type
     * @param context 
     */
    protected void appendPrefix(StringBuffer sqlBuffer, QuotingStrategy context) {
        sqlBuffer.append("ALTER TABLE ");
        sqlBuffer.append(context.quotedFullyQualifiedName(getEntity()));
        sqlBuffer.append(" ALTER ");
        sqlBuffer.append(context.quotedName(columnNew));
        sqlBuffer.append(" TYPE ");
    }

    @Override
    public List<String> createSql(DbAdapter adapter) {
        StringBuffer sqlBuffer = new StringBuffer();
        appendPrefix(sqlBuffer, adapter.getQuotingStrategy());
  
        sqlBuffer.append(JdbcAdapter.getType(adapter, columnNew));
        sqlBuffer.append(JdbcAdapter.sizeAndPrecision(adapter, columnNew));

        return Collections.singletonList(sqlBuffer.toString());
    }

    @Override
    public String getTokenValue() {
        StringBuilder sb = new StringBuilder();
        sb.append(getEntity().getName());
        sb.append(".");
        sb.append(columnNew.getName());

        if (columnOriginal.getType() != columnNew.getType()) {
            sb.append(" type: ");
            sb.append(TypesMapping.getSqlNameByType(columnOriginal.getType()));
            sb.append(" -> ");
            sb.append(TypesMapping.getSqlNameByType(columnNew.getType()));
        }

        if (columnOriginal.getMaxLength() != columnNew.getMaxLength()) {
            sb.append(" maxLength: ");
            sb.append(columnOriginal.getMaxLength());
            sb.append(" -> ");
            sb.append(columnNew.getMaxLength());
        }

        if (columnOriginal.getAttributePrecision() != columnNew.getAttributePrecision()) {
            sb.append(" precision: ");
            sb.append(columnOriginal.getAttributePrecision());
            sb.append(" -> ");
            sb.append(columnNew.getAttributePrecision());
        }

        if (columnOriginal.getScale() != columnNew.getScale()) {
            sb.append(" scale: ");
            sb.append(columnOriginal.getScale());
            sb.append(" -> ");
            sb.append(columnNew.getScale());
        }

        return sb.toString();
    }

    @Override
    public MergerToken createReverse(MergerTokenFactory factory) {
        return factory.createSetColumnTypeToModel(getEntity(), columnNew, columnOriginal);
    }

    public DbAttribute getColumnNew() {
        return columnNew;
    }

    public DbAttribute getColumnOriginal() {
        return columnOriginal;
    }
}

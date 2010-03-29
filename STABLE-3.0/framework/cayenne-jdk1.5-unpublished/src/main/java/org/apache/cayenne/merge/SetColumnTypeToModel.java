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
package org.apache.cayenne.merge;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

/**
 * A {@link MergerToken} that modifies one original {@link DbAttribute} to match another
 * new {@link DbAttribute}s type, maxLength and precision. The name and mandatory fields
 * are not modified by this token.
 * 
 */
public class SetColumnTypeToModel extends AbstractToModelToken.Entity {

    private DbAttribute columnOriginal;
    private DbAttribute columnNew;

    public SetColumnTypeToModel(DbEntity entity, DbAttribute columnOriginal,
            DbAttribute columnNew) {
        super(entity);
        this.columnOriginal = columnOriginal;
        this.columnNew = columnNew;
    }

    public MergerToken createReverse(MergerFactory factory) {
        return factory.createSetColumnTypeToDb(getEntity(), columnNew, columnOriginal);
    }

    public void execute(MergerContext mergerContext) {
        columnOriginal.setType(columnNew.getType());
        columnOriginal.setMaxLength(columnNew.getMaxLength());
        columnOriginal.setAttributePrecision(columnNew.getAttributePrecision());
        columnOriginal.setScale(columnNew.getScale());
        mergerContext.getModelMergeDelegate().dbAttributeModified(columnOriginal);
    }

    public String getTokenName() {
        return "Set Column Type";
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
    
    public DbAttribute getColumnOriginal() {
        return columnOriginal;
    }

    public DbAttribute getColumnNew() {
        return columnNew;
    }
    
}

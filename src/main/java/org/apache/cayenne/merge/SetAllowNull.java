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

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

public class SetAllowNull extends AbstractMergerToken {

    private DbEntity entity;
    private DbAttribute column;

    public SetAllowNull(MergeDirection direction, DbEntity entity, DbAttribute column) {
        super(direction);
        this.entity = entity;
        this.column = column;
    }

    public void execute(MergerContext mergerContext) {
        switch (getDirection().getId()) {
            case MergeDirection.TO_DB_ID:
                mergerContext.executeSql(createSql(mergerContext.getAdapter()));
                break;
            case MergeDirection.TO_MODEL_ID:
                column.setMandatory(false);
                break;
        }
    }

    public String createSql(DbAdapter adapter) {
        StringBuffer sqlBuffer = new StringBuffer();

        sqlBuffer.append("ALTER TABLE ");
        sqlBuffer.append(entity.getFullyQualifiedName());
        sqlBuffer.append(" ALTER COLUMN ");
        sqlBuffer.append(column.getName());
        sqlBuffer.append(" DROP NOT NULL");

        return sqlBuffer.toString();
    }

    public String getTokenName() {
        return "Set Allow Null";
    }

    public String getTokenValue() {
        return entity.getName() + "." + column.getName();
    }

    public MergerToken createReverse(MergerFactory factory) {
        return factory.createSetNotNull(reverseDirection(), entity, column);
    }

}

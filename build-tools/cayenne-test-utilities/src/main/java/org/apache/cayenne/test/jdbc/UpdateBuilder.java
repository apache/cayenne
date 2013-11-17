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
package org.apache.cayenne.test.jdbc;

public class UpdateBuilder extends SQLBuilder {

    protected int setCount;

    protected UpdateBuilder(DBHelper dbHelper, String tableName) {
        super(dbHelper);
        sqlBuffer.append("update ").append(dbHelper.quote(tableName)).append(" set ");
    }

    public UpdateBuilder set(String column, Object value) {
        return set(column, value, NO_TYPE);
    }

    public UpdateBuilder set(String column, Object value, int valueType) {
        if (setCount++ > 0) {
            sqlBuffer.append(", ");
        }

        sqlBuffer.append(dbHelper.quote(column)).append(" = ?");
        initBinding(value, valueType);
        return this;
    }

    public WhereBuilder where(String column, Object value) {
        return where(column, value, NO_TYPE);
    }

    public WhereBuilder where(String column, Object value, int valueType) {
        WhereBuilder where = new WhereBuilder(dbHelper, sqlBuffer, bindings, bindingTypes);
        where.and(column, value, valueType);
        return where;
    }
}

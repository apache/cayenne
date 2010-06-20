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

import java.util.ArrayList;

public class UpdateBuilder extends SQLBuilder {

    protected int setCount;

    protected UpdateBuilder(DBHelper dbHelper, String tableName) {
        super(dbHelper, new StringBuilder(), new ArrayList<Object>());
        sqlBuffer.append("update ").append(dbHelper.quote(tableName)).append(" set ");
    }

    public UpdateBuilder set(String column, Object value) {
        if (setCount++ > 0) {
            sqlBuffer.append(", ");
        }

        sqlBuffer.append(dbHelper.quote(column)).append(" = ?");
        bindings.add(value);
        return this;
    }

    public WhereBuilder where(String column, Object value) {
        WhereBuilder where = new WhereBuilder(dbHelper, sqlBuffer, bindings);
        where.and(column, value);
        return where;
    }

}

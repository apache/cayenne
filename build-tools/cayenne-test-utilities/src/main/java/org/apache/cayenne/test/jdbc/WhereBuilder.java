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

import java.util.Collection;

public class WhereBuilder extends SQLBuilder {

    protected int whereCount;

    protected WhereBuilder(DBHelper dbHelper, StringBuilder sqlBuffer,
            Collection<Object> bindings, Collection<Integer> bindingTypes) {
        super(dbHelper, sqlBuffer, bindings, bindingTypes);
        sqlBuffer.append(" where ");
    }

    public WhereBuilder and(String column, Object value) {
        return and(column, value, NO_TYPE);
    }

    public WhereBuilder and(String column, Object value, int valueType) {

        if (whereCount++ > 0) {
            sqlBuffer.append(" and ");
        }

        sqlBuffer.append(dbHelper.quote(column)).append(" = ?");
        initBinding(value, valueType);

        return this;
    }

    public WhereBuilder or(String column, Object value) {
        return or(column, value, NO_TYPE);
    }

    public WhereBuilder or(String column, Object value, int valueType) {
        if (whereCount++ > 0) {
            sqlBuffer.append(" or ");
        }

        sqlBuffer.append(dbHelper.quote(column)).append(" = ?");
        initBinding(value, valueType);

        return this;
    }

}

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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

public abstract class SQLBuilder {

    static final int NO_TYPE = Integer.MIN_VALUE;

    protected DBHelper dbHelper;
    protected Collection<Object> bindings;
    protected Collection<Integer> bindingTypes;
    protected StringBuilder sqlBuffer;

    protected SQLBuilder(DBHelper dbHelper) {
        this(
                dbHelper,
                new StringBuilder(),
                new ArrayList<Object>(),
                new ArrayList<Integer>());
    }

    protected SQLBuilder(DBHelper dbHelper, StringBuilder sqlBuffer,
            Collection<Object> bindings, Collection<Integer> bindingTypes) {
        this.dbHelper = dbHelper;
        this.bindings = bindings;
        this.bindingTypes = bindingTypes;
        this.sqlBuffer = sqlBuffer;
    }

    public int execute() throws SQLException {
        return new UpdateTemplate(dbHelper).execute(
                sqlBuffer.toString(),
                bindings,
                bindingTypes);
    }

    protected void initBinding(Object value, int type) {
        bindings.add(value);
        bindingTypes.add(type);
    }
}

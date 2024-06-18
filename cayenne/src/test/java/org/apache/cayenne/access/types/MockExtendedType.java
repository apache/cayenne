/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access.types;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class MockExtendedType implements ExtendedType<Object> {

    protected Class<?> objectClass;

    public MockExtendedType() {
        this(Object.class);
    }

    public MockExtendedType(Class<?> objectClass) {
        this.objectClass = objectClass;
    }

    @Override
    public String getClassName() {
        return objectClass.getName();
    }

    @Override
    public void setJdbcObject(
            PreparedStatement statement,
            Object value,
            int pos,
            int type,
            int precision) throws Exception {
    }

    @Override
    public Object materializeObject(ResultSet rs, int index, int type) throws Exception {
        return objectClass.getDeclaredConstructor().newInstance();
    }

    @Override
    public Object materializeObject(CallableStatement rs, int index, int type)
            throws Exception {
        return objectClass.getDeclaredConstructor().newInstance();
    }

    @Override
    public String toString(Object value) {
        if (value == null) {
            return "NULL";
        }

        return value.toString();
    }
}

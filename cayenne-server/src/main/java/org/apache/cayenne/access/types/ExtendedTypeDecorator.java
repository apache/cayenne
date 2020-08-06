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

/**
 * Decorates another ExtendedType, converting objects to other Java types.
 * 
 * @since 3.0
 */
abstract class ExtendedTypeDecorator<T, E> implements ExtendedType<T> {

    private ExtendedType<E> decorated;

    ExtendedTypeDecorator(ExtendedType<E> decorated) {
        this.decorated = decorated;
    }

    abstract T toJavaObject(E object);

    abstract E fromJavaObject(T object);

    @Override
    public abstract String getClassName();

    @Override
    public T materializeObject(CallableStatement rs, int index, int type)
            throws Exception {
        return toJavaObject(decorated.materializeObject(rs, index, type));
    }

    @Override
    public T materializeObject(ResultSet rs, int index, int type) throws Exception {
        return toJavaObject(decorated.materializeObject(rs, index, type));
    }

    @Override
    public void setJdbcObject(
            PreparedStatement statement,
            T value,
            int pos,
            int type,
            int precision) throws Exception {
        decorated.setJdbcObject(statement, fromJavaObject(value), pos, type, precision);
    }

    @Override
    public String toString(T value) {
        if (value == null) {
            return "NULL";
        }

        return value.toString();
    }
}

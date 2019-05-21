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

import org.apache.cayenne.ExtendedEnumeration;
import org.apache.cayenne.dba.TypesMapping;

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * An ExtendedType that handles an enum class. If Enum is mapped to a character column,
 * its name is used as persistent value; if it is mapped to a numeric column, its ordinal
 * (i.e. a position in enum class) is used.
 * 
 * @since 1.2
 */
public class EnumType<T extends Enum<T>> implements ExtendedType<T> {

    protected Class<T> enumClass;
    protected T[] values;
    protected String canonicalName;

    public EnumType(Class<T> enumClass) {
        if (enumClass == null) {
            throw new IllegalArgumentException("Null enum class");
        }

        this.enumClass = enumClass;
        this.canonicalName = enumClass.getCanonicalName();

        try {
            Method m = enumClass.getMethod("values");
            this.values = (T[]) m.invoke(null);
        } catch (Exception e) {
            throw new IllegalArgumentException("Class " + enumClass.getName() + " is not an Enum", e);
        }
    }

    @Override
    public String getClassName() {
        return canonicalName;
    }

    @Override
    public void setJdbcObject(
            PreparedStatement statement,
            T value,
            int pos,
            int type,
            int precision) throws Exception {

        if (value != null) {
            if (TypesMapping.isNumeric(type)) {
                statement.setInt(pos, value.ordinal());
            } else {
                statement.setString(pos, value.name());
            }
        } else {
            statement.setNull(pos, type);
        }
    }

    @Override
    public T materializeObject(ResultSet rs, int index, int type) throws Exception {
        if (TypesMapping.isNumeric(type)) {
            int i = rs.getInt(index);
            return (rs.wasNull() || index < 0) ? null : values[i];
        } else {
            String string = rs.getString(index);
            return string != null ? Enum.valueOf(enumClass, string) : null;
        }
    }

    @Override
    public T materializeObject(CallableStatement rs, int index, int type) throws Exception {
        if (TypesMapping.isNumeric(type)) {
            int i = rs.getInt(index);
            return (rs.wasNull() || index < 0) ? null : values[i];
        } else {
            String string = rs.getString(index);
            return string != null ? Enum.valueOf(enumClass, string) : null;
        }
    }

    @Override
    public String toString(T value) {
        if (value == null) {
            return "NULL";
        }

        StringBuilder buffer = new StringBuilder();
        buffer.append(value.name());
        if (value instanceof ExtendedEnumeration) {
            buffer.append("=");
            Object dbValue = ((ExtendedEnumeration) value).getDatabaseValue();
            if (dbValue instanceof String) {
                buffer.append("'");
            }
            buffer.append(value);
            if (dbValue instanceof String) {
                buffer.append("'");
            }
        }

        return buffer.toString();
    }
}

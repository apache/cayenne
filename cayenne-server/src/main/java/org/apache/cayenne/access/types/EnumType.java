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

package org.apache.cayenne.access.types;

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.cayenne.dba.TypesMapping;

/**
 * An ExtendedType that handles an enum class. If Enum is mapped to a character column,
 * its name is used as persistent value; if it is mapped to a numeric column, its ordinal
 * (i.e. a position in enum class) is used.
 * <p>
 * <i>Requires Java 1.5 or newer</i>
 * </p>
 * 
 * @since 1.2
 */
public class EnumType<T extends Enum<T>> implements ExtendedType {

    protected Class<T> enumClass;
    protected Object[] values;

    public EnumType(Class<T> enumClass) {
        if (enumClass == null) {
            throw new IllegalArgumentException("Null enum class");
        }

        this.enumClass = enumClass;

        try {
            Method m = enumClass.getMethod("values");
            this.values = (Object[]) m.invoke(null);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Class "
                    + enumClass.getName()
                    + " is not an Enum", e);
        }
    }

    @Override
    public String getClassName() {
        return enumClass.getName();
    }

    @Override
    public void setJdbcObject(
            PreparedStatement statement,
            Object value,
            int pos,
            int type,
            int precision) throws Exception {

        if (value instanceof Enum<?>) {

            Enum<?> e = (Enum<?>) value;

            if (TypesMapping.isNumeric(type)) {
                statement.setInt(pos, e.ordinal());
            }
            else {
                statement.setString(pos, e.name());
            }
        }
        else {
            statement.setNull(pos, type);
        }
    }

    @Override
    public Object materializeObject(ResultSet rs, int index, int type) throws Exception {
        if (TypesMapping.isNumeric(type)) {
            int i = rs.getInt(index);
            return (rs.wasNull() || index < 0) ? null : values[i];
        }
        else {
            String string = rs.getString(index);
            return string != null ? Enum.valueOf(enumClass, string) : null;
        }
    }

    @Override
    public Object materializeObject(CallableStatement rs, int index, int type)
            throws Exception {

        if (TypesMapping.isNumeric(type)) {
            int i = rs.getInt(index);
            return (rs.wasNull() || index < 0) ? null : values[i];
        }
        else {
            String string = rs.getString(index);
            return string != null ? Enum.valueOf(enumClass, string) : null;
        }
    }
}

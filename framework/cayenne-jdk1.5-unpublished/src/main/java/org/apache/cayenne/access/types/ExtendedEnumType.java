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
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ExtendedEnumeration;
import org.apache.cayenne.dba.TypesMapping;

/**
 * An ExtendedType that handles a Java Enum based upon the Cayenne ExtendedEnumeration
 * interface. The ExtendedEnumeration interface requires the developer to specify the
 * database values for the Enum being mapped. This ExtendedType is used to auto-register
 * those Enums found in the model.
 * 
 * @since 3.0
 */
public class ExtendedEnumType<T extends Enum<T>> implements ExtendedType {

    private Class<T> enumerationClass = null;
    private Object[] values = null;

    // Contains a mapping of database values (Integer or String) and the
    // Enum for that value. This is to facilitate mapping database values
    // back to the Enum upon reading them from the database.
    private Map<Object, Enum<T>> enumerationMappings = new HashMap<Object, Enum<T>>();

    public ExtendedEnumType(Class<T> enumerationClass) {
        if (enumerationClass == null)
            throw new IllegalArgumentException("Null ExtendedEnumType class");

        this.enumerationClass = enumerationClass;

        try {
            Method m = enumerationClass.getMethod("values");

            values = (Object[]) m.invoke(null);

            for (int i = 0; i < values.length; i++)
                register((Enum<T>) values[i], ((ExtendedEnumeration) values[i])
                        .getDatabaseValue());

        }
        catch (Exception e) {
            throw new IllegalArgumentException("Class "
                    + enumerationClass.getName()
                    + " is not an Enum", e);
        }
    }

    public String getClassName() {
        return enumerationClass.getName();
    }

    public Object materializeObject(ResultSet rs, int index, int type) throws Exception {
        if (TypesMapping.isNumeric(type)) {
            int i = rs.getInt(index);
            return (rs.wasNull() || index < 0) ? null : lookup(i);
        }
        else {
            String string = rs.getString(index);
            return string != null ? lookup(string) : null;
        }
    }

    public Object materializeObject(CallableStatement rs, int index, int type)
            throws Exception {
        if (TypesMapping.isNumeric(type)) {
            int i = rs.getInt(index);
            return (rs.wasNull() || index < 0) ? null : lookup(i);
        }
        else {
            String string = rs.getString(index);
            return string != null ? lookup(string) : null;
        }
    }

    public void setJdbcObject(
            PreparedStatement statement,
            Object value,
            int pos,
            int type,
            int precision) throws Exception {
        if (value instanceof ExtendedEnumeration) {
            ExtendedEnumeration e = (ExtendedEnumeration) value;

            if (TypesMapping.isNumeric(type))
                statement.setInt(pos, (Integer) e.getDatabaseValue());
            else
                statement.setString(pos, (String) e.getDatabaseValue());
        }
        else {
            statement.setNull(pos, type);
        }
    }

    /**
     * Register the given enum with the mapped database value.
     */
    private void register(Enum<T> enumeration, Object databaseValue) {
        // Check for duplicates.
        if (enumerationMappings.containsKey(databaseValue)
                || enumerationMappings.containsValue(enumeration))
            throw new CayenneRuntimeException(
                    "Enumerations/values may not be duplicated.");

        // Store by database value/enum because we have to lookup by db value later.
        enumerationMappings.put(databaseValue, enumeration);
    }

    /**
     * Lookup the giving database value and return the matching enum.
     */
    private Enum<T> lookup(Object databaseValue) {
        if (enumerationMappings.containsKey(databaseValue) == false) {
            // All integers enums are mapped. Not necessarily all strings.
            if (databaseValue instanceof Integer)
                throw new CayenneRuntimeException("Missing enumeration mapping for "
                        + getClassName()
                        + " with value "
                        + databaseValue
                        + ".");

            // Use the database value (a String) as the enum value.
            return Enum.valueOf(enumerationClass, (String) databaseValue);
        }

        // Mapped value->enum exists, return it.
        return enumerationMappings.get(databaseValue);
    }

    /**
     * Returns the enumeration mapping for this enumerated data type. The key is the
     * database value, the value is the actual enum.
     */
    public Map<Object, Enum<T>> getEnumerationMappings() {
        return enumerationMappings;
    }

    public Object[] getValues() {
        return values;
    }
}

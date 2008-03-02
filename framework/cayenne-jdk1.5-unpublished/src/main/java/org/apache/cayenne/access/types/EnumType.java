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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ExtendedEnumeration;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.validation.ValidationResult;

/**
 * An ExtendedType that handles an enum class. If Enum is an instance of
 * ExtendedEnumeration (preferred), use the mapped database value (provided
 * by the getDatabaseValue() method) to map enumerations to the database.  If
 * the enum is not an instance of ExtendedEnumeration, fall back to a more
 * simplistic mapping.  If mapped to a character column, the name is used as
 * the persistent value; if it is mapped to a numeric column, the ordinal
 * value (i.e. a position in enum class) is used.
 * <p>
 * <i>Requires Java 1.5 or newer</i>
 * </p>
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class EnumType<T extends Enum<T>> implements ExtendedType {

    protected Class<T> enumClass;
//    protected Object[] values;
    
    // Contains a mapping of database values (Integer or String) and the Enum for that value.
    private Map<Object, Enum<T>> enumerationMappings = new HashMap<Object, Enum<T>>();

    public EnumType(Class<T> enumClass) {
        if (enumClass == null) {
            throw new IllegalArgumentException("Null enum class");
        }

        this.enumClass = enumClass;

        try {
            Method m = enumClass.getMethod("values");
            Object[] values = (Object[]) m.invoke(null);

            for (int i = 0; i < values.length; i++)
                if (values[i] instanceof ExtendedEnumeration)
                    register((Enum<T>) values[i], ((ExtendedEnumeration) values[i]).getDatabaseValue());
                else
                    register((Enum<T>) values[i], i);
                
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Class "
                    + enumClass.getName()
                    + " is not an Enum", e);
        }
    }

    public String getClassName() {
        return enumClass.getName();
    }

    /**
     * @deprecated since 3.0 as validation should not be done at the DataNode level.
     */
    public boolean validateProperty(
            Object source,
            String property,
            Object value,
            DbAttribute dbAttribute,
            ValidationResult validationResult) {

        return AbstractType.validateNull(
                source,
                property,
                value,
                dbAttribute,
                validationResult);
    }

    public void setJdbcObject(
            PreparedStatement statement,
            Object value,
            int pos,
            int type,
            int precision) throws Exception {

        if (value instanceof Enum) {

            Enum<?> e = (Enum<?>) value;

            if (TypesMapping.isNumeric(type)) {
                if (e instanceof ExtendedEnumeration)
                    statement.setInt(pos, (Integer) ((ExtendedEnumeration) e).getDatabaseValue());
                else
                    statement.setInt(pos, e.ordinal());
            }
            else {
                if (e instanceof ExtendedEnumeration)
                    statement.setString(pos, (String) ((ExtendedEnumeration) e).getDatabaseValue());
                else
                    statement.setString(pos, e.name());
            }
        }
        else {
            statement.setNull(pos, type);
        }
    }

    public Object materializeObject(ResultSet rs, int index, int type) throws Exception {
        if (TypesMapping.isNumeric(type)) {
            int i = rs.getInt(index);
            return (rs.wasNull() || index < 0) ? null : lookup(i);
//            return (rs.wasNull() || index < 0) ? null : values[i];
        }
        else {
            String string = rs.getString(index);
            return string != null ? lookup(string) : null;
//            return string != null ? Enum.valueOf(enumClass, string) : null;
        }
    }

    public Object materializeObject(CallableStatement rs, int index, int type)
            throws Exception {

        if (TypesMapping.isNumeric(type)) {
            int i = rs.getInt(index);
            return (rs.wasNull() || index < 0) ? null : lookup(i);
//            return (rs.wasNull() || index < 0) ? null : values[i];
        }
        else {
            String string = rs.getString(index);
            return string != null ? lookup(string) : null;
//            return string != null ? Enum.valueOf(enumClass, string) : null;
        }
    }

    /**
     * Register the given enum with the mapped database value.
     */
    private void register(Enum<T> enumeration, Object databaseValue)
    {
      // Check for duplicates.
      if (enumerationMappings.containsKey(databaseValue) || enumerationMappings.containsValue(enumeration))
          throw new CayenneRuntimeException("Enumerations/values may not be duplicated.");

      // Store by database value/enum because we have to lookup by db value later.
      enumerationMappings.put(databaseValue, enumeration);
    }

    /**
     * Lookup the giving database value and return the matching enum.
     */
    private Enum<T> lookup(Object databaseValue)
    {
      if (enumerationMappings.containsKey(databaseValue) == false)
      {
          // All integers enums are mapped.  Not necessarily all strings.
          if (databaseValue instanceof Integer)
              throw new CayenneRuntimeException("Missing enumeration mapping for " + getClassName() + " with value " + databaseValue + ".");

          // Use the database value (a String) as the enum value.
          return Enum.valueOf(enumClass, (String) databaseValue);
      }

      // Mapped value->enum exists, return it.
      return enumerationMappings.get(databaseValue);
    }

    
    /**
     * Returns the enumeration mapping for this enumerated data type.  The
     * key is the database value, the value is the actual enum.
     */
    public Map<Object, Enum<T>> getEnumerationMappings() {
        return enumerationMappings;
    }
    
    /**
     * Returns the values registered for this enumerated data type.  Note that
     * the order the values are returned in could differ from the ordinal order
     * in which they are declared in the actual enum class.
     */
    public Collection<Enum<T>> values() {
        return enumerationMappings.values();
    }
}

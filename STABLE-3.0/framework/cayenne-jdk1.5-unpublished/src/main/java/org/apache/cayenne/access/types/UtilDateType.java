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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Date;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.validation.ValidationResult;

/**
 * Maps <code>java.util.Date</code> to any of the three database date/time types: TIME,
 * DATE, TIMESTAMP.
 */
public class UtilDateType implements ExtendedType {

    /**
     * Returns "java.util.Date".
     */
    public String getClassName() {
        return Date.class.getName();
    }

    /**
     * Always returns true indicating no validation failures. There is no date-specific
     * validations at the moment.
     * 
     * @since 1.1
     * @deprecated since 3.0 as validation should not be done at the DataNode level.
     */
    public boolean validateProperty(
            Object source,
            String property,
            Object value,
            DbAttribute dbAttribute,
            ValidationResult validationResult) {
        return true;
    }

    protected Object convertToJdbcObject(Object val, int type) throws Exception {
        if (type == Types.DATE)
            return new java.sql.Date(((Date) val).getTime());
        else if (type == Types.TIME)
            return new java.sql.Time(((Date) val).getTime());
        else if (type == Types.TIMESTAMP)
            return new java.sql.Timestamp(((Date) val).getTime());
        else
            throw new IllegalArgumentException(
                    "Only DATE, TIME or TIMESTAMP can be mapped as '"
                            + getClassName()
                            + "', got "
                            + TypesMapping.getSqlNameByType(type));
    }

    public Object materializeObject(ResultSet rs, int index, int type) throws Exception {
        Date val = null;

        switch (type) {
            case Types.TIMESTAMP:
                val = rs.getTimestamp(index);
                break;
            case Types.DATE:
                val = rs.getDate(index);
                break;
            case Types.TIME:
                val = rs.getTime(index);
                break;
            default:
                // here the driver can "surprise" us
                // check the type of returned value...
                Object object = rs.getObject(index);

                if (object != null && !(object instanceof Date)) {
                    throw new CayenneRuntimeException(
                            "Expected an instance of java.util.Date, instead got "
                                    + object.getClass().getName()
                                    + ", column index: "
                                    + index);
                }

                val = (Date) object;
                break;
        }

        return val == null ? null : new Date(val.getTime());
    }

    public Object materializeObject(CallableStatement cs, int index, int type)
            throws Exception {
        Object val = null;

        switch (type) {
            case Types.TIMESTAMP:
                val = cs.getTimestamp(index);
                break;
            case Types.DATE:
                val = cs.getDate(index);
                break;
            case Types.TIME:
                val = cs.getTime(index);
                break;
            default:
                val = cs.getObject(index);
                // check if value was properly converted by the driver
                if (val != null && !(val instanceof java.util.Date)) {
                    String typeName = TypesMapping.getSqlNameByType(type);
                    throw new ClassCastException(
                            "Expected a java.util.Date or subclass, instead fetched '"
                                    + val.getClass().getName()
                                    + "' for JDBC type "
                                    + typeName);
                }
                break;
        }

        // all sql time/date classes are subclasses of java.util.Date,
        // so lets cast it to Date,
        // if it is not date, ClassCastException will be thrown,
        // which is what we want
        return val == null ? null : new java.util.Date(((java.util.Date) val).getTime());
    }

    public void setJdbcObject(
            PreparedStatement statement,
            Object value,
            int pos,
            int type,
            int scale) throws Exception {

        if (value == null) {
            statement.setNull(pos, type);
        }
        else {
            statement.setObject(pos, convertToJdbcObject(value, type), type);
        }
    }
}

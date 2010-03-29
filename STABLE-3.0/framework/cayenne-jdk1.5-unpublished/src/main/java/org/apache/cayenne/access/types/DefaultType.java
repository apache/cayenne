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
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.dba.TypesMapping;

/**
 * An ExtendedType that can work with any Java class, providing JDBC-to-Java mapping
 * exactly per JDBC specification.
 * 
 * @deprecated since 3.0, as explicit type mappings are created for each JDBC spec type.
 */
public class DefaultType extends AbstractType {

    private static final Map<String, Method> readMethods = new HashMap<String, Method>();
    private static final Map<String, Method> procReadMethods = new HashMap<String, Method>();
    private static Method readObjectMethod;
    private static Method procReadObjectMethod;

    static {
        try {
            Class<?> rsClass = ResultSet.class;
            Class<?>[] paramTypes = new Class[] {
                Integer.TYPE
            };
            readMethods.put(TypesMapping.JAVA_LONG, rsClass.getMethod(
                    "getLong",
                    paramTypes));
            readMethods.put(TypesMapping.JAVA_BIGDECIMAL, rsClass.getMethod(
                    "getBigDecimal",
                    paramTypes));
            readMethods.put(TypesMapping.JAVA_BOOLEAN, rsClass.getMethod(
                    "getBoolean",
                    paramTypes));
            readMethods.put(TypesMapping.JAVA_BYTE, rsClass.getMethod(
                    "getByte",
                    paramTypes));
            readMethods.put(TypesMapping.JAVA_BYTES, rsClass.getMethod(
                    "getBytes",
                    paramTypes));
            readMethods.put(TypesMapping.JAVA_SQLDATE, rsClass.getMethod(
                    "getDate",
                    paramTypes));
            readMethods.put(TypesMapping.JAVA_DOUBLE, rsClass.getMethod(
                    "getDouble",
                    paramTypes));
            readMethods.put(TypesMapping.JAVA_FLOAT, rsClass.getMethod(
                    "getFloat",
                    paramTypes));
            readMethods.put(TypesMapping.JAVA_INTEGER, rsClass.getMethod(
                    "getInt",
                    paramTypes));
            readMethods.put(TypesMapping.JAVA_SHORT, rsClass.getMethod(
                    "getShort",
                    paramTypes));
            readMethods.put(TypesMapping.JAVA_STRING, rsClass.getMethod(
                    "getString",
                    paramTypes));
            readMethods.put(TypesMapping.JAVA_TIME, rsClass.getMethod(
                    "getTime",
                    paramTypes));
            readMethods.put(TypesMapping.JAVA_TIMESTAMP, rsClass.getMethod(
                    "getTimestamp",
                    paramTypes));
            readMethods.put(TypesMapping.JAVA_BLOB, rsClass.getMethod(
                    "getBlob",
                    paramTypes));

            readObjectMethod = rsClass.getMethod("getObject", paramTypes);

            // init procedure read methods
            Class<?> csClass = CallableStatement.class;
            procReadMethods.put(TypesMapping.JAVA_LONG, csClass.getMethod(
                    "getLong",
                    paramTypes));
            procReadMethods.put(TypesMapping.JAVA_BIGDECIMAL, csClass.getMethod(
                    "getBigDecimal",
                    paramTypes));
            procReadMethods.put(TypesMapping.JAVA_BOOLEAN, csClass.getMethod(
                    "getBoolean",
                    paramTypes));
            procReadMethods.put(TypesMapping.JAVA_BYTE, csClass.getMethod(
                    "getByte",
                    paramTypes));
            procReadMethods.put(TypesMapping.JAVA_BYTES, csClass.getMethod(
                    "getBytes",
                    paramTypes));
            procReadMethods.put(TypesMapping.JAVA_SQLDATE, csClass.getMethod(
                    "getDate",
                    paramTypes));
            procReadMethods.put(TypesMapping.JAVA_DOUBLE, csClass.getMethod(
                    "getDouble",
                    paramTypes));
            procReadMethods.put(TypesMapping.JAVA_FLOAT, csClass.getMethod(
                    "getFloat",
                    paramTypes));
            procReadMethods.put(TypesMapping.JAVA_INTEGER, csClass.getMethod(
                    "getInt",
                    paramTypes));
            procReadMethods.put(TypesMapping.JAVA_SHORT, csClass.getMethod(
                    "getShort",
                    paramTypes));
            procReadMethods.put(TypesMapping.JAVA_STRING, csClass.getMethod(
                    "getString",
                    paramTypes));
            procReadMethods.put(TypesMapping.JAVA_TIME, csClass.getMethod(
                    "getTime",
                    paramTypes));
            procReadMethods.put(TypesMapping.JAVA_TIMESTAMP, csClass.getMethod(
                    "getTimestamp",
                    paramTypes));
            procReadMethods.put(TypesMapping.JAVA_BLOB, csClass.getMethod(
                    "getBlob",
                    paramTypes));

            procReadObjectMethod = csClass.getMethod("getObject", paramTypes);
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error initializing read methods.", ex);
        }
    }

    /**
     * Returns an Iterator over the names of supported default Java classes.
     */
    public static Iterator<String> defaultTypes() {
        return readMethods.keySet().iterator();
    }

    protected String className;
    protected Method readMethod;
    protected Method procReadMethod;

    /**
     * Creates DefaultType to read objects from ResultSet using "getObject" method.
     */
    public DefaultType() {
        this.className = Object.class.getName();
        this.readMethod = readObjectMethod;
        this.procReadMethod = procReadObjectMethod;
    }

    public DefaultType(String className) {
        this.className = className;
        this.readMethod = readMethods.get(className);

        if (readMethod == null) {
            throw new CayenneRuntimeException("Unsupported default class: "
                    + className
                    + ". If you want a non-standard class to map to JDBC type,"
                    + " you will need to implement ExtendedType interface yourself.");
        }

        this.procReadMethod = procReadMethods.get(className);
        if (procReadMethod == null) {
            throw new CayenneRuntimeException("Unsupported default class: "
                    + className
                    + ". If you want a non-standard class to map to JDBC type,"
                    + " you will need to implement ExtendedType interface yourself.");
        }
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public Object materializeObject(ResultSet rs, int index, int type) throws Exception {
        Object val = readMethod.invoke(rs, Integer.valueOf(index));
        return (rs.wasNull()) ? null : val;
    }

    @Override
    public Object materializeObject(CallableStatement st, int index, int type)
            throws Exception {
        Object val = procReadMethod.invoke(st, Integer.valueOf(index));
        return (st.wasNull()) ? null : val;
    }
}

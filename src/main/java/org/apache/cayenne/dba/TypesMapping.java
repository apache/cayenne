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

package org.apache.cayenne.dba;

import java.io.Serializable;
import java.math.BigInteger;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.util.Util;

/**
 * A utility class that handles mappings of JDBC data types to the database types and Java
 * types. Also contains methods that provide information about JDBC types.
 * 
 * @author Michael Shengaout
 * @author Andrus Adamchik
 */
public class TypesMapping {

    // Never use "-1" or any other normal integer, since there
    // is a big chance it is being reserved in java.sql.Types
    public static final int NOT_DEFINED = Integer.MAX_VALUE;

    // char constants for the sql data types
    public static final String SQL_ARRAY = "ARRAY";
    public static final String SQL_BIGINT = "BIGINT";
    public static final String SQL_BINARY = "BINARY";
    public static final String SQL_BIT = "BIT";
    public static final String SQL_BLOB = "BLOB";

    /**
     * @since 1.2
     */
    public static final String SQL_BOOLEAN = "BOOLEAN";

    public static final String SQL_CLOB = "CLOB";
    public static final String SQL_CHAR = "CHAR";
    public static final String SQL_DATE = "DATE";
    public static final String SQL_DECIMAL = "DECIMAL";
    public static final String SQL_DOUBLE = "DOUBLE";
    public static final String SQL_FLOAT = "FLOAT";
    public static final String SQL_INTEGER = "INTEGER";
    public static final String SQL_LONGVARCHAR = "LONGVARCHAR";
    public static final String SQL_LONGVARBINARY = "LONGVARBINARY";
    public static final String SQL_NUMERIC = "NUMERIC";
    public static final String SQL_REAL = "REAL";
    public static final String SQL_SMALLINT = "SMALLINT";
    public static final String SQL_TINYINT = "TINYINT";
    public static final String SQL_TIME = "TIME";
    public static final String SQL_TIMESTAMP = "TIMESTAMP";
    public static final String SQL_VARBINARY = "VARBINARY";
    public static final String SQL_VARCHAR = "VARCHAR";
    public static final String SQL_OTHER = "OTHER";

    // char constants for Java data types
    public static final String JAVA_LONG = "java.lang.Long";
    public static final String JAVA_BYTES = "byte[]";
    public static final String JAVA_BOOLEAN = "java.lang.Boolean";
    public static final String JAVA_STRING = "java.lang.String";
    public static final String JAVA_SQLDATE = "java.sql.Date";
    public static final String JAVA_UTILDATE = "java.util.Date";
    public static final String JAVA_BIGDECIMAL = "java.math.BigDecimal";
    public static final String JAVA_DOUBLE = "java.lang.Double";
    public static final String JAVA_FLOAT = "java.lang.Float";
    public static final String JAVA_INTEGER = "java.lang.Integer";
    public static final String JAVA_SHORT = "java.lang.Short";
    public static final String JAVA_BYTE = "java.lang.Byte";
    public static final String JAVA_TIME = "java.sql.Time";
    public static final String JAVA_TIMESTAMP = "java.sql.Timestamp";
    public static final String JAVA_BLOB = "java.sql.Blob";

    /**
     * Keys: SQL string type names, Values: SQL int type definitions from java.sql.Types
     */
    private static final Map<String, Integer> sqlStringType = new HashMap<String, Integer>();

    /**
     * Keys: SQL int type definitions from java.sql.Types, Values: SQL string type names
     */
    private static final Map<Integer, String> sqlEnumType = new HashMap<Integer, String>();

    /**
     * Keys: SQL int type definitions from java.sql.Types, Values: java class names
     */
    private static final Map<Integer, String> sqlEnumJava = new HashMap<Integer, String>();

    /**
     * Keys: java class names, Values: SQL int type definitions from java.sql.Types
     */
    private static final Map<String, Integer> javaSqlEnum = new HashMap<String, Integer>();

    static {
        sqlStringType.put(SQL_ARRAY, new Integer(Types.ARRAY));
        sqlStringType.put(SQL_BIGINT, new Integer(Types.BIGINT));
        sqlStringType.put(SQL_BINARY, new Integer(Types.BINARY));
        sqlStringType.put(SQL_BIT, new Integer(Types.BIT));
        sqlStringType.put(SQL_BLOB, new Integer(Types.BLOB));
        sqlStringType.put(SQL_BOOLEAN, new Integer(Types.BOOLEAN));
        sqlStringType.put(SQL_CLOB, new Integer(Types.CLOB));
        sqlStringType.put(SQL_CHAR, new Integer(Types.CHAR));
        sqlStringType.put(SQL_DATE, new Integer(Types.DATE));
        sqlStringType.put(SQL_DECIMAL, new Integer(Types.DECIMAL));
        sqlStringType.put(SQL_DOUBLE, new Integer(Types.DOUBLE));
        sqlStringType.put(SQL_FLOAT, new Integer(Types.FLOAT));
        sqlStringType.put(SQL_INTEGER, new Integer(Types.INTEGER));
        sqlStringType.put(SQL_LONGVARCHAR, new Integer(Types.LONGVARCHAR));
        sqlStringType.put(SQL_LONGVARBINARY, new Integer(Types.LONGVARBINARY));
        sqlStringType.put(SQL_NUMERIC, new Integer(Types.NUMERIC));
        sqlStringType.put(SQL_REAL, new Integer(Types.REAL));
        sqlStringType.put(SQL_SMALLINT, new Integer(Types.SMALLINT));
        sqlStringType.put(SQL_TINYINT, new Integer(Types.TINYINT));
        sqlStringType.put(SQL_TIME, new Integer(Types.TIME));
        sqlStringType.put(SQL_TIMESTAMP, new Integer(Types.TIMESTAMP));
        sqlStringType.put(SQL_VARBINARY, new Integer(Types.VARBINARY));
        sqlStringType.put(SQL_VARCHAR, new Integer(Types.VARCHAR));
        sqlStringType.put(SQL_OTHER, new Integer(Types.OTHER));

        sqlEnumType.put(new Integer(Types.ARRAY), SQL_ARRAY);
        sqlEnumType.put(new Integer(Types.BIGINT), SQL_BIGINT);
        sqlEnumType.put(new Integer(Types.BINARY), SQL_BINARY);
        sqlEnumType.put(new Integer(Types.BIT), SQL_BIT);
        sqlEnumType.put(new Integer(Types.BOOLEAN), SQL_BOOLEAN);
        sqlEnumType.put(new Integer(Types.BLOB), SQL_BLOB);
        sqlEnumType.put(new Integer(Types.CLOB), SQL_CLOB);
        sqlEnumType.put(new Integer(Types.CHAR), SQL_CHAR);
        sqlEnumType.put(new Integer(Types.DATE), SQL_DATE);
        sqlEnumType.put(new Integer(Types.DECIMAL), SQL_DECIMAL);
        sqlEnumType.put(new Integer(Types.DOUBLE), SQL_DOUBLE);
        sqlEnumType.put(new Integer(Types.FLOAT), SQL_FLOAT);
        sqlEnumType.put(new Integer(Types.INTEGER), SQL_INTEGER);
        sqlEnumType.put(new Integer(Types.LONGVARCHAR), SQL_LONGVARCHAR);
        sqlEnumType.put(new Integer(Types.LONGVARBINARY), SQL_LONGVARBINARY);
        sqlEnumType.put(new Integer(Types.NUMERIC), SQL_NUMERIC);
        sqlEnumType.put(new Integer(Types.REAL), SQL_REAL);
        sqlEnumType.put(new Integer(Types.SMALLINT), SQL_SMALLINT);
        sqlEnumType.put(new Integer(Types.TINYINT), SQL_TINYINT);
        sqlEnumType.put(new Integer(Types.TIME), SQL_TIME);
        sqlEnumType.put(new Integer(Types.TIMESTAMP), SQL_TIMESTAMP);
        sqlEnumType.put(new Integer(Types.VARBINARY), SQL_VARBINARY);
        sqlEnumType.put(new Integer(Types.VARCHAR), SQL_VARCHAR);
        sqlEnumType.put(new Integer(Types.OTHER), SQL_OTHER);

        sqlEnumJava.put(new Integer(Types.BIGINT), JAVA_LONG);
        sqlEnumJava.put(new Integer(Types.BINARY), JAVA_BYTES);
        sqlEnumJava.put(new Integer(Types.BIT), JAVA_BOOLEAN);
        sqlEnumJava.put(new Integer(Types.BOOLEAN), JAVA_BOOLEAN);
        sqlEnumJava.put(new Integer(Types.BLOB), JAVA_BYTES);
        sqlEnumJava.put(new Integer(Types.CLOB), JAVA_STRING);
        sqlEnumJava.put(new Integer(Types.CHAR), JAVA_STRING);
        sqlEnumJava.put(new Integer(Types.DATE), JAVA_UTILDATE);
        sqlEnumJava.put(new Integer(Types.DECIMAL), JAVA_BIGDECIMAL);
        sqlEnumJava.put(new Integer(Types.DOUBLE), JAVA_DOUBLE);
        sqlEnumJava.put(new Integer(Types.FLOAT), JAVA_FLOAT);
        sqlEnumJava.put(new Integer(Types.INTEGER), JAVA_INTEGER);
        sqlEnumJava.put(new Integer(Types.LONGVARCHAR), JAVA_STRING);
        sqlEnumJava.put(new Integer(Types.LONGVARBINARY), JAVA_BYTES);
        sqlEnumJava.put(new Integer(Types.NUMERIC), JAVA_BIGDECIMAL);
        sqlEnumJava.put(new Integer(Types.REAL), JAVA_FLOAT);
        sqlEnumJava.put(new Integer(Types.SMALLINT), JAVA_SHORT);
        sqlEnumJava.put(new Integer(Types.TINYINT), JAVA_BYTE);
        sqlEnumJava.put(new Integer(Types.TIME), JAVA_UTILDATE);
        sqlEnumJava.put(new Integer(Types.TIMESTAMP), JAVA_UTILDATE);
        sqlEnumJava.put(new Integer(Types.VARBINARY), JAVA_BYTES);
        sqlEnumJava.put(new Integer(Types.VARCHAR), JAVA_STRING);

        javaSqlEnum.put(JAVA_LONG, new Integer(Types.BIGINT));
        javaSqlEnum.put(JAVA_BYTES, new Integer(Types.BINARY));
        javaSqlEnum.put(JAVA_BOOLEAN, new Integer(Types.BIT));
        javaSqlEnum.put(JAVA_STRING, new Integer(Types.VARCHAR));
        javaSqlEnum.put(JAVA_SQLDATE, new Integer(Types.DATE));
        javaSqlEnum.put(JAVA_TIMESTAMP, new Integer(Types.TIMESTAMP));
        javaSqlEnum.put(JAVA_BIGDECIMAL, new Integer(Types.DECIMAL));
        javaSqlEnum.put(JAVA_DOUBLE, new Integer(Types.DOUBLE));
        javaSqlEnum.put(JAVA_FLOAT, new Integer(Types.FLOAT));
        javaSqlEnum.put(JAVA_INTEGER, new Integer(Types.INTEGER));
        javaSqlEnum.put(JAVA_SHORT, new Integer(Types.SMALLINT));
        javaSqlEnum.put(JAVA_BYTE, new Integer(Types.TINYINT));
        javaSqlEnum.put(JAVA_TIME, new Integer(Types.TIME));
        javaSqlEnum.put(JAVA_TIMESTAMP, new Integer(Types.TIMESTAMP));

        // add primitives
        javaSqlEnum.put("byte", new Integer(Types.TINYINT));
        javaSqlEnum.put("int", new Integer(Types.INTEGER));
        javaSqlEnum.put("short", new Integer(Types.SMALLINT));
        javaSqlEnum.put("char", new Integer(Types.CHAR));
        javaSqlEnum.put("double", new Integer(Types.DOUBLE));
        javaSqlEnum.put("long", new Integer(Types.BIGINT));
        javaSqlEnum.put("float", new Integer(Types.FLOAT));
        javaSqlEnum.put("boolean", new Integer(Types.BIT));
    }

    /**
     * Returns true if supplied type can have a length attribute as a part of column
     * definition.
     */
    public static boolean supportsLength(int type) {
        return type == Types.BINARY
                || type == Types.CHAR
                || type == Types.DECIMAL
                || type == Types.DOUBLE
                || type == Types.FLOAT
                || type == Types.NUMERIC
                || type == Types.REAL
                || type == Types.VARBINARY
                || type == Types.VARCHAR;
    }

    /**
     * Returns true if supplied type is a numeric type.
     */
    public static boolean isNumeric(int type) {
        return type == Types.BIGINT
                || type == Types.BIT
                || type == Types.DECIMAL
                || type == Types.DOUBLE
                || type == Types.FLOAT
                || type == Types.INTEGER
                || type == Types.NUMERIC
                || type == Types.REAL
                || type == Types.SMALLINT
                || type == Types.TINYINT;
    }

    /**
     * Returns true if supplied type is a decimal type.
     */
    public static boolean isDecimal(int type) {
        return type == Types.DECIMAL
                || type == Types.DOUBLE
                || type == Types.FLOAT
                || type == Types.REAL
                || type == Types.NUMERIC;
    }

    /** Returns an array of string names of the default JDBC data types. */
    public static String[] getDatabaseTypes() {
        return sqlStringType.keySet().toArray(new String[0]);
    }

    /**
     * Method implements an algorithm to pick a data type from a list of alternatives that
     * most closely matches JDBC data type.
     */
    protected static String pickDataType(int jdbcType, TypeInfo[] alts) {
        int len = alts.length;

        if (len == 0)
            return null;

        if (len == 1)
            return alts[0].name;

        // now the fun starts.. try to guess the right type

        String jdbcName = getSqlNameByType(jdbcType).toUpperCase();

        // 1. exact match
        for (int i = 0; i < len; i++) {
            if (jdbcName.equalsIgnoreCase(alts[i].name))
                return alts[i].name;
        }

        // 2. filter those with biggest precision
        long maxPrec = 0;
        for (int i = 0; i < len; i++) {
            if (maxPrec < alts[i].precision) {
                maxPrec = alts[i].precision;
            }
        }

        List<TypeInfo> list = new ArrayList<TypeInfo>();
        for (int i = 0; i < len; i++) {
            if (maxPrec == alts[i].precision) {
                list.add(alts[i]);
            }
        }

        // work with smaller list now.....
        int slen = list.size();
        if (slen == 1)
            return (list.get(0)).name;

        // start/end match
        for (int i = 0; i < slen; i++) {
            String uppercase = (list.get(i)).name.toUpperCase();
            if (uppercase.startsWith(jdbcName) || uppercase.endsWith(jdbcName))
                return (list.get(i)).name;
        }

        // in the middle match
        for (int i = 0; i < slen; i++) {
            String uppercase = (list.get(i)).name.toUpperCase();

            if (uppercase.contains(jdbcName))
                return (list.get(i)).name;
        }

        // out of ideas... return the first one
        return (list.get(0)).name;
    }

    /**
     * Returns a JDBC int type for SQL typem name.
     */
    public static int getSqlTypeByName(String typeName) {
        Integer tmp = sqlStringType.get(typeName);
        return (null == tmp) ? NOT_DEFINED : tmp.intValue();
    }

    /**
     * Returns a String representation of the SQL type from its JDBC code.
     */
    public static String getSqlNameByType(int type) {
        return sqlEnumType.get(new Integer(type));
    }

    /**
     * Returns default java.sql.Types type by the Java type name.
     * 
     * @param className Fully qualified Java Class name.
     * @return The SQL type or NOT_DEFINED if no type found.
     */
    public static int getSqlTypeByJava(String className) {
        if (className == null) {
            return NOT_DEFINED;
        }

        Integer type = javaSqlEnum.get(className);
        if (type != null) {
            return type.intValue();
        }

        // try to load a Java class - some nonstandard mappings may work

        Class<?> aClass;
        try {
            aClass = Util.getJavaClass(className);
        }
        catch (Throwable th) {
            return NOT_DEFINED;
        }

        return getSqlTypeByJava(aClass);
    }

    /**
     * Guesses a default JDBC type for the Java class.
     * 
     * @since 1.1
     */
    public static int getSqlTypeByJava(Class<?> javaClass) {
        if (javaClass == null) {
            return NOT_DEFINED;
        }

        // check standard mapping of class and superclasses
        Class<?> aClass = javaClass;
        while (aClass != null) {

            String name;

            if (aClass.isArray()) {
                name = aClass.getComponentType().getName() + "[]";
            }
            else {
                name = aClass.getName();
            }

            Object type = javaSqlEnum.get(name);
            if (type != null) {
                return ((Number) type).intValue();
            }

            aClass = aClass.getSuperclass();
        }

        // check non-standard JDBC types that are still supported by JPA
        if (javaClass.isArray()) {

            Class<?> elementType = javaClass.getComponentType();
            if (Character.class.isAssignableFrom(elementType)
                    || Character.TYPE.isAssignableFrom(elementType)) {
                return Types.VARCHAR;
            }
            else if (Byte.class.isAssignableFrom(elementType)
                    || Byte.TYPE.isAssignableFrom(elementType)) {
                return Types.VARBINARY;
            }
        }
        
        if (Calendar.class.isAssignableFrom(javaClass)) {
            return Types.TIMESTAMP;
        }
        else if (BigInteger.class.isAssignableFrom(javaClass)) {
            return Types.BIGINT;
        }
        // serializable check should be the last one when all other mapping attempts
        // failed
        else if (Serializable.class.isAssignableFrom(javaClass)) {
            return Types.VARBINARY;
        }

        return NOT_DEFINED;
    }

    /**
     * Get the corresponding Java type by its java.sql.Types counterpart.
     * 
     * @return Fully qualified Java type name or null if not found.
     */
    public static String getJavaBySqlType(int type) {
        return sqlEnumJava.get(new Integer(type));
    }

    /**
     * Get the corresponding Java type by its java.sql.Types counterpart.
     * 
     * @return Fully qualified Java type name or null if not found.
     */
    public static String getJavaBySqlType(int type, int length, int precision) {

        if (type == Types.NUMERIC && precision == 0) {
            type = Types.INTEGER;
        }
        return sqlEnumJava.get(new Integer(type));
    }

    // *************************************************************
    // non-static code
    // *************************************************************

    protected Map<Integer, List<TypeInfo>> databaseTypes = new HashMap<Integer, List<TypeInfo>>();

    public TypesMapping(DatabaseMetaData metaData) throws SQLException {
        // map database types to standard JDBC types
        ResultSet rs = metaData.getTypeInfo();

        try {
            while (rs.next()) {
                TypeInfo info = new TypeInfo();
                info.name = rs.getString("TYPE_NAME");
                info.jdbcType = rs.getInt("DATA_TYPE");
                info.precision = rs.getLong("PRECISION");

                Integer key = new Integer(info.jdbcType);
                List<TypeInfo> infos = databaseTypes.get(key);

                if (infos == null) {
                    infos = new ArrayList<TypeInfo>();
                    databaseTypes.put(key, infos);
                }

                infos.add(info);
            }
        }
        finally {
            rs.close();
        }

        // do some tricks to substitute for missing datatypes

        // 1. swap TIMESTAMP - DATE
        Integer ts = new Integer(Types.TIMESTAMP);
        Integer dt = new Integer(Types.DATE);
        List<TypeInfo> tsInfo = databaseTypes.get(ts);
        List<TypeInfo> dtInfo = databaseTypes.get(dt);

        if (tsInfo != null && dtInfo == null)
            databaseTypes.put(dt, tsInfo);

        if (dtInfo != null && tsInfo == null)
            databaseTypes.put(ts, dtInfo);

        // 2. Swap CLOB - LONGVARCHAR
        Integer clob = new Integer(Types.CLOB);
        Integer lvc = new Integer(Types.LONGVARCHAR);
        List<TypeInfo> clobInfo = databaseTypes.get(clob);
        List<TypeInfo> lvcInfo = databaseTypes.get(lvc);

        if (clobInfo != null && lvcInfo == null)
            databaseTypes.put(lvc, clobInfo);

        if (lvcInfo != null && clobInfo == null)
            databaseTypes.put(clob, lvcInfo);

        // 2. Swap BLOB - LONGVARBINARY
        Integer blob = new Integer(Types.BLOB);
        Integer lvb = new Integer(Types.LONGVARBINARY);
        List<TypeInfo> blobInfo = databaseTypes.get(blob);
        List<TypeInfo> lvbInfo = databaseTypes.get(lvb);

        if (blobInfo != null && lvbInfo == null)
            databaseTypes.put(lvb, blobInfo);

        if (lvbInfo != null && blobInfo == null)
            databaseTypes.put(blob, lvbInfo);
    }

    /** Stores (incomplete) information about database data type */
    static class TypeInfo {

        String name;
        int jdbcType;
        long precision;

        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append("[   TypeInfo: ").append(name);
            buf.append("\n    JDBC Type: ").append(
                    TypesMapping.getSqlNameByType(jdbcType));
            buf.append("\n    Precision: ").append(precision);
            buf.append("\n]");
            return buf.toString();
        }
    }

}

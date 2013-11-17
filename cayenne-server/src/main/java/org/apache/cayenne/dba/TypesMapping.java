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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.util.Util;

/**
 * A utility class that handles mappings of JDBC data types to the database types and Java
 * types. Also contains methods that provide information about JDBC types.
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
    public static final String SQL_NULL = "NULL";

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
        sqlStringType.put(SQL_ARRAY, Integer.valueOf(Types.ARRAY));
        sqlStringType.put(SQL_BIGINT, Integer.valueOf(Types.BIGINT));
        sqlStringType.put(SQL_BINARY, Integer.valueOf(Types.BINARY));
        sqlStringType.put(SQL_BIT, Integer.valueOf(Types.BIT));
        sqlStringType.put(SQL_BLOB, Integer.valueOf(Types.BLOB));
        sqlStringType.put(SQL_BOOLEAN, Integer.valueOf(Types.BOOLEAN));
        sqlStringType.put(SQL_CLOB, Integer.valueOf(Types.CLOB));
        sqlStringType.put(SQL_CHAR, Integer.valueOf(Types.CHAR));
        sqlStringType.put(SQL_DATE, Integer.valueOf(Types.DATE));
        sqlStringType.put(SQL_DECIMAL, Integer.valueOf(Types.DECIMAL));
        sqlStringType.put(SQL_DOUBLE, Integer.valueOf(Types.DOUBLE));
        sqlStringType.put(SQL_FLOAT, Integer.valueOf(Types.FLOAT));
        sqlStringType.put(SQL_INTEGER, Integer.valueOf(Types.INTEGER));
        sqlStringType.put(SQL_LONGVARCHAR, Integer.valueOf(Types.LONGVARCHAR));
        sqlStringType.put(SQL_LONGVARBINARY, Integer.valueOf(Types.LONGVARBINARY));
        sqlStringType.put(SQL_NUMERIC, Integer.valueOf(Types.NUMERIC));
        sqlStringType.put(SQL_REAL, Integer.valueOf(Types.REAL));
        sqlStringType.put(SQL_SMALLINT, Integer.valueOf(Types.SMALLINT));
        sqlStringType.put(SQL_TINYINT, Integer.valueOf(Types.TINYINT));
        sqlStringType.put(SQL_TIME, Integer.valueOf(Types.TIME));
        sqlStringType.put(SQL_TIMESTAMP, Integer.valueOf(Types.TIMESTAMP));
        sqlStringType.put(SQL_VARBINARY, Integer.valueOf(Types.VARBINARY));
        sqlStringType.put(SQL_VARCHAR, Integer.valueOf(Types.VARCHAR));
        sqlStringType.put(SQL_OTHER, Integer.valueOf(Types.OTHER));
        sqlStringType.put(SQL_NULL, Integer.valueOf(Types.NULL));

        sqlEnumType.put(Integer.valueOf(Types.ARRAY), SQL_ARRAY);
        sqlEnumType.put(Integer.valueOf(Types.BIGINT), SQL_BIGINT);
        sqlEnumType.put(Integer.valueOf(Types.BINARY), SQL_BINARY);
        sqlEnumType.put(Integer.valueOf(Types.BIT), SQL_BIT);
        sqlEnumType.put(Integer.valueOf(Types.BOOLEAN), SQL_BOOLEAN);
        sqlEnumType.put(Integer.valueOf(Types.BLOB), SQL_BLOB);
        sqlEnumType.put(Integer.valueOf(Types.CLOB), SQL_CLOB);
        sqlEnumType.put(Integer.valueOf(Types.CHAR), SQL_CHAR);
        sqlEnumType.put(Integer.valueOf(Types.DATE), SQL_DATE);
        sqlEnumType.put(Integer.valueOf(Types.DECIMAL), SQL_DECIMAL);
        sqlEnumType.put(Integer.valueOf(Types.DOUBLE), SQL_DOUBLE);
        sqlEnumType.put(Integer.valueOf(Types.FLOAT), SQL_FLOAT);
        sqlEnumType.put(Integer.valueOf(Types.INTEGER), SQL_INTEGER);
        sqlEnumType.put(Integer.valueOf(Types.LONGVARCHAR), SQL_LONGVARCHAR);
        sqlEnumType.put(Integer.valueOf(Types.LONGVARBINARY), SQL_LONGVARBINARY);
        sqlEnumType.put(Integer.valueOf(Types.NUMERIC), SQL_NUMERIC);
        sqlEnumType.put(Integer.valueOf(Types.REAL), SQL_REAL);
        sqlEnumType.put(Integer.valueOf(Types.SMALLINT), SQL_SMALLINT);
        sqlEnumType.put(Integer.valueOf(Types.TINYINT), SQL_TINYINT);
        sqlEnumType.put(Integer.valueOf(Types.TIME), SQL_TIME);
        sqlEnumType.put(Integer.valueOf(Types.TIMESTAMP), SQL_TIMESTAMP);
        sqlEnumType.put(Integer.valueOf(Types.VARBINARY), SQL_VARBINARY);
        sqlEnumType.put(Integer.valueOf(Types.VARCHAR), SQL_VARCHAR);
        sqlEnumType.put(Integer.valueOf(Types.OTHER), SQL_OTHER);
        sqlEnumType.put(Integer.valueOf(Types.NULL), SQL_NULL);

        sqlEnumJava.put(Integer.valueOf(Types.BIGINT), JAVA_LONG);
        sqlEnumJava.put(Integer.valueOf(Types.BINARY), JAVA_BYTES);
        sqlEnumJava.put(Integer.valueOf(Types.BIT), JAVA_BOOLEAN);
        sqlEnumJava.put(Integer.valueOf(Types.BOOLEAN), JAVA_BOOLEAN);
        sqlEnumJava.put(Integer.valueOf(Types.BLOB), JAVA_BYTES);
        sqlEnumJava.put(Integer.valueOf(Types.CLOB), JAVA_STRING);
        sqlEnumJava.put(Integer.valueOf(Types.CHAR), JAVA_STRING);
        sqlEnumJava.put(Integer.valueOf(Types.DATE), JAVA_UTILDATE);
        sqlEnumJava.put(Integer.valueOf(Types.DECIMAL), JAVA_BIGDECIMAL);
        sqlEnumJava.put(Integer.valueOf(Types.DOUBLE), JAVA_DOUBLE);
        sqlEnumJava.put(Integer.valueOf(Types.FLOAT), JAVA_FLOAT);
        sqlEnumJava.put(Integer.valueOf(Types.INTEGER), JAVA_INTEGER);
        sqlEnumJava.put(Integer.valueOf(Types.LONGVARCHAR), JAVA_STRING);
        sqlEnumJava.put(Integer.valueOf(Types.LONGVARBINARY), JAVA_BYTES);
        sqlEnumJava.put(Integer.valueOf(Types.NUMERIC), JAVA_BIGDECIMAL);
        sqlEnumJava.put(Integer.valueOf(Types.REAL), JAVA_FLOAT);
        sqlEnumJava.put(Integer.valueOf(Types.SMALLINT), JAVA_SHORT);
        sqlEnumJava.put(Integer.valueOf(Types.TINYINT), JAVA_SHORT);
        sqlEnumJava.put(Integer.valueOf(Types.TIME), JAVA_UTILDATE);
        sqlEnumJava.put(Integer.valueOf(Types.TIMESTAMP), JAVA_UTILDATE);
        sqlEnumJava.put(Integer.valueOf(Types.VARBINARY), JAVA_BYTES);
        sqlEnumJava.put(Integer.valueOf(Types.VARCHAR), JAVA_STRING);

        javaSqlEnum.put(JAVA_LONG, Integer.valueOf(Types.BIGINT));
        javaSqlEnum.put(JAVA_BYTES, Integer.valueOf(Types.BINARY));
        javaSqlEnum.put(JAVA_BOOLEAN, Integer.valueOf(Types.BIT));
        javaSqlEnum.put(JAVA_STRING, Integer.valueOf(Types.VARCHAR));
        javaSqlEnum.put(JAVA_SQLDATE, Integer.valueOf(Types.DATE));
        javaSqlEnum.put(JAVA_UTILDATE, Integer.valueOf(Types.DATE));
        javaSqlEnum.put(JAVA_TIMESTAMP, Integer.valueOf(Types.TIMESTAMP));
        javaSqlEnum.put(JAVA_BIGDECIMAL, Integer.valueOf(Types.DECIMAL));
        javaSqlEnum.put(JAVA_DOUBLE, Integer.valueOf(Types.DOUBLE));
        javaSqlEnum.put(JAVA_FLOAT, Integer.valueOf(Types.FLOAT));
        javaSqlEnum.put(JAVA_INTEGER, Integer.valueOf(Types.INTEGER));
        javaSqlEnum.put(JAVA_SHORT, Integer.valueOf(Types.SMALLINT));
        javaSqlEnum.put(JAVA_BYTE, Integer.valueOf(Types.SMALLINT));
        javaSqlEnum.put(JAVA_TIME, Integer.valueOf(Types.TIME));
        javaSqlEnum.put(JAVA_TIMESTAMP, Integer.valueOf(Types.TIMESTAMP));

        // add primitives
        javaSqlEnum.put("byte", Integer.valueOf(Types.TINYINT));
        javaSqlEnum.put("int", Integer.valueOf(Types.INTEGER));
        javaSqlEnum.put("short", Integer.valueOf(Types.SMALLINT));
        javaSqlEnum.put("char", Integer.valueOf(Types.CHAR));
        javaSqlEnum.put("double", Integer.valueOf(Types.DOUBLE));
        javaSqlEnum.put("long", Integer.valueOf(Types.BIGINT));
        javaSqlEnum.put("float", Integer.valueOf(Types.FLOAT));
        javaSqlEnum.put("boolean", Integer.valueOf(Types.BIT));
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

    /**
     * Returns an array of string names of the default JDBC data types.
     */
    public static String[] getDatabaseTypes() {
        Collection<String> types = sqlStringType.keySet();
        return types.toArray(new String[types.size()]);
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
        return sqlEnumType.get(Integer.valueOf(type));
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
     * Get the corresponding Java type by its java.sql.Types counterpart. Note that this
     * method should be used as a last resort, with explicit mapping provided by user used
     * as a first choice, as it can only guess how to map certain types, such as NUMERIC,
     * etc.
     * 
     * @return Fully qualified Java type name or null if not found.
     */
    public static String getJavaBySqlType(int type) {
        return sqlEnumJava.get(Integer.valueOf(type));
    }

 
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

                Integer key = Integer.valueOf(info.jdbcType);
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
        Integer ts = Integer.valueOf(Types.TIMESTAMP);
        Integer dt = Integer.valueOf(Types.DATE);
        List<TypeInfo> tsInfo = databaseTypes.get(ts);
        List<TypeInfo> dtInfo = databaseTypes.get(dt);

        if (tsInfo != null && dtInfo == null)
            databaseTypes.put(dt, tsInfo);

        if (dtInfo != null && tsInfo == null)
            databaseTypes.put(ts, dtInfo);

        // 2. Swap CLOB - LONGVARCHAR
        Integer clob = Integer.valueOf(Types.CLOB);
        Integer lvc = Integer.valueOf(Types.LONGVARCHAR);
        List<TypeInfo> clobInfo = databaseTypes.get(clob);
        List<TypeInfo> lvcInfo = databaseTypes.get(lvc);

        if (clobInfo != null && lvcInfo == null)
            databaseTypes.put(lvc, clobInfo);

        if (lvcInfo != null && clobInfo == null)
            databaseTypes.put(clob, lvcInfo);

        // 2. Swap BLOB - LONGVARBINARY
        Integer blob = Integer.valueOf(Types.BLOB);
        Integer lvb = Integer.valueOf(Types.LONGVARBINARY);
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

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append("[   TypeInfo: ").append(name);
            buf.append("\n    JDBC Type: ").append(
                    TypesMapping.getSqlNameByType(jdbcType));
            buf.append("\n    Precision: ").append(precision);
            buf.append("\n]");
            return buf.toString();
        }
    }

}

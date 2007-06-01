/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.dba;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 
 * A utility class that handles mappings of JDBC data types to 
 * the database types and Java types. Also contains methods that
 * provide information about JDBC types.
 *
 *
 * @author Michael Shengaout
 * @author Andrei Adamchik
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

    /** Keys: SQL string type names,
     *  Values: SQL int type definitions from java.sql.Types */
    private static final Map sqlStringType = new HashMap();

    /** Keys: SQL int type definitions from java.sql.Types,
     *  Values: SQL string type names */
    private static final Map sqlEnumType = new HashMap();

    /** Keys: SQL int type definitions from java.sql.Types,
     *  Values: java class names */
    private static final Map sqlEnumJava = new HashMap();

    /** Keys: java class names,
     *  Values:  SQL int type definitions from java.sql.Types */
    private static final Map javaSqlEnum = new HashMap();

    static {
        sqlStringType.put(SQL_ARRAY, new Integer(Types.ARRAY));
        sqlStringType.put(SQL_BIGINT, new Integer(Types.BIGINT));
        sqlStringType.put(SQL_BINARY, new Integer(Types.BINARY));
        sqlStringType.put(SQL_BIT, new Integer(Types.BIT));
        sqlStringType.put(SQL_BLOB, new Integer(Types.BLOB));
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
    }

    /** 
     * Returns true if supplied type can have a length attribute 
     * as a part of column definition. 
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

    /** Returns an array of string names of the default JDBC data types.*/
    public static String[] getDatabaseTypes() {
        Set keys = sqlStringType.keySet();
        int len = keys.size();
        String[] types = new String[len];

        Iterator it = keys.iterator();
        for (int i = 0; i < len; i++) {
            types[i] = (String) it.next();
        }

        return types;
    }

    /** Method implements an algorithm to pick a data type from a list of alternatives
    * that most closely matches JDBC data type. */
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

        List list = new ArrayList();
        for (int i = 0; i < len; i++) {
            if (maxPrec == alts[i].precision) {
                list.add(alts[i]);
            }
        }

        // work with smaller list now.....
        int slen = list.size();
        if (slen == 1)
            return ((TypeInfo) list.get(0)).name;

        // start/end match
        for (int i = 0; i < slen; i++) {
            String uppercase = ((TypeInfo) list.get(i)).name.toUpperCase();
            if (uppercase.startsWith(jdbcName) || uppercase.endsWith(jdbcName))
                return ((TypeInfo) list.get(i)).name;
        }

        // in the middle match
        for (int i = 0; i < slen; i++) {
            String uppercase = ((TypeInfo) list.get(i)).name.toUpperCase();

            if (uppercase.indexOf(jdbcName) >= 0)
                return ((TypeInfo) list.get(i)).name;
        }

        // out of ideas... return the first one
        return ((TypeInfo) list.get(0)).name;
    }

    /** 
     * Returns a JDBC int type for SQL typem name.
     */
    public static int getSqlTypeByName(String typeName) {
        Integer tmp = (Integer) sqlStringType.get(typeName);
        return (null == tmp) ? NOT_DEFINED : tmp.intValue();
    }

    /** 
     * Returns a String representation of the SQL type from its JDBC code.
     */
    public static String getSqlNameByType(int type) {
        return (String) sqlEnumType.get(new Integer(type));
    }

    /** 
     * Returns default java.sql.Types type by the Java type name.
     * 
     * @param javaTypeName Fully qualified Java Class name.
     * @return The SQL type or NOT_DEFINED if no type found.
     */
    public static int getSqlTypeByJava(String javaTypeName) {
        Integer temp = (Integer) javaSqlEnum.get(javaTypeName);
        return (null == temp) ? NOT_DEFINED : temp.intValue();
    }

    /**
     * Guesses a default JDBC type for the Java class.
     * 
     * @since 1.1
     */
    public static int getSqlTypeByJava(Class javaClass) {
        while (javaClass != null) {
            Object type = javaSqlEnum.get(javaClass.getName());
            if (type != null) {
                return ((Number) type).intValue();
            }

            javaClass = javaClass.getSuperclass();
        }

        return NOT_DEFINED;
    }

    /** 
     * Get the corresponding Java type by its java.sql.Types counterpart.
     * 
     *  @return Fully qualified Java type name or null if not found. 
     */
    public static String getJavaBySqlType(int type) {
        return (String) sqlEnumJava.get(new Integer(type));
    }

    /** 
      * Get the corresponding Java type by its java.sql.Types counterpart.
      * 
      *  @return Fully qualified Java type name or null if not found. 
      */
    public static String getJavaBySqlType(int type, int length, int precision) {

        if (type == Types.NUMERIC && precision == 0) {
            type = Types.INTEGER;
        }
        return (String) sqlEnumJava.get(new Integer(type));
    }

    // *************************************************************
    // non-static code
    // *************************************************************

    protected Map databaseTypes = new HashMap();

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
                List infos = (List) databaseTypes.get(key);

                if (infos == null) {
                    infos = new ArrayList();
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
        List tsInfo = (List) databaseTypes.get(ts);
        List dtInfo = (List) databaseTypes.get(dt);

        if (tsInfo != null && dtInfo == null)
            databaseTypes.put(dt, tsInfo);

        if (dtInfo != null && tsInfo == null)
            databaseTypes.put(ts, dtInfo);

        // 2. Swap CLOB - LONGVARCHAR
        Integer clob = new Integer(Types.CLOB);
        Integer lvc = new Integer(Types.LONGVARCHAR);
        List clobInfo = (List) databaseTypes.get(clob);
        List lvcInfo = (List) databaseTypes.get(lvc);

        if (clobInfo != null && lvcInfo == null)
            databaseTypes.put(lvc, clobInfo);

        if (lvcInfo != null && clobInfo == null)
            databaseTypes.put(clob, lvcInfo);

        // 2. Swap BLOB - LONGVARBINARY
        Integer blob = new Integer(Types.BLOB);
        Integer lvb = new Integer(Types.LONGVARBINARY);
        List blobInfo = (List) databaseTypes.get(blob);
        List lvbInfo = (List) databaseTypes.get(lvb);

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
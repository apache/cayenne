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

package org.apache.cayenne.dba;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.util.Util;

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

import static java.sql.Types.ARRAY;
import static java.sql.Types.BIGINT;
import static java.sql.Types.BINARY;
import static java.sql.Types.BIT;
import static java.sql.Types.BLOB;
import static java.sql.Types.BOOLEAN;
import static java.sql.Types.CHAR;
import static java.sql.Types.CLOB;
import static java.sql.Types.DATE;
import static java.sql.Types.DECIMAL;
import static java.sql.Types.DOUBLE;
import static java.sql.Types.FLOAT;
import static java.sql.Types.INTEGER;
import static java.sql.Types.LONGNVARCHAR;
import static java.sql.Types.LONGVARBINARY;
import static java.sql.Types.LONGVARCHAR;
import static java.sql.Types.NCHAR;
import static java.sql.Types.NCLOB;
import static java.sql.Types.NULL;
import static java.sql.Types.NUMERIC;
import static java.sql.Types.NVARCHAR;
import static java.sql.Types.OTHER;
import static java.sql.Types.REAL;
import static java.sql.Types.SMALLINT;
import static java.sql.Types.SQLXML;
import static java.sql.Types.TIME;
import static java.sql.Types.TIMESTAMP;
import static java.sql.Types.TINYINT;
import static java.sql.Types.VARBINARY;
import static java.sql.Types.VARCHAR;

/**
 * A utility class that handles mappings of JDBC data types to the database
 * types and Java types. Also contains methods that provide information about
 * JDBC types.
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
	public static final String SQL_NCLOB = "NCLOB";
	public static final String SQL_CHAR = "CHAR";
	public static final String SQL_NCHAR = "NCHAR";
	public static final String SQL_DATE = "DATE";
	public static final String SQL_DECIMAL = "DECIMAL";
	public static final String SQL_DOUBLE = "DOUBLE";
	public static final String SQL_FLOAT = "FLOAT";
	public static final String SQL_INTEGER = "INTEGER";
	public static final String SQL_LONGVARCHAR = "LONGVARCHAR";
	public static final String SQL_LONGNVARCHAR = "LONGNVARCHAR";
	public static final String SQL_LONGVARBINARY = "LONGVARBINARY";
	public static final String SQL_NUMERIC = "NUMERIC";
	public static final String SQL_REAL = "REAL";
	public static final String SQL_SMALLINT = "SMALLINT";
	public static final String SQL_TINYINT = "TINYINT";
	public static final String SQL_TIME = "TIME";
	public static final String SQL_TIMESTAMP = "TIMESTAMP";
	public static final String SQL_VARBINARY = "VARBINARY";
	public static final String SQL_VARCHAR = "VARCHAR";
	public static final String SQL_NVARCHAR = "NVARCHAR";
	public static final String SQL_SQLXML = "SQLXML";
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
	public static final String JAVA_BIGINTEGER = "java.math.BigInteger";
	public static final String JAVA_DOUBLE = "java.lang.Double";
	public static final String JAVA_FLOAT = "java.lang.Float";
	public static final String JAVA_INTEGER = "java.lang.Integer";
	public static final String JAVA_SHORT = "java.lang.Short";
	public static final String JAVA_BYTE = "java.lang.Byte";
	public static final String JAVA_TIME = "java.sql.Time";
	public static final String JAVA_TIMESTAMP = "java.sql.Timestamp";
	public static final String JAVA_BLOB = "java.sql.Blob";

	/**
	 * Keys: SQL string type names, Values: SQL int type definitions from
	 * java.sql.Types
	 */
	private static final Map<String, Integer> SQL_STRING_TYPE = new HashMap<>();

	/**
	 * Keys: SQL int type definitions from java.sql.Types, Values: SQL string
	 * type names
	 */
	private static final Map<Integer, String> SQL_ENUM_TYPE = new HashMap<>();

	/**
	 * Keys: SQL int type definitions from java.sql.Types, Values: java class
	 * names
	 */
	private static final Map<Integer, String> SQL_ENUM_JAVA = new HashMap<>();

	/**
	 * Keys: java class names, Values: SQL int type definitions from
	 * java.sql.Types
	 */
	private static final Map<String, Integer> JAVA_SQL_ENUM = new HashMap<>();

	static {
		// SQL_STRING_TYPE.put(SQL_ARRAY, ARRAY);
		SQL_STRING_TYPE.put(SQL_BIGINT, BIGINT);
		SQL_STRING_TYPE.put(SQL_BINARY, BINARY);
		SQL_STRING_TYPE.put(SQL_BIT, BIT);
		SQL_STRING_TYPE.put(SQL_BLOB, BLOB);
		SQL_STRING_TYPE.put(SQL_BOOLEAN, BOOLEAN);
		SQL_STRING_TYPE.put(SQL_CLOB, CLOB);
		SQL_STRING_TYPE.put(SQL_NCLOB, NCLOB);
		SQL_STRING_TYPE.put(SQL_CHAR, CHAR);
		SQL_STRING_TYPE.put(SQL_NCHAR, NCHAR);
		SQL_STRING_TYPE.put(SQL_DATE, DATE);
		SQL_STRING_TYPE.put(SQL_DECIMAL, DECIMAL);
		SQL_STRING_TYPE.put(SQL_DOUBLE, DOUBLE);
		SQL_STRING_TYPE.put(SQL_FLOAT, FLOAT);
		SQL_STRING_TYPE.put(SQL_INTEGER, INTEGER);
		SQL_STRING_TYPE.put(SQL_LONGVARCHAR, LONGVARCHAR);
		SQL_STRING_TYPE.put(SQL_LONGNVARCHAR, LONGNVARCHAR);
		SQL_STRING_TYPE.put(SQL_LONGVARBINARY, LONGVARBINARY);
		SQL_STRING_TYPE.put(SQL_NUMERIC, NUMERIC);
		SQL_STRING_TYPE.put(SQL_REAL, REAL);
		SQL_STRING_TYPE.put(SQL_SMALLINT, SMALLINT);
		SQL_STRING_TYPE.put(SQL_TINYINT, TINYINT);
		SQL_STRING_TYPE.put(SQL_TIME, TIME);
		SQL_STRING_TYPE.put(SQL_TIMESTAMP, TIMESTAMP);
		SQL_STRING_TYPE.put(SQL_VARBINARY, VARBINARY);
		SQL_STRING_TYPE.put(SQL_VARCHAR, VARCHAR);
		SQL_STRING_TYPE.put(SQL_NVARCHAR, NVARCHAR);
		SQL_STRING_TYPE.put(SQL_OTHER, OTHER);
		SQL_STRING_TYPE.put(SQL_NULL, NULL);

		SQL_ENUM_TYPE.put(ARRAY, SQL_ARRAY);
		SQL_ENUM_TYPE.put(BIGINT, SQL_BIGINT);
		SQL_ENUM_TYPE.put(BINARY, SQL_BINARY);
		SQL_ENUM_TYPE.put(BIT, SQL_BIT);
		SQL_ENUM_TYPE.put(BOOLEAN, SQL_BOOLEAN);
		SQL_ENUM_TYPE.put(BLOB, SQL_BLOB);
		SQL_ENUM_TYPE.put(CLOB, SQL_CLOB);
		SQL_ENUM_TYPE.put(NCLOB, SQL_NCLOB);
		SQL_ENUM_TYPE.put(CHAR, SQL_CHAR);
		SQL_ENUM_TYPE.put(NCHAR, SQL_NCHAR);
		SQL_ENUM_TYPE.put(DATE, SQL_DATE);
		SQL_ENUM_TYPE.put(DECIMAL, SQL_DECIMAL);
		SQL_ENUM_TYPE.put(DOUBLE, SQL_DOUBLE);
		SQL_ENUM_TYPE.put(FLOAT, SQL_FLOAT);
		SQL_ENUM_TYPE.put(INTEGER, SQL_INTEGER);
		SQL_ENUM_TYPE.put(LONGVARCHAR, SQL_LONGVARCHAR);
		SQL_ENUM_TYPE.put(LONGNVARCHAR, SQL_LONGNVARCHAR);
		SQL_ENUM_TYPE.put(LONGVARBINARY, SQL_LONGVARBINARY);
		SQL_ENUM_TYPE.put(NUMERIC, SQL_NUMERIC);
		SQL_ENUM_TYPE.put(REAL, SQL_REAL);
		SQL_ENUM_TYPE.put(SMALLINT, SQL_SMALLINT);
		SQL_ENUM_TYPE.put(TINYINT, SQL_TINYINT);
		SQL_ENUM_TYPE.put(TIME, SQL_TIME);
		SQL_ENUM_TYPE.put(TIMESTAMP, SQL_TIMESTAMP);
		SQL_ENUM_TYPE.put(VARBINARY, SQL_VARBINARY);
		SQL_ENUM_TYPE.put(VARCHAR, SQL_VARCHAR);
		SQL_ENUM_TYPE.put(NVARCHAR, SQL_NVARCHAR);
		SQL_ENUM_TYPE.put(SQLXML, SQL_SQLXML);
		SQL_ENUM_TYPE.put(OTHER, SQL_OTHER);
		SQL_ENUM_TYPE.put(NULL, SQL_NULL);

		SQL_ENUM_JAVA.put(BIGINT, JAVA_LONG);
		SQL_ENUM_JAVA.put(BINARY, JAVA_BYTES);
		SQL_ENUM_JAVA.put(BIT, JAVA_BOOLEAN);
		SQL_ENUM_JAVA.put(BOOLEAN, JAVA_BOOLEAN);
		SQL_ENUM_JAVA.put(BLOB, JAVA_BYTES);
		SQL_ENUM_JAVA.put(CLOB, JAVA_STRING);
		SQL_ENUM_JAVA.put(NCLOB, JAVA_STRING);
		SQL_ENUM_JAVA.put(CHAR, JAVA_STRING);
		SQL_ENUM_JAVA.put(NCHAR, JAVA_STRING);
		SQL_ENUM_JAVA.put(DATE, JAVA_UTILDATE);
		SQL_ENUM_JAVA.put(DECIMAL, JAVA_BIGDECIMAL);
		SQL_ENUM_JAVA.put(DOUBLE, JAVA_DOUBLE);
		SQL_ENUM_JAVA.put(FLOAT, JAVA_FLOAT);
		SQL_ENUM_JAVA.put(INTEGER, JAVA_INTEGER);
		SQL_ENUM_JAVA.put(LONGVARCHAR, JAVA_STRING);
		SQL_ENUM_JAVA.put(LONGNVARCHAR, JAVA_STRING);
		SQL_ENUM_JAVA.put(LONGVARBINARY, JAVA_BYTES);
		SQL_ENUM_JAVA.put(NUMERIC, JAVA_BIGDECIMAL);
		SQL_ENUM_JAVA.put(REAL, JAVA_FLOAT);
		SQL_ENUM_JAVA.put(SMALLINT, JAVA_SHORT);
		SQL_ENUM_JAVA.put(TINYINT, JAVA_SHORT);
		SQL_ENUM_JAVA.put(TIME, JAVA_UTILDATE);
		SQL_ENUM_JAVA.put(TIMESTAMP, JAVA_UTILDATE);
		SQL_ENUM_JAVA.put(VARBINARY, JAVA_BYTES);
		SQL_ENUM_JAVA.put(VARCHAR, JAVA_STRING);
		SQL_ENUM_JAVA.put(NVARCHAR, JAVA_STRING);
		SQL_ENUM_JAVA.put(SQLXML, JAVA_STRING);

		JAVA_SQL_ENUM.put(JAVA_LONG, BIGINT);
		JAVA_SQL_ENUM.put(JAVA_BYTES, BINARY);
		JAVA_SQL_ENUM.put(JAVA_BOOLEAN, BIT);
		JAVA_SQL_ENUM.put(JAVA_STRING, VARCHAR);
		JAVA_SQL_ENUM.put(JAVA_SQLDATE, DATE);
		JAVA_SQL_ENUM.put(JAVA_UTILDATE, DATE);
		JAVA_SQL_ENUM.put(JAVA_TIMESTAMP, TIMESTAMP);
		JAVA_SQL_ENUM.put(JAVA_BIGDECIMAL, DECIMAL);
		JAVA_SQL_ENUM.put(JAVA_DOUBLE, DOUBLE);
		JAVA_SQL_ENUM.put(JAVA_FLOAT, FLOAT);
		JAVA_SQL_ENUM.put(JAVA_INTEGER, INTEGER);
		JAVA_SQL_ENUM.put(JAVA_SHORT, SMALLINT);
		JAVA_SQL_ENUM.put(JAVA_BYTE, SMALLINT);
		JAVA_SQL_ENUM.put(JAVA_TIME, TIME);

		// add primitives
		JAVA_SQL_ENUM.put("byte", TINYINT);
		JAVA_SQL_ENUM.put("int", INTEGER);
		JAVA_SQL_ENUM.put("short", SMALLINT);
		JAVA_SQL_ENUM.put("char", CHAR);
		JAVA_SQL_ENUM.put("double", DOUBLE);
		JAVA_SQL_ENUM.put("long", BIGINT);
		JAVA_SQL_ENUM.put("float", FLOAT);
		JAVA_SQL_ENUM.put("boolean", BIT);
	}

	// TODO: redo all isXyz as an internal enum over types, where each enum object knows whether it is this or that kind

	/**
	 * Returns true if supplied type is a character type.
	 * @since 4.0
	 * @param type JDBC type
	 * @return true if supplied type is a character type.
	 */
	public static boolean isCharacter(int type) {
		return type == Types.CHAR || type == Types.NCHAR || type == Types.VARCHAR || type == Types.NVARCHAR
				|| type == Types.CLOB || type == Types.NCLOB || type == Types.LONGVARCHAR || type == Types.LONGNVARCHAR;
	}

	/**
	 * Returns true if supplied type is a binary type.
	 * @since 4.0
	 * @param type JDBC type
	 * @return true if supplied type is a binary type.
	 */
	public static boolean isBinary(int type) {
		return type == Types.BINARY || type == Types.BLOB || type == Types.VARBINARY || type == Types.LONGVARBINARY;
	}

	/**
	 * Returns true if supplied type is a numeric type.
	 */
	public static boolean isNumeric(int type) {
		return type == BIGINT || type == BIT || type == DECIMAL || type == DOUBLE || type == FLOAT || type == INTEGER
				|| type == NUMERIC || type == REAL || type == SMALLINT || type == TINYINT;
	}

	/**
	 * Returns true if supplied type is a decimal type.
	 */
	public static boolean isDecimal(int type) {
		return type == DECIMAL || type == DOUBLE || type == FLOAT || type == REAL || type == NUMERIC;
	}

	/**
	 * Returns an array of string names of the default JDBC data types.
	 */
	public static String[] getDatabaseTypes() {
		Collection<String> types = SQL_STRING_TYPE.keySet();
		return types.toArray(new String[0]);
	}

	/**
	 * Method implements an algorithm to pick a data type from a list of
	 * alternatives that most closely matches JDBC data type.
	 */
	protected static String pickDataType(int jdbcType, TypeInfo[] alts) {
		int len = alts.length;

		if (len == 0) {
			return null;
		}

		if (len == 1) {
			return alts[0].name;
		}

		// now the fun starts.. try to guess the right type

		String jdbcName = getSqlNameByType(jdbcType).toUpperCase();

		// 1. exact match
		for (TypeInfo alt : alts) {
			if (jdbcName.equalsIgnoreCase(alt.name)) {
				return alt.name;
			}
		}

		// 2. filter those with biggest precision
		long maxPrec = 0;
		for (TypeInfo alt : alts) {
			if (maxPrec < alt.precision) {
				maxPrec = alt.precision;
			}
		}

		List<TypeInfo> list = new ArrayList<>();
		for (TypeInfo alt : alts) {
			if (maxPrec == alt.precision) {
				list.add(alt);
			}
		}

		// work with smaller list now.....
		int slen = list.size();
		if (slen == 1) {
			return list.get(0).name;
		}

		// start/end match
		for (TypeInfo aList : list) {
			String uppercase = aList.name.toUpperCase();
			if (uppercase.startsWith(jdbcName) || uppercase.endsWith(jdbcName)) {
				return aList.name;
			}
		}

		// in the middle match
		for (TypeInfo aList : list) {
			String uppercase = aList.name.toUpperCase();

			if (uppercase.contains(jdbcName)) {
				return aList.name;
			}
		}

		// out of ideas... return the first one
		return list.get(0).name;
	}

	/**
	 * Returns a JDBC int type for SQL typem name.
	 */
	public static int getSqlTypeByName(String typeName) {
		Integer tmp = SQL_STRING_TYPE.get(typeName);
		return tmp == null ? NOT_DEFINED : tmp;
	}

	/**
	 * Returns a String representation of the SQL type from its JDBC code.
	 */
	public static String getSqlNameByType(int type) {
		return SQL_ENUM_TYPE.get(type);
	}

	/**
	 * Returns default java.sql.Types type by the Java type name.
	 * 
	 * @param className
	 *            Fully qualified Java Class name.
	 * @return The SQL type or NOT_DEFINED if no type found.
	 */
	public static int getSqlTypeByJava(String className) {
		if (className == null) {
			return NOT_DEFINED;
		}

		Integer type = JAVA_SQL_ENUM.get(className);
		if (type != null) {
			return type;
		}

		// try to load a Java class - some nonstandard mappings may work

		Class<?> aClass;
		try {
			aClass = Util.getJavaClass(className);
		} catch (Throwable th) {
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
			} else {
				name = aClass.getName();
			}

			Number type = JAVA_SQL_ENUM.get(name);
			if (type != null) {
				return type.intValue();
			}

			aClass = aClass.getSuperclass();
		}

		// check non-standard JDBC types that are still supported by JPA
		if (javaClass.isArray()) {

			Class<?> elementType = javaClass.getComponentType();
			if (Character.class.isAssignableFrom(elementType) || Character.TYPE.isAssignableFrom(elementType)) {
				return VARCHAR;
			} else if (Byte.class.isAssignableFrom(elementType) || Byte.TYPE.isAssignableFrom(elementType)) {
				return VARBINARY;
			}
		}

		if (Calendar.class.isAssignableFrom(javaClass)) {
			return TIMESTAMP;
		}

		if (BigInteger.class.isAssignableFrom(javaClass)) {
			return BIGINT;
		}

		// serializable check should be the last one when all other mapping
		// attempts
		// failed
		if (Serializable.class.isAssignableFrom(javaClass)) {
			return VARBINARY;
		}

		return NOT_DEFINED;
	}

	/**
	 * Get the corresponding Java type by its {@link java.sql.Types} counterpart. Note
	 * that this method should be used as a last resort, with explicit mapping
	 * provided by user used as a first choice, as it can only guess how to map
	 * certain types, such as NUMERIC, etc.
	 *
	 * @param type as defined in {@link java.sql.Types}
	 * @return Fully qualified Java type name or null if not found.
	 */
	public static String getJavaBySqlType(int type) {
		return SQL_ENUM_JAVA.get(type);
	}

	/**
	 * @param attribute to get java type for
	 * @return Fully qualified Java type name or null if not found.
	 * @see #getJavaBySqlType(int)
	 *
	 * @since 4.2
	 */
	public static String getJavaBySqlType(DbAttribute attribute) {
		if(attribute.getType() == DECIMAL) {
			if(attribute.getScale() == 0) {
				// integer value, could fold into a smaller type
				if (attribute.getMaxLength() < 10) {
					return JAVA_INTEGER;
				} else if(attribute.getMaxLength() < 19) {
					return JAVA_LONG;
				} else {
					return JAVA_BIGINTEGER;
				}
			} else {
				// decimal, no optimizations here
				return JAVA_BIGDECIMAL;
			}
		}

		return SQL_ENUM_JAVA.get(attribute.getType());
	}

	Map<Integer, List<TypeInfo>> databaseTypes = new HashMap<>();

	public TypesMapping(DatabaseMetaData metaData) throws SQLException {
		// map database types to standard JDBC types

		try (ResultSet rs = metaData.getTypeInfo()) {
			while (rs.next()) {
				TypeInfo info = new TypeInfo();
				info.name = rs.getString("TYPE_NAME");
				info.jdbcType = rs.getInt("DATA_TYPE");
				info.precision = rs.getLong("PRECISION");

				Integer key = info.jdbcType;
				List<TypeInfo> infos = databaseTypes.computeIfAbsent(key, k -> new ArrayList<>());

				infos.add(info);
			}
		}

		// do some tricks to substitute for missing datatypes

		// 1. swap TIMESTAMP - DATE
		swapTypes(TIMESTAMP, DATE);

		// 2. Swap CLOB - LONGVARCHAR
		swapTypes(CLOB, LONGVARCHAR);

		// 3. Swap BLOB - LONGVARBINARY
		swapTypes(BLOB, LONGVARBINARY);

		// 4. Swap NCLOB - LONGNVARCHAR
		swapTypes(NCLOB, LONGNVARCHAR);
	}

	private void swapTypes(int type1, int type2) {
		List<TypeInfo> type1Info = databaseTypes.get(type1);
		List<TypeInfo> type2Info = databaseTypes.get(type2);

		if (type1Info != null && type2Info == null) {
			databaseTypes.put(type2, type1Info);
		}
		if (type2Info != null && type1Info == null) {
			databaseTypes.put(type1, type2Info);
		}
	}

	/** Stores (incomplete) information about database data type */
	static class TypeInfo {

		String name;
		int jdbcType;
		long precision;

		@Override
		public String toString() {
			return "[   TypeInfo: " + name + "\n    JDBC Type: " + TypesMapping.getSqlNameByType(jdbcType)
					+ "\n    Precision: " + precision + "\n]";
		}
	}

}

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
package org.apache.cayenne.access.loader.mapper;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.util.Util;

import java.io.Serializable;
import java.math.BigInteger;
import java.sql.Types;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @since 4.0.
 */
public class DefaultJdbc2JavaTypeMapper implements Jdbc2JavaTypeMapper {

	// Never use "-1" or any other normal integer, since there
	// is a big chance it is being reserved in java.sql.Types
	public static final int NOT_DEFINED = Integer.MAX_VALUE;

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
	 * Keys: java class names, Values: SQL int type definitions from
	 * java.sql.Types
	 */
	private final Map<String, Integer> javaSqlEnum = new HashMap<>();

	private final Map<DbType, String> mapping = new HashMap<>();
	private final SortedSet<DbType> dbTypes = new TreeSet<>();

	private Map<String, String> classToPrimitive;

	{
		add(Types.BIGINT, JAVA_LONG);
		add(Types.BINARY, JAVA_BYTES);
		add(Types.BIT, JAVA_BOOLEAN);
		add(Types.BOOLEAN, JAVA_BOOLEAN);
		add(Types.BLOB, JAVA_BYTES);
		add(Types.CLOB, JAVA_STRING);
		add(Types.NCLOB, JAVA_STRING);
		add(Types.SQLXML, JAVA_STRING);
		add(Types.CHAR, JAVA_STRING);
		add(Types.NCHAR, JAVA_STRING);
		add(Types.DATE, JAVA_UTILDATE);
		add(Types.DECIMAL, JAVA_BIGDECIMAL);
		add(Types.DOUBLE, JAVA_DOUBLE);
		add(Types.FLOAT, JAVA_FLOAT);
		add(Types.INTEGER, JAVA_INTEGER);
		add(Types.LONGVARCHAR, JAVA_STRING);
		add(Types.LONGNVARCHAR, JAVA_STRING);
		add(Types.LONGVARBINARY, JAVA_BYTES);
		add(Types.NUMERIC, JAVA_BIGDECIMAL);
		add(Types.REAL, JAVA_FLOAT);
		add(Types.SMALLINT, JAVA_SHORT);
		add(Types.TINYINT, JAVA_SHORT);
		add(Types.TIME, JAVA_UTILDATE);
		add(Types.TIMESTAMP, JAVA_UTILDATE);
		add(Types.VARBINARY, JAVA_BYTES);
		add(Types.VARCHAR, JAVA_STRING);
		add(Types.NVARCHAR, JAVA_STRING);

		javaSqlEnum.put(JAVA_LONG, Types.BIGINT);
		javaSqlEnum.put(JAVA_BYTES, Types.BINARY);
		javaSqlEnum.put(JAVA_BOOLEAN, Types.BIT);
		javaSqlEnum.put(JAVA_STRING, Types.VARCHAR);
		javaSqlEnum.put(JAVA_SQLDATE, Types.DATE);
		javaSqlEnum.put(JAVA_UTILDATE, Types.DATE);
		javaSqlEnum.put(JAVA_TIMESTAMP, Types.TIMESTAMP);
		javaSqlEnum.put(JAVA_BIGDECIMAL, Types.DECIMAL);
		javaSqlEnum.put(JAVA_DOUBLE, Types.DOUBLE);
		javaSqlEnum.put(JAVA_FLOAT, Types.FLOAT);
		javaSqlEnum.put(JAVA_INTEGER, Types.INTEGER);
		javaSqlEnum.put(JAVA_SHORT, Types.SMALLINT);
		javaSqlEnum.put(JAVA_BYTE, Types.SMALLINT);
		javaSqlEnum.put(JAVA_TIME, Types.TIME);
		javaSqlEnum.put(JAVA_TIMESTAMP, Types.TIMESTAMP);

		// add primitives
		javaSqlEnum.put("byte", Types.TINYINT);
		javaSqlEnum.put("int", Types.INTEGER);
		javaSqlEnum.put("short", Types.SMALLINT);
		javaSqlEnum.put("char", Types.CHAR);
		javaSqlEnum.put("double", Types.DOUBLE);
		javaSqlEnum.put("long", Types.BIGINT);
		javaSqlEnum.put("float", Types.FLOAT);
		javaSqlEnum.put("boolean", Types.BIT);

		classToPrimitive = new HashMap<>();
		classToPrimitive.put(Byte.class.getName(), "byte");
		classToPrimitive.put(Long.class.getName(), "long");
		classToPrimitive.put(Double.class.getName(), "double");
		classToPrimitive.put(Boolean.class.getName(), "boolean");
		classToPrimitive.put(Float.class.getName(), "float");
		classToPrimitive.put(Short.class.getName(), "short");
		classToPrimitive.put(Integer.class.getName(), "int");
	}

	private Boolean usePrimitives;

	/**
	 * Returns default java.sql.Types type by the Java type name.
	 *
	 * @param className
	 *            Fully qualified Java Class name.
	 * @return The SQL type or NOT_DEFINED if no type found.
	 */
	public int getJdbcTypeByJava(DbAttribute attribute, String className) {
		if (className == null) {
			return NOT_DEFINED;
		}

		Integer type = javaSqlEnum.get(className);
		if (type != null) {
			return type;
		}

		// try to load a Java class - some nonstandard mappings may work
		try {
			return getSqlTypeByJava(attribute, Util.getJavaClass(className));
		} catch (Throwable th) {
			return NOT_DEFINED;
		}
	}

	public void add(int jdbcType, String java) {
		add(new DbType(TypesMapping.getSqlNameByType(jdbcType)), java);
	}

	@Override
	public void add(DbType type, String java) {
		mapping.put(type, java);
		dbTypes.add(type);
	}

	/**
	 * Guesses a default JDBC type for the Java class.
	 *
	 * @since 1.1
	 */
	protected int getSqlTypeByJava(DbAttribute attribute, Class<?> javaClass) {
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

			Object type = javaSqlEnum.get(name);
			if (type != null) {
				return ((Number) type).intValue();
			}

			aClass = aClass.getSuperclass();
		}

		// check non-standard JDBC types that are still supported by JPA
		if (javaClass.isArray()) {

			Class<?> elementType = javaClass.getComponentType();
			if (Character.class.isAssignableFrom(elementType) || Character.TYPE.isAssignableFrom(elementType)) {
				return Types.VARCHAR;
			} else if (Byte.class.isAssignableFrom(elementType) || Byte.TYPE.isAssignableFrom(elementType)) {
				return Types.VARBINARY;
			}
		}

		if (Calendar.class.isAssignableFrom(javaClass)) {
			return Types.TIMESTAMP;
		}

		if (BigInteger.class.isAssignableFrom(javaClass)) {
			return Types.BIGINT;
		}

		if (Serializable.class.isAssignableFrom(javaClass)) {
			// serializable check should be the last one when all other mapping
			// attempts failed
			return Types.VARBINARY;
		}

		return NOT_DEFINED;
	}

	/**
	 * Get the corresponding Java type by its java.sql.Types counterpart. Note
	 * that this method should be used as a last resort, with explicit mapping
	 * provided by user used as a first choice, as it can only guess how to map
	 * certain types, such as NUMERIC, etc.
	 *
	 * @return Fully qualified Java type name or null if not found.
	 */
	@Override
	public String getJavaByJdbcType(DbAttribute attribute, int type) {
		String jdbcType = TypesMapping.getSqlNameByType(type);
		DbType dbType;
		if (attribute != null) {
			dbType = new DbType(jdbcType, attribute.getMaxLength(), attribute.getAttributePrecision(),
					attribute.getScale(), attribute.isMandatory());
		} else {
			dbType = new DbType(jdbcType);
		}

		String typeName = getJavaByJdbcType(dbType);

		if (usePrimitives != null && usePrimitives) {
			String primitive = classToPrimitive.get(typeName);
			if (primitive != null) {
				return primitive;
			}
		}

		return typeName;
	}

	public String getJavaByJdbcType(DbType type) {
		for (DbType t : dbTypes) {
			if (t.isCover(type)) {
				// because dbTypes sorted by specificity we will take first and
				// the most specific matching
				// that applicable for attribute
				return mapping.get(t);
			}
		}

		return null;
	}

	public Boolean getUsePrimitives() {
		return usePrimitives;
	}

	public void setUsePrimitives(Boolean usePrimitives) {
		this.usePrimitives = usePrimitives;
	}
}

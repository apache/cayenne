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

package org.apache.cayenne.gen;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Methods for mangling strings.
 * 
 */
public class ImportUtils {

	public static final String[] importOrdering = { "java.", "javax.", "org.", "com." };

	static final String[] primitives = { "long", "double", "byte", "boolean", "float", "short", "int", "char" };

	static final String[] primitiveClasses = new String[] { Long.class.getName(), Double.class.getName(),
			Byte.class.getName(), Boolean.class.getName(), Float.class.getName(), Short.class.getName(),
			Integer.class.getName(), Character.class.getName() };

	static Map<String, String> classesForPrimitives = Util.toMap(primitives, primitiveClasses);
	static Map<String, String> primitivesForClasses = Util.toMap(primitiveClasses, primitives);

	protected Map<String, String> importTypesMap = new HashMap<>();

	// Types forced to be FQN
	protected Map<String, String> reservedImportTypesMap = new HashMap<>();

	protected String packageName;

	protected boolean canRegisterType(String typeName) {
		// Not sure why this would ever happen, but it did
		if (null == typeName) {
            return false;
        }

		StringUtils stringUtils = StringUtils.getInstance();
		String typeClassName = stringUtils.stripPackageName(typeName);
		String typePackageName = stringUtils.stripClass(typeName);

		if (typePackageName.length() == 0) {
            return false; // disallow non-packaged types (primitives, probably)
        }
		if ("java.lang".equals(typePackageName)) {
            return false;
        }

		// Can only have one type -- rest must use fqn
		if (reservedImportTypesMap.containsKey(typeClassName)) {
            return false;
        }
        return !importTypesMap.containsKey(typeClassName);
    }

	/**
	 * Reserve a fully-qualified data type class name so it cannot be used by
	 * another class. No import statements will be generated for reserved types.
	 * Typically, this is the fully-qualified class name of the class being
	 * generated.
	 * 
	 * @param typeName
	 *            FQ data type class name.
	 */
	public void addReservedType(String typeName) {
		if (!canRegisterType(typeName)) {
            return;
        }

		StringUtils stringUtils = StringUtils.getInstance();
		String typeClassName = stringUtils.stripPackageName(typeName);

		reservedImportTypesMap.put(typeClassName, typeName);
	}

	/**
	 * Register a fully-qualified data type class name. For example,
	 * org.apache.cayenne.GenericPersistentObject.
	 * 
	 * @param typeName
	 *            FQ data type class name.
	 */
	public void addType(String typeName) {
		if (!canRegisterType(typeName)) {
            return;
        }

		StringUtils stringUtils = StringUtils.getInstance();
		String typePackageName = stringUtils.stripClass(typeName);
		if (typePackageName.equals(packageName)) {
            return;
        }

		importTypesMap.put(stringUtils.stripPackageName(typeName), typeName);
	}

	/**
	 * Add the package name to use for this importUtil invocation.
	 */
	public void setPackage(String packageName) {
		this.packageName = packageName;
	}

	/**
	 * Performs processing similar to <code>formatJavaType(String)</code>, with
	 * special handling of primitive types and their Java class counterparts.
	 * This method allows users to make a decision whether to use primitives or
	 * not, regardless of how type is mapped.
	 */
	public String formatJavaType(String typeName, boolean usePrimitives) {
		if (usePrimitives) {
			String primitive = primitivesForClasses.get(typeName);
			return (primitive != null) ? primitive : formatJavaType(typeName);
		} else {
			String primitiveClass = classesForPrimitives.get(typeName);
			return (primitiveClass != null) ? formatJavaType(primitiveClass) : formatJavaType(typeName);
		}
	}

	/**
	 * Removes registered package and non-reserved registered type name prefixes
	 * from java types
	 */
	public String formatJavaType(String typeName) {
		if (typeName != null) {
			StringUtils stringUtils = StringUtils.getInstance();
			String typeClassName = stringUtils.stripPackageName(typeName);

			if (!reservedImportTypesMap.containsKey(typeClassName)
                && importTypesMap.containsKey(typeClassName)
                    && typeName.equals(importTypesMap.get(typeClassName))) {
			    return typeClassName;
			}

			String typePackageName = stringUtils.stripClass(typeName);
			if ("java.lang".equals(typePackageName)) {
                return typeClassName;
            }
			if ((null != packageName) && (packageName.equals(typePackageName))) {
                return typeClassName;
            }
		}

		return typeName;
	}

	/**
	 * @since 3.0
	 */
	public String formatJavaTypeAsNonBooleanPrimitive(String type) {
		String value = ImportUtils.classesForPrimitives.get(type);
		return formatJavaType(value != null ? value : type);
	}

	/**
	 * @since 3.0
	 */
	public boolean isNonBooleanPrimitive(String type) {
		return ImportUtils.classesForPrimitives.containsKey(type) && !isBoolean(type);
	}

	/**
	 * @since 4.2
	 */
	public boolean isNumericPrimitive(String type) {
		return isNonBooleanPrimitive(type) && !"char".equals(type);
	}

	/**
	 * @since 3.0
	 */
	public boolean isBoolean(String type) {
		return "boolean".equals(type);
	}

	/**
	 * @since 4.1
	 * @param type name
	 * @return is given type primitive
	 */
	public boolean isPrimitive(String type) {
		return classesForPrimitives.containsKey(type);
	}

	/**
	 *
	 * This method decide can primitive type be used for given attribute.
	 * It can be used in following cases:
	 * 		- attribute is PK and primitive
	 * 		- attribute not PK and is mandatory
	 *
	 * @param attribute to check
	 * @return can primitive Java type be used
	 *
	 * @since 4.1
	 */
	public boolean canUsePrimitive(ObjAttribute attribute) {
        return !attribute.isLazy() && attribute.isMandatory() && isPrimitive(attribute.getType());
    }

	public boolean canUsePrimitive(EmbeddableAttribute attribute) {
		return isPrimitive(attribute.getType());
	}

	/**
	 * Generate package and list of import statements based on the registered
	 * types.
	 */
	public String generate() {
		StringBuilder outputBuffer = new StringBuilder();

		if (null != packageName) {
			outputBuffer.append("package ");
			outputBuffer.append(packageName);

			// Using UNIX line endings intentionally - generated Java files should look
			// the same regardless of platform to prevent developer teams working on
			// multiple OS's to override each other's work
			outputBuffer.append(";\n\n");
		}

		List<String> typesList = new ArrayList<>(importTypesMap.values());
		typesList.sort((s1, s2) -> {
            for (String ordering : importOrdering) {
                if ((s1.startsWith(ordering)) && (!s2.startsWith(ordering))) {
                    return -1;
                }
                if ((!s1.startsWith(ordering)) && (s2.startsWith(ordering))) {
                    return 1;
                }
            }

            return s1.compareTo(s2);
        });

		String lastStringPrefix = null;
		boolean firstIteration = true;
		for (String typeName : typesList) {

			if (firstIteration) {
				firstIteration = false;
			} else {
				outputBuffer.append('\n');
			}
			// Output another newline if we're in a different root package.
			// Find root package
			String thisStringPrefix = typeName;
			int dotIndex = typeName.indexOf('.');
			if (-1 != dotIndex) {
				thisStringPrefix = typeName.substring(0, dotIndex);
			}
			// if this isn't the first import,
			if (null != lastStringPrefix) {
				// and it's different from the last import
				if (!thisStringPrefix.equals(lastStringPrefix)) {
					// output a newline; force UNIX style per comment above
					outputBuffer.append("\n");
				}
			}
			lastStringPrefix = thisStringPrefix;

			outputBuffer.append("import ");
			outputBuffer.append(typeName);
			outputBuffer.append(';');
		}

		return outputBuffer.toString();
	}

	/**
	 * @param attribute db attribute
	 * @return name of the java type
	 *
	 * @since 4.1
	 */
	public String dbAttributeToJavaType(DbAttribute attribute) {
		String javaTypeName = TypesMapping.getJavaBySqlType(attribute);
		return formatJavaType(javaTypeName);
	}
}

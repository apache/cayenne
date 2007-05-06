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


package org.apache.cayenne.gen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.util.Util;

/**
 * Methods for mangling strings.
 * 
 * @author Mike Kienenberger
 */
public class ImportUtils {

    public static final String importOrdering[] = new String[] {
        "java.", "javax.", "org.", "com." };

    static final String primitives[] = new String[] {
            "long", "double", "byte", "boolean", "float", "short", "int"
    };
    
    static final String primitiveClasses[] = new String[] {
            Long.class.getName(), Double.class.getName(), Byte.class.getName(),
            Boolean.class.getName(), Float.class.getName(), Short.class.getName(),
            Integer.class.getName()
    };
    
    static Map classesForPrimitives = Util.toMap(primitives, primitiveClasses);
    static Map primitivesForClasses = Util.toMap(primitiveClasses, primitives);

    protected Map importTypesMap = new HashMap();
    protected Map reservedImportTypesMap = new HashMap();  // Types forced to be FQN
    
    protected String packageName;
    
    public ImportUtils()
    {
        super();
    }
    
    protected boolean canRegisterType(String typeName)
    {
        // Not sure why this would ever happen, but it did
        if (null == typeName)  return false;
        
        StringUtils stringUtils = StringUtils.getInstance();
        String typeClassName = stringUtils.stripPackageName(typeName);
        String typePackageName = stringUtils.stripClass(typeName);
        
        if (typePackageName.length() == 0)  return false; // disallow non-packaged types (primatives, probably)
        if ("java.lang".equals(typePackageName))  return false;
        
        // Can only have one type -- rest must use fqn
        if (reservedImportTypesMap.containsKey(typeClassName))  return false;
        if (importTypesMap.containsKey(typeClassName))  return false;
        
        return true;
    }
    
    /**
     * Reserve a fully-qualified data type class name so it cannot be used by another class.
     * No import statements will be generated for reserved types.
     * Typically, this is the fully-qualified class name of the class being generated.
     * @param typeName FQ data type class name.
     */
    public void addReservedType(String typeName)
    {
        if (! canRegisterType(typeName))  return;
        
        StringUtils stringUtils = StringUtils.getInstance();
        String typeClassName = stringUtils.stripPackageName(typeName);
        
        reservedImportTypesMap.put(typeClassName, typeName);
    }
    
    /**
     * Register a fully-qualified data type class name.
     * For example, org.apache.cayenne.CayenneDataObject
     * @param typeName FQ data type class name.
     */
    public void addType(String typeName)
    {
        if (! canRegisterType(typeName))  return;
        
        StringUtils stringUtils = StringUtils.getInstance();
        String typePackageName = stringUtils.stripClass(typeName);
        String typeClassName = stringUtils.stripPackageName(typeName);
        
        if (typePackageName.equals(packageName))  return;

        importTypesMap.put(typeClassName, typeName);
    }
    
    /**
     * Add the package name to use for this importUtil invocation.
     * @param packageName
     */
    public void setPackage(String packageName)
    {
        this.packageName = packageName;
    }
    
    /**
     * Performs processing similar to <code>formatJavaType(String)</code>, with special
     * handling of primitive types and their Java class counterparts. This method allows
     * users to make a decision whether to use primitives or not, regardless of how type
     * is mapped.
     */
    public String formatJavaType(String typeName, boolean usePrimitives) {
        if (usePrimitives) {
            String primitive = (String) primitivesForClasses.get(typeName);
            return (primitive != null) ? primitive : formatJavaType(typeName);
        }
        else {
            String primitiveClass = (String) classesForPrimitives.get(typeName);
            return (primitiveClass != null)
                    ? formatJavaType(primitiveClass)
                    : formatJavaType(typeName);
        }
    }
    
    /**
     * Removes registered package and non-reserved registered type name prefixes from java types 
     */
    public String formatJavaType(String typeName) {
        if (typeName != null) {
            StringUtils stringUtils = StringUtils.getInstance();
            String typeClassName = stringUtils.stripPackageName(typeName);
            
            if (! reservedImportTypesMap.containsKey(typeClassName))
            {
                if (importTypesMap.containsKey(typeClassName))
                {
                    if (typeName.equals(importTypesMap.get(typeClassName)))  return typeClassName;
                }
            }
            
            String typePackageName = stringUtils.stripClass(typeName);
            if ("java.lang".equals(typePackageName))  return typeClassName;
            if ((null != packageName) && (packageName.equals(typePackageName)))
            	return typeClassName;
        }

        return typeName;
    }
    
    /**
     * Generate package and list of import statements based on the registered types.
     */
    public String generate()
    {
        StringBuffer outputBuffer = new StringBuffer();

        if (null != packageName)
        {
            outputBuffer.append("package ");
            outputBuffer.append(packageName);
            outputBuffer.append(';');
            outputBuffer.append(System.getProperty("line.separator"));
            outputBuffer.append(System.getProperty("line.separator"));
        }

        List typesList = new ArrayList(importTypesMap.values());
        Collections.sort(typesList, new Comparator() {

            public int compare(Object o1, Object o2) {
                
                String s1 = (String)o1;
                String s2 = (String)o2;
                
                for (int index = 0; index < importOrdering.length; index++) {
                    String ordering = importOrdering[index];
                    
                    if ( (s1.startsWith(ordering)) && (!s2.startsWith(ordering)) )
                        return -1;
                    if ( (!s1.startsWith(ordering)) && (s2.startsWith(ordering)) )
                        return 1;
                }
                    
                return s1.compareTo(s2);
            }
        });
        
        String lastStringPrefix = null;
        Iterator typesIterator = typesList.iterator();
        while (typesIterator.hasNext()) {
            String typeName = (String)typesIterator.next();

            // Output another newline if we're in a different root package.
            // Find root package
            String thisStringPrefix = typeName;
            int dotIndex = typeName.indexOf('.');
            if (-1 != dotIndex)
            {
                thisStringPrefix = typeName.substring(0, dotIndex);
            }
            // if this isn't the first import,
            if (null != lastStringPrefix)
            {
                // and it's different from the last import
                if (false == thisStringPrefix.equals(lastStringPrefix))
                {
                    // output a newline
                    outputBuffer.append(System.getProperty("line.separator"));
                }
            }
            lastStringPrefix = thisStringPrefix;

            outputBuffer.append("import ");
            outputBuffer.append(typeName);
            outputBuffer.append(';');
            if (typesIterator.hasNext())
            {
                outputBuffer.append(System.getProperty("line.separator"));
            }
        }

        return outputBuffer.toString();
    }
}

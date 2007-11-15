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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.util.Util;

/**
 * Stores ExtendedTypes, implementing an algorithm to determine the right type for a given
 * Java class. See {@link #getRegisteredType(String)} documentation for lookup algorithm
 * details.
 * 
 * @author Andrus Adamchik
 */
// TODO: andrus 10/30/2007 - implement efficient synchronization. This class is 99% read
// and 1% write, so probably should use ConcurrentHashMap once we switch to Java 5.
public class ExtendedTypeMap {

    static final Map classesForPrimitives;

    static {
        classesForPrimitives = new HashMap();
        classesForPrimitives.put("long", Long.class.getName());
        classesForPrimitives.put("double", Double.class.getName());
        classesForPrimitives.put("byte", Byte.class.getName());
        classesForPrimitives.put("boolean", Boolean.class.getName());
        classesForPrimitives.put("float", Float.class.getName());
        classesForPrimitives.put("short", Short.class.getName());
        classesForPrimitives.put("int", Integer.class.getName());
    }

    protected final Map typeMap;
    protected DefaultType defaultType;

    Collection extendedTypeFactories;

    // standard type factories registered by Cayenne that are consulted after the user
    // factories.
    Collection internalTypeFactories;

    /**
     * Creates new ExtendedTypeMap, populating it with default JDBC-compatible types. If
     * JDK version is at least 1.5, also loads support for enumerated types.
     */
    public ExtendedTypeMap() {
        this.typeMap = new HashMap();
        this.defaultType = new DefaultType();

        initDefaultTypes();
        initDefaultFactories();
    }

    /**
     * Registers default extended types. This method is called from constructor.
     */
    protected void initDefaultTypes() {
        // void placeholder
        registerType(new VoidType());

        // register default types
        Iterator it = DefaultType.defaultTypes();
        while (it.hasNext()) {
            registerType(new DefaultType((String) it.next()));
        }
    }

    /**
     * Registers default factories for creating enum types and serializable types. Note
     * that user-defined factories are consulted before any default factory.
     * 
     * @since 3.0
     */
    protected void initDefaultFactories() {
        internalTypeFactories = new ArrayList(3);
        internalTypeFactories.add(new EnumTypeFactory());
        internalTypeFactories.add(new ByteOrCharArrayFactory(this));

        // note that Serializable type should be used as a last resort after all other
        // alternatives are exhausted.
        internalTypeFactories.add(new SerializableTypeFactory(this));
    }

    /**
     * Returns ExtendedTypeFactories registered with this instance.
     * 
     * @since 1.2
     */
    public Collection getFactories() {
        return extendedTypeFactories != null ? Collections
                .unmodifiableCollection(extendedTypeFactories) : Collections.EMPTY_SET;
    }

    /**
     * Adds an ExtendedTypeFactory that will be consulted if no direct mapping for a given
     * class exists. This feature can be used to map interfaces.
     * <p>
     * <i>Note that the order in which factories are added is important, as factories are
     * consulted in turn when an ExtendedType is looked up, and lookup is stopped when any
     * factory provides a non-null type.</i>
     * </p>
     * 
     * @since 1.2
     */
    public void addFactory(ExtendedTypeFactory factory) {
        if (factory == null) {
            throw new IllegalArgumentException("Attempt to add null factory");
        }

        if (extendedTypeFactories == null) {
            extendedTypeFactories = new ArrayList();
        }

        extendedTypeFactories.add(factory);
    }

    /**
     * Removes a factory from the registered factories if it was previously added.
     * 
     * @since 1.2
     */
    public void removeFactory(ExtendedTypeFactory factory) {
        if (factory != null && extendedTypeFactories != null) {
            // nullify for consistency
            if (extendedTypeFactories.remove(factory) && extendedTypeFactories.isEmpty()) {
                extendedTypeFactories = null;
            }
        }
    }

    /**
     * Adds a new type to the list of registered types. If there is another type
     * registered for a class described by the <code>type</code> argument, the old
     * handler is overridden by the new one.
     */
    public void registerType(ExtendedType type) {
        typeMap.put(type.getClassName(), type);

        // factory to handle subclasses of type.className
        addFactory(new SubclassTypeFactory(type));
    }

    /**
     * Returns a default ExtendedType that is used to handle unmapped types.
     */
    public ExtendedType getDefaultType() {
        return defaultType;
    }

    /**
     * Returns a guaranteed non-null ExtendedType instance for a given Java class name.
     * Primitive class names are internally replaced by the non-primitive counterparts.
     * The following lookup sequence is used to determine the type:
     * <ul>
     * <li>First the methods checks for an ExtendedType explicitly registered with the
     * map for a given class name (most common types are registered by Cayenne internally;
     * users can register their own).</li>
     * <li>Second, the method tries to obtain a type by iterating through
     * {@link ExtendedTypeFactory} instances registered by users. If a factory returns a
     * non-null type, it is returned to the user and the rest of the factories are
     * ignored. </li>
     * <li>Third, the method iterates through standard {@link ExtendedTypeFactory}
     * instances that can dynamically construct extended types for serializable objects
     * and JDK 1.5 enums.</li>
     * <li>If all the methods above failed, the default type is returned that relies on
     * default JDBC driver mapping to set and get objects.</li>
     * </ul>
     * <i>Note that for array types class name must be in the form 'MyClass[]'</i>.
     */
    public ExtendedType getRegisteredType(String javaClassName) {

        String nonPrimitive = (String) classesForPrimitives.get(javaClassName);
        if (nonPrimitive != null) {
            javaClassName = nonPrimitive;
        }

        ExtendedType type = getExplictlyRegisteredType(javaClassName);

        if (type != null) {
            return type;
        }

        type = createType(javaClassName);

        if (type != null) {
            // register to speed up future access
            registerType(type);
            return type;
        }

        return getDefaultType();
    }

    ExtendedType getExplictlyRegisteredType(String className) {
        return (ExtendedType) typeMap.get(className);
    }

    /**
     * Returns a type registered for the class name. If no such type exists, returns the
     * default type. It is guaranteed that this method returns a non-null ExtendedType
     * instance.
     */
    public ExtendedType getRegisteredType(Class javaClass) {
        String name = null;

        if (javaClass.isArray()) {
            // only support single dimensional arrays now
            name = javaClass.getComponentType().getName() + "[]";
        }
        else {
            name = javaClass.getName();
        }

        return getRegisteredType(name);
    }

    /**
     * Removes registered ExtendedType object corresponding to <code>javaClassName</code>
     * parameter.
     */
    public void unregisterType(String javaClassName) {
        typeMap.remove(javaClassName);
    }

    /**
     * Returns array of Java class names supported by Cayenne for JDBC mapping.
     */
    public String[] getRegisteredTypeNames() {
        Set keys = typeMap.keySet();
        int len = keys.size();
        String[] types = new String[len];

        Iterator it = keys.iterator();
        for (int i = 0; i < len; i++) {
            types[i] = (String) it.next();
        }

        return types;
    }

    /**
     * @deprecated since 3.0 - use {@link #createType(String)} instead.
     * @since 1.2
     */
    protected ExtendedType getDefaultType(String javaClassName) {
        return createType(javaClassName);
    }

    /**
     * Returns an ExtendedType for specific Java classes. Uses user-provided and
     * Cayenne-provided {@link ExtendedTypeFactory} factories to instantiate the
     * ExtendedType. All primitive classes must be converted to the corresponding Java
     * classes by the callers.
     * 
     * @return a default type for a given class or null if a class has no default type
     *         mapping.
     * @since 1.2
     */
    protected ExtendedType createType(String className) {

        if (className == null) {
            return null;
        }

        Class typeClass;
        try {
            typeClass = Util.getJavaClass(className);
        }
        catch (Throwable th) {
            // ignore exceptions...
            return null;
        }

        // lookup in user factories first
        if (extendedTypeFactories != null) {

            Iterator it = extendedTypeFactories.iterator();
            while (it.hasNext()) {
                ExtendedTypeFactory factory = (ExtendedTypeFactory) it.next();

                ExtendedType type = factory.getType(typeClass);
                if (type != null) {
                    return type;
                }
            }
        }

        // lookup in internal factories

        Iterator it = internalTypeFactories.iterator();
        while (it.hasNext()) {
            ExtendedTypeFactory factory = (ExtendedTypeFactory) it.next();

            ExtendedType type = factory.getType(typeClass);
            if (type != null) {
                return type;
            }
        }

        return null;
    }
}

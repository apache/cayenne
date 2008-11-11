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

package org.apache.cayenne.jpa.map;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.enhancer.EnhancementHelper;

/**
 * Provides information about a class relevant to JPA, such potential persistence fields,
 * etc.
 * 
 */
public class JpaClassDescriptor {

    private static final Pattern GETTER_PATTERN = Pattern
            .compile("^(is|get)([A-Z])(.*)$");
    private static final Pattern SETTER_PATTERN = Pattern.compile("^set([A-Z])(.*)$");

    static final Set<String> reservedProperties;

    static {
        reservedProperties = new HashSet<String>();
        reservedProperties.add(propertyKey("objectId", ObjectId.class));
        reservedProperties.add(propertyKey("persistenceState", Integer.TYPE));
        reservedProperties.add(propertyKey("objectContext", ObjectContext.class));
    }

    protected Collection<JpaPropertyDescriptor> fieldDescriptors;
    protected Collection<JpaPropertyDescriptor> propertyDescriptors;
    protected Class<?> managedClass;
    protected AccessType access;

    static String propertyKey(String propertyName, Class<?> propertyType) {
        return propertyName + ':' + propertyType.getName();
    }

    public static String propertyNameForGetter(String getterName) {
        Matcher getMatch = GETTER_PATTERN.matcher(getterName);
        if (getMatch.matches()) {
            return getMatch.group(2).toLowerCase() + getMatch.group(3);
        }

        return null;
    }

    public static String propertyNameForSetter(String setterName) {
        Matcher setMatch = SETTER_PATTERN.matcher(setterName);

        if (setMatch.matches()) {
            return setMatch.group(1).toLowerCase() + setMatch.group(2);
        }

        return null;
    }

    public JpaClassDescriptor(Class<?> managedClass) {
        this.managedClass = managedClass;
    }

    public Class<?> getManagedClass() {
        return managedClass;
    }

    public AccessType getAccess() {
        return access;
    }

    public void setAccess(AccessType access) {
        this.access = access;
    }

    /**
     * Returns descriptor matching the property name. If the underlying entity map uses
     * FIELD access, a descriptor is looked up in the list of class fields, if it uses
     * PROPERTY access - descriptor is looked up in the list of class properties.
     */
    public JpaPropertyDescriptor getProperty(String name) {
        if (getAccess() == AccessType.FIELD) {
            for (JpaPropertyDescriptor d : getFieldDescriptors()) {
                if (name.equals(d.getName())) {
                    return d;
                }
            }
        }
        else if (getAccess() == AccessType.PROPERTY) {
            for (JpaPropertyDescriptor d : getPropertyDescriptors()) {
                if (name.equals(d.getName())) {
                    return d;
                }
            }
        }

        return null;
    }

    /**
     * Returns descriptor matching the property name. Note that entity map access type is
     * ignored and instead field vs. property descriptor is determined from the member
     * type.
     */
    public JpaPropertyDescriptor getPropertyForMember(Member classMember) {
        if (classMember instanceof Field) {
            for (JpaPropertyDescriptor d : getFieldDescriptors()) {
                if (d.getMember().equals(classMember)) {
                    return d;
                }
            }
        }
        else if (classMember instanceof Method) {
            for (JpaPropertyDescriptor d : getPropertyDescriptors()) {
                if (d.getMember().equals(classMember)) {
                    return d;
                }
            }
        }

        return null;
    }

    public Collection<JpaPropertyDescriptor> getFieldDescriptors() {
        if (fieldDescriptors == null) {
            compileFields();
        }

        return fieldDescriptors;
    }

    /**
     * Returns getters for public and protected methods that look like read/write bean
     * properties, as those are potential persistent properties.
     */
    public Collection<JpaPropertyDescriptor> getPropertyDescriptors() {
        if (propertyDescriptors == null) {
            compileProperties();
        }

        return propertyDescriptors;
    }

    protected void compileFields() {

        Field[] fields = managedClass.getDeclaredFields();
        fieldDescriptors = new ArrayList<JpaPropertyDescriptor>(fields.length);

        for (int i = 0; i < fields.length; i++) {

            int modifiers = fields[i].getModifiers();
            // skip transient fields (in a Java sense)
            if (Modifier.isTransient(modifiers)) {
                continue;
            }

            // skip static fields
            if (Modifier.isStatic(modifiers)) {
                continue;
            }

            // skip fields created by Cayenne enhancer
            if (EnhancementHelper.isGeneratedField(fields[i].getName())) {
                continue;
            }

            fieldDescriptors.add(new JpaPropertyDescriptor(fields[i]));
        }
    }

    protected void compileProperties() {

        Map<String, PropertyTuple> properties = new HashMap<String, PropertyTuple>();

        // per JPA spec, 2.1.1:
        // The property accessor methods must be public or protected. When
        // property-based access is used, the object/relational mapping annotations for
        // the entity class annotate the getter property accessors.

        // JPA Spec, 2.1.9.3, regarding non-entity superclasses:

        // The non-entity superclass serves for inheritance of behavior only. The state of
        // a non-entity superclass is not persistent. Any state inherited from non-entity
        // superclasses is non-persistent in an inheriting entity class. This
        // non-persistent state is not managed by the EntityManager, nor it is
        // required to be retained across transactions. Any annotations on such
        // superclasses are ignored.

        Method[] methods = managedClass.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {

            int modifiers = methods[i].getModifiers();
            if (!Modifier.isProtected(modifiers) && !Modifier.isPublic(modifiers)) {
                continue;
            }

            String name = methods[i].getName();
            Class<?>[] parameters = methods[i].getParameterTypes();
            Class<?> returnType = methods[i].getReturnType();
            boolean isVoid = Void.TYPE.equals(returnType);

            if (!isVoid && parameters.length == 0) {
                String propertyName = propertyNameForGetter(name);

                if (propertyName != null) {
                    String key = propertyKey(propertyName, returnType);

                    if (reservedProperties.contains(key)) {
                        continue;
                    }

                    PropertyTuple t = properties.get(key);
                    if (t == null) {
                        t = new PropertyTuple();
                        properties.put(key, t);
                    }

                    t.getter = methods[i];
                    t.name = propertyName;
                    continue;
                }
            }

            if (isVoid && parameters.length == 1) {
                String propertyName = propertyNameForSetter(name);

                if (propertyName != null) {

                    String key = propertyName + ":" + parameters[0].getName();

                    PropertyTuple t = properties.get(key);
                    if (t == null) {
                        t = new PropertyTuple();
                        properties.put(key, t);
                    }

                    t.setter = methods[i];
                }
            }
        }

        this.propertyDescriptors = new ArrayList<JpaPropertyDescriptor>(properties.size());

        for (PropertyTuple t : properties.values()) {
            if (t.getter != null && t.setter != null) {
                propertyDescriptors.add(new JpaPropertyDescriptor(t.getter, t.name));
            }
        }
    }

    static final class PropertyTuple {

        String name;
        Method getter;
        Method setter;
    }
}

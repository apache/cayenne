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

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

import org.apache.cayenne.jpa.JpaProviderException;

/**
 * A descriptor of a class property that may or may not be persistent.
 * 
 */
public class JpaPropertyDescriptor {

    protected AnnotatedElement member;
    protected String name;
    protected Class<?> type;
    protected Type genericType;
    protected Class<?> targetEntityType;

    public JpaPropertyDescriptor(Field field) {
        this.member = field;
        this.name = field.getName();
        this.type = field.getType();
        initTargetEntityType(field.getGenericType());
    }

    public JpaPropertyDescriptor(Method getter, String name) {

        if (JpaClassDescriptor.propertyNameForGetter(getter.getName()) == null) {
            throw new JpaProviderException("Invalid property getter name: "
                    + getter.getName());
        }

        this.member = getter;
        this.name = name;
        this.type = getter.getReturnType();
        initTargetEntityType(getter.getGenericReturnType());
    }

    protected void initTargetEntityType(Type genericType) {

        this.targetEntityType = Void.TYPE;

        if (Collection.class.isAssignableFrom(type)) {
            if (genericType instanceof ParameterizedType) {
                ParameterizedType pType = (ParameterizedType) genericType;
                Type[] types = pType.getActualTypeArguments();

                if (types.length == 1 && types[0] instanceof Class) {
                    this.targetEntityType = (Class<?>) types[0];
                    return;
                }
            }
        }
        else if (Map.class.isAssignableFrom(type)) {

            if (genericType instanceof ParameterizedType) {
                ParameterizedType pType = (ParameterizedType) genericType;
                Type[] types = pType.getActualTypeArguments();

                if (types.length == 2 && types[1] instanceof Class) {
                    this.targetEntityType = (Class<?>) types[1];
                    return;
                }
            }
        }
        else {
            targetEntityType = type;
        }
    }

    public AnnotatedElement getMember() {
        return member;
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public Class<?> getTargetEntityType() {
        return Void.TYPE.equals(targetEntityType) ? null : targetEntityType;
    }

    public boolean isStringType() {
        return String.class.isAssignableFrom(type);
    }

    /**
     * Returns true if the property is a default simple attribute.
     * <h3>JPA Spec, 2.1.6:</h3>
     * If the type of the field or property is one of the following, it is mapped in the
     * same way as it would if it were annotated as Basic: Java primitive types, wrappers
     * of the primitive types, java.lang.String, java.math.BigInteger,
     * java.math.BigDecimal, java.util.Date, java.util.Calendar, java.sql.Date,
     * java.sql.Time, java.sql.Timestamp, byte[], Byte[], char[], Character[], enums, any
     * other type that implements Serializable. See Sections 9.1.16 through 9.1.19. It is
     * an error if no annotation is present and none of the above rules apply.
     */
    public boolean isDefaultNonRelationalType() {
        return isDefaultNonRelationalType(getTargetEntityType());
    }

    boolean isDefaultNonRelationalType(Class<?> type) {

        if (type.isPrimitive() || type.isEnum()) {
            return true;
        }

        if (type.isArray()) {
            return isDefaultNonRelationalType(type.getComponentType());
        }

        // it is sufficient to check serializability as all the types mentioned in the
        // spec are serializable
        return Serializable.class.isAssignableFrom(type);
    }
}

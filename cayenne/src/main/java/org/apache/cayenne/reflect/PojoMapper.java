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

package org.apache.cayenne.reflect;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.function.Function;

import org.apache.cayenne.CayenneRuntimeException;

/**
 * Simple mapper of Object[] to a POJO class.
 * Target class must have default constructor and at least as many fields as a processed array.
 * <br/>
 * <b>Note:</b> Current implementation relies on the field order,
 * so use with caution as this order may vary on different JDK platforms.
 * @param <T> type of object to produce
 *
 * @see org.apache.cayenne.query.ColumnSelect#map(Function)
 * @see org.apache.cayenne.query.SQLSelect#map(Function)
 *
 * @since 4.2
 */
public class PojoMapper<T> implements Function<Object[], T> {

    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

    private final Class<T> type;
    private final MethodHandle constructor;
    private final MethodHandle[] setters;

    public PojoMapper(Class<T> type) {
        this.type = type;
        try {
            this.constructor = lookup.unreflectConstructor(type.getConstructor());
        } catch (NoSuchMethodException | IllegalAccessException ex) {
            throw new CayenneRuntimeException("No default constructor found for class '%s'.", type.getName());
        }

        Field[] declaredFields = type.getDeclaredFields();
        this.setters = new MethodHandle[declaredFields.length];
        int i = 0;
        for(Field field : declaredFields) {
            field.setAccessible(true);
            try {
                setters[i++] = lookup.unreflectSetter(field);
            } catch (IllegalAccessException e) {
                throw new CayenneRuntimeException("Field '%s'.'%s' is inaccessible.", e, type.getName(), field.getName());
            }
        }
    }

    private T newObject() {
        try {
            @SuppressWarnings("unchecked")
            T object = (T)constructor.invoke();
            return object;
        } catch (Throwable ex) {
            throw new CayenneRuntimeException("Unable to instantiate %s.", ex, type.getName());
        }
    }

    public T apply(Object[] data) {
        if(data.length > setters.length) {
            throw new CayenneRuntimeException("Unable to create '%s'. Values length (%d) > fields count (%d)"
                    , type.getName(), data.length, setters.length);
        }

        T object = newObject();

        for (int i = 0; i < data.length; i++) {
            if (data[i] != null) {
                try {
                    setters[i].invoke(object, data[i]);
                } catch (Throwable ex) {
                    throw new CayenneRuntimeException("Unable to set field of %s.", ex, type.getName());
                }
            }
        }

        return object;
    }
}

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
package org.apache.cayenne.configuration;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.util.Util;

/**
 * A default implementation of {@link AdhocObjectFactory} that creates objects using
 * default no-arg constructor and injects dependencies into annotated fields. Note that
 * constructor injection is not supported by this factory.
 * 
 * @since 3.1
 */
public class DefaultAdhocObjectFactory implements AdhocObjectFactory {

    @Inject
    protected Injector injector;

    public <T> T newInstance(Class<? super T> superType, String className) {

        if (superType == null) {
            throw new NullPointerException("Null superType");
        }

        if (className == null) {
            throw new NullPointerException("Null className");
        }

        Class<T> type;
        try {
            type = (Class<T>) Util.getJavaClass(className);
        }
        catch (ClassNotFoundException e) {
            throw new CayenneRuntimeException(
                    "Invalid class %s of type %s",
                    e,
                    className,
                    superType.getName());
        }

        if (!superType.isAssignableFrom(type)) {
            throw new CayenneRuntimeException(
                    "Class %s is not assignable to %s",
                    className,
                    superType.getName());
        }

        T instance;
        try {
            instance = type.newInstance();
        }
        catch (Exception e) {
            throw new CayenneRuntimeException(
                    "Error creating instance of class %s of type %s",
                    e,
                    className,
                    superType.getName());
        }

        injector.injectMembers(instance);
        return instance;
    }

}

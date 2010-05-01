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
package org.apache.cayenne.di;

/**
 * An object passed to a {@link Module} by the DI container during initialization, that
 * provides the API for the module to bind its services to the container.
 * 
 * @since 3.1
 */
public interface Binder {

    /**
     * Starts an unnamed binding of a specific interface. Binding should continue using
     * returned BindingBuilder.
     */
    <T> BindingBuilder<T> bind(Class<T> interfaceType);

    /**
     * Starts a binding of a specific interface based on a provided binding key. This
     * method is more generic than {@link #bind(Class)} and allows to create named
     * bindings in addition to default ones. Binding should continue using returned
     * BindingBuilder.
     */
    <T> BindingBuilder<T> bind(Key<T> key);

    /**
     * Starts a binding of a map "configuration" that will be injected into an
     * implementation class for the specified "interfaceType" parameter. Configurations
     * can only be injected via a constructor. An object can take at most one
     * configuration object via a constructor.
     */
    <T> MapBuilder<T> bindMap(Class<T> interfaceType);

    /**
     * Starts a binding of a list "configuration" that will be injected into an
     * implementation class for the specified "interfaceType" parameter. Configurations
     * can only be injected via a constructor. An object can take at most one
     * configuration object via a constructor.
     */
    <T> ListBuilder<T> bindList(Class<T> interfaceType);
}

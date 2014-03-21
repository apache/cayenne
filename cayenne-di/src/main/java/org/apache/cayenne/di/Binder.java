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
 * provides the API for the module to bind its services to the container. Note that the
 * default {@link Scope} of the bound objects is normally "singleton" and can be changed
 * to "no scope" or a custom scope via a corresponding method of a binding builder. E.g.
 * see {@link BindingBuilder#in(Scope)}.
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
     * Starts a binding of a java.util.Map&lt;String, ?&gt; distinguished by its binding name.
     * Map binding should continue using returned MapBuilder. This is somewhat equivalent
     * of using "bind(Map.class, bindingName)", however returned MapBuilder provides extra
     * DI capabilities.
     */
    <T> MapBuilder<T> bindMap(String bindingName);

    /**
     * Starts a binding of a java.util.List&lt;?&gt; distinguished by its binding name. List
     * binding should continue using returned ListBuilder. This is somewhat equivalent of
     * using "bind(List.class, bindingName)", however returned ListBuilder provides extra
     * DI capabilities.
     */
    <T> ListBuilder<T> bindList(String bindingName);
    
    /**
     * @since 3.2
     */
    <T> DecoratorBuilder<T> decorate(Class<T> interfaceType);
    
    /**
     * @since 3.2
     */
    <T> DecoratorBuilder<T> decorate(Key<T> key);
}

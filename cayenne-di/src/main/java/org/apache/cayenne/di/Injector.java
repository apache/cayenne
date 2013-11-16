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

import org.apache.cayenne.ConfigurationException;

/**
 * A facade to the Cayenne DI container. To create an injector use {@link DIBootstrap}
 * static methods.
 * 
 * @since 3.1
 */
public interface Injector {

    /**
     * Returns a service instance bound in the container for a specific type. Throws
     *{@link ConfigurationException} if the type is not bound, or an instance can not be
     * created.
     */
    <T> T getInstance(Class<T> type) throws ConfigurationException;

    /**
     * Returns a service instance bound in the container for a specific binding key.
     * Throws {@link ConfigurationException} if the key is not bound, or an instance can
     * not be created.
     */
    <T> T getInstance(Key<T> key) throws ConfigurationException;

    <T> Provider<T> getProvider(Class<T> type) throws ConfigurationException;

    <T> Provider<T> getProvider(Key<T> key) throws ConfigurationException;

    /**
     * Performs field injection on a given object, ignoring constructor injection. Since
     * Cayenne DI injector returns fully injected objects, this method is rarely used
     * directly.
     * <p>
     * Note that using this method inside a custom DI {@link Provider} will most likely
     * result in double injection, as custom provider is wrapped in a field-injecting
     * provider by the DI container. Instead custom providers must initialize object
     * properties manually, obtaining dependencies from Injector.
     */
    void injectMembers(Object object);

    /**
     * A lifecycle method that let's the injector's services to clean up their state and
     * release resources. This method would normally generate a scope end event for the
     * injector's one and only singleton scope.
     */
    void shutdown();
}

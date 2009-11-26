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
package org.apache.cayenne.di.spi;

import org.apache.cayenne.di.DIException;
import org.apache.cayenne.di.Provider;

/**
 * An injecting provider for custom providers.
 * 
 * @since 3.1
 */
class ConstructorInjectingProvidersProvider<T> implements Provider<Provider<? extends T>> {

    private Class<? extends Provider<? extends T>> providerType;

    ConstructorInjectingProvidersProvider(
            Class<? extends Provider<? extends T>> providerType) {
        this.providerType = providerType;
    }

    public Provider<? extends T> get() throws DIException {
        try {
            // TODO: constructor injection in provider?
            return providerType.newInstance();
        }
        catch (Exception e) {
            throw new DIException("Error instantiating provider '%s'", e, providerType
                    .getName());
        }
    }
}

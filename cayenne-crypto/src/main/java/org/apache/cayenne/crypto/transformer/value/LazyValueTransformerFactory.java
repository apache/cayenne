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
package org.apache.cayenne.crypto.transformer.value;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.map.DbAttribute;

/**
 * A {@link ValueTransformerFactory} that  allows to defer its initialization and hence allows Cayenne stack
 * to operate without cryto keys over a subset of entities that do not require encryption. Useful e.g. when the
 * crypto keys are supplied at a later point after startup.
 *
 * @since 4.0
 */
public class LazyValueTransformerFactory implements ValueTransformerFactory {

    private Provider<ValueTransformerFactory> delegateProvider;
    private volatile ValueTransformerFactory delegate;

    public LazyValueTransformerFactory(@Inject Provider<ValueTransformerFactory> delegateProvider) {
        this.delegateProvider = delegateProvider;
    }

    @Override
    public ValueEncryptor encryptor(DbAttribute a) {
        return ensureInit().encryptor(a);
    }

    @Override
    public ValueDecryptor decryptor(DbAttribute a) {
        return ensureInit().decryptor(a);
    }

    protected ValueTransformerFactory ensureInit() {

        if (delegate == null) {
            synchronized (this) {
                if (delegate == null) {
                    delegate = delegateProvider.get();
                }
            }
        }

        return delegate;
    }

}

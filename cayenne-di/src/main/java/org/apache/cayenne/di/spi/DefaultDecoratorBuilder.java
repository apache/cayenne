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

import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.DecoratorBuilder;
import org.apache.cayenne.di.Key;

/**
 * @since 3.2
 */
class DefaultDecoratorBuilder<T> implements DecoratorBuilder<T> {

    private Key<T> bindingKey;
    private DefaultInjector injector;

    DefaultDecoratorBuilder(Key<T> bindingKey, DefaultInjector injector) {
        this.bindingKey = bindingKey;
        this.injector = injector;
    }

    @Override
    public DecoratorBuilder<T> after(Class<? extends T> decoratorImplementationType) throws DIRuntimeException {
        injector.putDecorationAfter(bindingKey, decoratorProvider(decoratorImplementationType));
        return this;
    }

    @Override
    public DecoratorBuilder<T> before(Class<? extends T> decoratorImplementationType) throws DIRuntimeException {
        injector.putDecorationBefore(bindingKey, decoratorProvider(decoratorImplementationType));
        return this;
    }

    private DecoratorProvider<T> decoratorProvider(Class<? extends T> decoratorType) {
        DecoratorProvider<T> provider0 = new ConstructorInjectingDecoratorProvider<T>(decoratorType, injector);
        DecoratorProvider<T> provider1 = new FieldInjectingDecoratorProvider<T>(decoratorType, provider0, injector);
        return provider1;
    }

}

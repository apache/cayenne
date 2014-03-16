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

import java.util.List;
import java.util.Map;

import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.BindingBuilder;
import org.apache.cayenne.di.DecoratorBuilder;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.MapBuilder;

/**
 * @since 3.1
 */
class DefaultBinder implements Binder {

    private DefaultInjector injector;

    DefaultBinder(DefaultInjector injector) {
        this.injector = injector;
    }

    @Override
    public <T> BindingBuilder<T> bind(Class<T> interfaceType) {
        return new DefaultBindingBuilder<T>(Key.get(interfaceType), injector);
    }

    @Override
    public <T> BindingBuilder<T> bind(Key<T> key) {
        return new DefaultBindingBuilder<T>(key, injector);
    }

    @Override
    public <T> ListBuilder<T> bindList(String bindingName) {
        Class<?> listClass = List.class;
        return new DefaultListBuilder<T>(
                Key.get((Class<List<?>>) listClass, bindingName),
                injector);
    }

    @Override
    public <T> MapBuilder<T> bindMap(String bindingName) {
        Class<?> mapClass = Map.class;
        return new DefaultMapBuilder<T>(Key.get(
                (Class<Map<String, ?>>) mapClass,
                bindingName), injector);
    }
    
    @Override
    public <T> DecoratorBuilder<T> decorate(Class<T> interfaceType) {
        return new DefaultDecoratorBuilder<T>(Key.get(interfaceType), injector);
    }
    
    @Override
    public <T> DecoratorBuilder<T> decorate(Key<T> key) {
        return new DefaultDecoratorBuilder<T>(key, injector);
    }
}

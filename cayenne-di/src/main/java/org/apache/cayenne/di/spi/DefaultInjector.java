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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.di.Scope;

/**
 * A default Cayenne implementations of a DI injector.
 * 
 * @since 3.1
 */
public class DefaultInjector implements Injector {

    private DefaultScope singletonScope;
    private Scope noScope;

    private Map<Key<?>, Binding<?>> bindings;
    private Map<Key<?>, Decoration<?>> decorations;
    private InjectionStack injectionStack;
    private Scope defaultScope;

    public DefaultInjector(Module... modules) throws DIRuntimeException {

        this.singletonScope = new DefaultScope();
        this.noScope = NoScope.INSTANCE;

        // this is intentionally hardcoded and is not configurable
        this.defaultScope = singletonScope;

        this.bindings = new HashMap<Key<?>, Binding<?>>();
        this.decorations = new HashMap<Key<?>, Decoration<?>>();
        this.injectionStack = new InjectionStack();

        DefaultBinder binder = new DefaultBinder(this);

        // bind self for injector injection...
        binder.bind(Injector.class).toInstance(this);

        // bind modules
        if (modules != null && modules.length > 0) {

            for (Module module : modules) {
                module.configure(binder);
            }
            
            applyDecorators();
        }
    }

    InjectionStack getInjectionStack() {
        return injectionStack;
    }

    <T> Binding<T> getBinding(Key<T> key) throws DIRuntimeException {

        if (key == null) {
            throw new NullPointerException("Null key");
        }

        // may return null - this is intentionally allowed in this non-public method
        return (Binding<T>) bindings.get(key);
    }

    <T> void putBinding(Key<T> bindingKey, Provider<T> provider) {
        // TODO: andrus 11/15/2009 - report overriding existing binding??
        bindings.put(bindingKey, new Binding<T>(provider, defaultScope));
    }
    
    <T> void putDecorationAfter(Key<T> bindingKey, DecoratorProvider<T> decoratorProvider) {

        Decoration<T> decoration = (Decoration<T>) decorations.get(bindingKey);
        if (decoration == null) {
            decoration = new Decoration<T>();
            decorations.put(bindingKey, decoration);
        }

        decoration.after(decoratorProvider);
    }
    
    <T> void putDecorationBefore(Key<T> bindingKey, DecoratorProvider<T> decoratorProvider) {

        Decoration<T> decoration = (Decoration<T>) decorations.get(bindingKey);
        if (decoration == null) {
            decoration = new Decoration<T>();
            decorations.put(bindingKey, decoration);
        }

        decoration.before(decoratorProvider);
    }

    <T> void changeBindingScope(Key<T> bindingKey, Scope scope) {
        if (scope == null) {
            scope = noScope;
        }

        Binding<?> binding = bindings.get(bindingKey);
        if (binding == null) {
            throw new DIRuntimeException("No existing binding for key " + bindingKey);
        }

        binding.changeScope(scope);
    }

    @Override
    public <T> T getInstance(Class<T> type) throws DIRuntimeException {
        return getProvider(type).get();
    }

    @Override
    public <T> T getInstance(Key<T> key) throws DIRuntimeException {
        return getProvider(key).get();
    }

    @Override
    public <T> Provider<T> getProvider(Class<T> type) throws DIRuntimeException {
        return getProvider(Key.get(type));
    }

    @Override
    public <T> Provider<T> getProvider(Key<T> key) throws DIRuntimeException {

        if (key == null) {
            throw new NullPointerException("Null key");
        }

        Binding<T> binding = (Binding<T>) bindings.get(key);

        if (binding == null) {
            throw new DIRuntimeException(
                    "DI container has no binding for key %s",
                    key);
        }

        return binding.getScoped();
    }

    @Override
    public void injectMembers(Object object) {
        Provider<Object> provider0 = new InstanceProvider<Object>(object);
        Provider<Object> provider1 = new FieldInjectingProvider<Object>(provider0, this);
        provider1.get();
    }

    @Override
    public void shutdown() {
        singletonScope.shutdown();
    }

    DefaultScope getSingletonScope() {
        return singletonScope;
    }

    Scope getNoScope() {
        return noScope;
    }
    
    void applyDecorators() {
        for (Entry<Key<?>, Decoration<?>> e : decorations.entrySet()) {

            Binding b = bindings.get(e.getKey());
            if (b == null) {
                // TODO: print warning - decorator of a non-existing service..
                continue;
            }

            b.decorate(e.getValue());
        }
    }
}

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

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.BeforeScopeEnd;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;

import java.util.Collection;
import java.util.Objects;

/**
 * A superclass of various Cayenne runtime stacks. A Runtime is the main access
 * point to Cayenne for a user application. It provides a default Cayenne
 * configuration as well as a way to customize this configuration via a built-in
 * dependency injection (DI) container. In fact implementation-wise, Runtime
 * object is just a convenience thin wrapper around a DI {@link Injector}.
 *
 * @since 3.1
 */
public abstract class CayenneRuntime {

    /**
     * A holder of an Injector bound to the current thread. Used mainly to allow
     * serializable contexts to attach to correct Cayenne stack on
     * deserialization.
     *
     * @since 3.1
     */
    protected static final ThreadLocal<Injector> threadInjector = new ThreadLocal<Injector>();

    /**
     * Binds a DI {@link Injector} bound to the current thread. It is primarily
     * intended for deserialization of ObjectContexts.
     *
     * @since 3.1
     */
    public static void bindThreadInjector(Injector injector) {
        threadInjector.set(injector);
    }

    /**
     * Returns the {@link Injector} bound to the current thread. Will return
     * null if none is bound.
     *
     * @since 3.1
     */
    public static Injector getThreadInjector() {
        return threadInjector.get();
    }

    protected Injector injector;
    protected Collection<Module> modules;

    /**
     * Creates a CayenneRuntime with configuration based on the supplied collection of DI modules.
     */
    protected CayenneRuntime(Collection<Module> modules) {
        this.modules = Objects.requireNonNull(modules);
        this.injector = DIBootstrap.createInjector(modules);
    }

    /**
     * Returns the collection of modules used to initialize this runtime.
     *
     * @since 4.0
     */
    public Collection<Module> getModules() {
        return modules;
    }

    /**
     * Returns DI injector used by this runtime.
     */
    public Injector getInjector() {
        return injector;
    }

    /**
     * Shuts down the DI injector of this runtime, giving all services that need
     * to release some resources a chance to do that.
     */
    // the following annotation is for environments that manage CayenneRuntimes
    // within
    // another DI registry (e.g. unit tests)
    @BeforeScopeEnd
    public void shutdown() {
        injector.shutdown();
    }

    /**
     * Returns the runtime {@link DataChannel}.
     */
    public DataChannel getChannel() {
        return injector.getInstance(DataChannel.class);
    }

    /**
     * Returns a new ObjectContext instance based on the runtime's main
     * DataChannel.
     *
     * @since 4.0
     */
    public ObjectContext newContext() {
        return injector.getInstance(ObjectContextFactory.class).createContext();
    }

    /**
     * Returns a new ObjectContext which is a child of the specified
     * DataChannel. This method is used for creation of nested ObjectContexts,
     * with parent ObjectContext passed as an argument.
     *
     * @since 4.0
     */
    public ObjectContext newContext(DataChannel parentChannel) {
        return injector.getInstance(ObjectContextFactory.class).createContext(parentChannel);
    }

    /**
     * @deprecated since 3.1 use better named {@link #newContext()} instead.
     */
    @Deprecated
    public ObjectContext getContext() {
        return newContext();
    }

    /**
     * @deprecated since 3.1 use better named {@link #newContext(DataChannel)}
     * instead.
     */
    @Deprecated
    public ObjectContext getContext(DataChannel parentChannel) {
        return newContext(parentChannel);
    }
}

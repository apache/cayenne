/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.runtime;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.configuration.ObjectContextFactory;
import org.apache.cayenne.di.BeforeScopeEnd;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.tx.TransactionDescriptor;
import org.apache.cayenne.tx.TransactionListener;
import org.apache.cayenne.tx.TransactionManager;
import org.apache.cayenne.tx.TransactionalOperation;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Objects;

/**
 * Object representing Cayenne stack. Serves as an entry point to Cayenne for user applications and a factory of ObjectContexts.
 * It provides a default Cayenne configuration as well as a way to customize this configuration via a built-in
 * dependency injection (DI) container.
 * In fact implementation-wise, Runtime object is just a convenience thin wrapper around a DI {@link Injector}.
 * <p>
 * To create CayenneRuntime use builder available with one of the {@link #builder()} methods:
 * <pre>
 * {@code
 * CayenneRuntime cayenneRuntime = CayenneRuntime.builder()
 *         .addConfig("cayenne-project.xml")
 *         .build();
 * }
 * </pre>
 *
 * @since 3.1 is introduced
 * @since 5.0 is repurposed as a single implementation of Cayenne runtime and moved to {@link org.apache.cayenne.runtime} package.
 *
 * @see #builder()
 * @see #builder(String)
 * @see CayenneRuntimeBuilder
 */
public class CayenneRuntime {

    /**
     * A holder of an Injector bound to the current thread. Used mainly to allow
     * serializable contexts to attach to correct Cayenne stack on
     * deserialization.
     *
     * @since 3.1
     */
    protected static final ThreadLocal<Injector> threadInjector = new ThreadLocal<>();

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

    protected final Injector injector;
    protected final Collection<Module> modules;

    /**
     * Creates a builder of CayenneRuntime.
     *
     * @return a builder of CayenneRuntime.
     *
     * @see #builder(String)
     * @see CayenneRuntimeBuilder
     * @since 5.0
     */
    public static CayenneRuntimeBuilder builder() {
        return new CayenneRuntimeBuilder(null);
    }

    /**
     * Creates a builder of CayenneRuntime.
     *
     * @param name optional symbolic name of the created runtime.
     * @return a named builder of CayenneRuntime.
     *
     * @see #builder()
     * @see CayenneRuntimeBuilder
     * @since 5.0
     */
    public static CayenneRuntimeBuilder builder(String name) {
        return new CayenneRuntimeBuilder(name);
    }

    /**
     * Creates a runtime configuring it with a standard set of services
     * contained in {@link org.apache.cayenne.configuration.runtime.CoreModule}. CoreModule is created with
     * one or more 'configurationLocations'. An optional array of extra modules
     * may contain service overrides and/or user services.
     */
    protected CayenneRuntime(Collection<Module> modules) {
        this.modules = Objects.requireNonNull(modules);
        this.injector = DIBootstrap.createInjector(modules);
    }

    /**
     * Runs provided operation wrapped in a single transaction. Transaction
     * handling delegated to the internal {@link TransactionManager}. Nested
     * calls to 'performInTransaction' are safe and attached to the same
     * in-progress transaction. TransactionalOperation can be some arbitrary
     * user code, which most often than not will consist of multiple Cayenne
     * operations.
     *
     * @since 4.0
     */
    public <T> T performInTransaction(TransactionalOperation<T> op) {
        TransactionManager tm = injector.getInstance(TransactionManager.class);
        return tm.performInTransaction(op);
    }

    /**
     * Runs provided operation wrapped in a single transaction. Transaction
     * handling delegated to the internal {@link TransactionManager}. Nested
     * calls to 'performInTransaction' are safe and attached to the same
     * in-progress transaction. TransactionalOperation can be some arbitrary
     * user code, which most often than not will consist of multiple Cayenne
     * operations.
     *
     * @since 4.0
     */
    public <T> T performInTransaction(TransactionalOperation<T> op, TransactionListener callback) {
        TransactionManager tm = injector.getInstance(TransactionManager.class);
        return tm.performInTransaction(op, callback);
    }

    /**
     * Runs provided operation wrapped in a single transaction. Transaction
     * handling delegated to the internal {@link TransactionManager}. Nested
     * calls to 'performInTransaction' are safe and attached to the same
     * in-progress transaction. TransactionalOperation can be some arbitrary
     * user code, which most often than not will consist of multiple Cayenne
     * operations.
     *
     * @param op         an operation to perform within the transaction.
     * @param descriptor describes additional transaction parameters
     * @param <T> result type
     * @return a value returned by the "op" operation.
     *
     * @since 4.2
     */
    public <T> T performInTransaction(TransactionalOperation<T> op, TransactionDescriptor descriptor) {
        TransactionManager tm = injector.getInstance(TransactionManager.class);
        return tm.performInTransaction(op, descriptor);
    }

    /**
     * Runs provided operation wrapped in a single transaction. Transaction
     * handling delegated to the internal {@link TransactionManager}. Nested
     * calls to 'performInTransaction' are safe and attached to the same
     * in-progress transaction. TransactionalOperation can be some arbitrary
     * user code, which most often than not will consist of multiple Cayenne
     * operations.
     *
     * @param op         an operation to perform within the transaction.
     * @param callback   a callback to notify as transaction progresses through stages.
     * @param descriptor describes additional transaction parameters
     * @param <T> returned value type
     * @return a value returned by the "op" operation.
     *
     * @since 4.2
     */
    public <T> T performInTransaction(TransactionalOperation<T> op, TransactionListener callback, TransactionDescriptor descriptor) {
        TransactionManager tm = injector.getInstance(TransactionManager.class);
        return tm.performInTransaction(op, callback, descriptor);
    }

    /**
     * Returns the main runtime DataDomain. Note that by default the returned
     * DataDomain is the same as the main DataChannel returned by
     * {@link #getChannel()}. Although users may redefine DataChannel provider
     * in the DI registry, for instance to decorate this DataDomain with a
     * custom wrapper.
     */
    public DataDomain getDataDomain() {
        return injector.getInstance(DataDomain.class);
    }

    /**
     * Returns a default DataSource for this runtime. If no default DataSource
     * exists, an exception is thrown.
     *
     * @since 4.0
     */
    public DataSource getDataSource() {
        DataDomain domain = getDataDomain();
        DataNode defaultNode = domain.getDefaultNode();
        if (defaultNode == null) {

            int s = domain.getDataNodes().size();
            if (s == 0) {
                throw new IllegalStateException("No DataSources configured");
            } else {
                throw new IllegalArgumentException(
                        "No default DataSource configured. You can get explicitly named DataSource by using 'getDataSource(String)'");
            }
        }

        return defaultNode.getDataSource();
    }

    /**
     * Provides access to the JDBC DataSource assigned to a given DataNode. A
     * null argument will work if there's only one DataNode configured.
     * <p>
     * Normally Cayenne applications don't need to access DataSource or any
     * other JDBC code directly, however in some unusual conditions it may be
     * needed, and this method provides a shortcut to raw JDBC.
     */
    public DataSource getDataSource(String dataNodeName) {
        DataDomain domain = getDataDomain();

        if (dataNodeName == null) {
            return getDataSource();
        }

        DataNode node = domain.getDataNode(dataNodeName);
        if (node == null) {
            throw new IllegalArgumentException("Unknown DataNode name: " + dataNodeName);
        }

        return node.getDataSource();
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
    // within another DI registry (e.g. unit tests)
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

}

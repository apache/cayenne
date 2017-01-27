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
package org.apache.cayenne.configuration.server;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.configuration.CayenneRuntime;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.tx.TransactionListener;
import org.apache.cayenne.tx.TransactionManager;
import org.apache.cayenne.tx.TransactionalOperation;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collection;

import static java.util.Arrays.asList;

/**
 * Object representing Cayenne stack. Serves as an entry point to Cayenne for user applications and a factory of ObjectContexts.
 * Implementation is a thin wrapper of the dependency injection container.
 * <p>The "Server" prefix in the name is in contrast to ROP "client" (that is started via ClientRuntime). So
 * ServerRuntime is the default Cayenne stack that you should be using in all apps with the exception of client-side ROP.</p>
 *
 * @since 3.1
 */
public class ServerRuntime extends CayenneRuntime {

    /**
     * Creates a builder of ServerRuntime.
     *
     * @return a builder of ServerRuntime.
     * @since 4.0
     */
    public static ServerRuntimeBuilder builder() {
        return new ServerRuntimeBuilder();
    }

    /**
     * Creates a builder of ServerRuntime.
     *
     * @param name optional symbolic name of the created runtime.
     * @return a named builder of ServerRuntime.
     */
    public static ServerRuntimeBuilder builder(String name) {
        return new ServerRuntimeBuilder(name);
    }

    @Deprecated
    private static Collection<Module> collectModules(final String[] configurationLocations, Module... extraModules) {
        Collection<Module> modules = new ArrayList<>();
        modules.add(new ServerModule());

        if (configurationLocations.length > 0) {
            modules.add(new Module() {
                @Override
                public void configure(Binder binder) {
                    ListBuilder<String> locationsBinder = ServerModule.contributeProjectLocations(binder);
                    for (String c : configurationLocations) {
                        locationsBinder.add(c);
                    }
                }
            });
        }

        if (extraModules != null) {
            modules.addAll(asList(extraModules));
        }

        return modules;
    }

    /**
     * Creates a server runtime configuring it with a standard set of services
     * contained in {@link ServerModule}. CayenneServerModule is created with
     * provided 'configurationLocation'. An optional array of extra modules may
     * contain service overrides and/or user services.
     *
     * @deprecated since 4.0 use {@link ServerRuntime#builder()}.
     */
    @Deprecated
    public ServerRuntime(String configurationLocation, Module... extraModules) {
        this(collectModules(new String[]{configurationLocation}, extraModules));
    }

    /**
     * Creates a server runtime configuring it with a standard set of services
     * contained in {@link ServerModule}. CayenneServerModule is created with
     * one or more 'configurationLocations'. An optional array of extra modules
     * may contain service overrides and/or user services.
     *
     * @deprecated since 4.0 use {@link ServerRuntime#builder()}.
     */
    @Deprecated
    public ServerRuntime(String[] configurationLocations, Module... extraModules) {
        this(collectModules(configurationLocations, extraModules));
    }

    /**
     * Creates a server runtime configuring it with a standard set of services
     * contained in {@link ServerModule}. CayenneServerModule is created with
     * one or more 'configurationLocations'. An optional array of extra modules
     * may contain service overrides and/or user services.
     *
     * @since 4.0
     */
    protected ServerRuntime(Collection<Module> modules) {
        super(modules);
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
}

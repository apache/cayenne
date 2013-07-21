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

import java.util.Collection;

import javax.sql.DataSource;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.configuration.CayenneRuntime;
import org.apache.cayenne.configuration.rop.client.ClientRuntime;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.tx.TransactionManager;
import org.apache.cayenne.tx.TransactionalOperation;

/**
 * An object representing Cayenne server-stack that connects directly to the
 * database via JDBC. This is an entry point for user applications to access
 * Cayenne, which encapsulates the dependency injection internals. The term
 * "server" is used as opposed to ROP "client" (see {@link ClientRuntime}). Any
 * application, desktop, server, etc. that has a direct JDBC connection should
 * be using this runtime.
 * 
 * @since 3.1
 */
public class ServerRuntime extends CayenneRuntime {

    private static Module mainModule(String... configurationLocations) {
        return new ServerModule(configurationLocations);
    }

    /**
     * Creates a server runtime configuring it with a standard set of services
     * contained in {@link ServerModule}. CayenneServerModule is created with
     * provided 'configurationLocation'. An optional array of extra modules may
     * contain service overrides and/or user services.
     */
    public ServerRuntime(String configurationLocation, Module... extraModules) {
        super(mergeModules(mainModule(configurationLocation), extraModules));
    }

    /**
     * Creates a server runtime configuring it with a standard set of services
     * contained in {@link ServerModule}. CayenneServerModule is created with
     * one or more 'configurationLocations'. An optional array of extra modules
     * may contain service overrides and/or user services.
     */
    public ServerRuntime(String[] configurationLocations, Module... extraModules) {
        super(mergeModules(mainModule(configurationLocations), extraModules));
    }

    /**
     * Runs provided operation wrapped in a single transaction. Transaction
     * handling delegated to the internal {@link TransactionManager}. Nested
     * calls to 'performInTransaction' are safe and attached to the same
     * in-progress transaction. TransactionalOperation can be some arbitrary
     * user code, which most often than not will consist of multiple Cayenne
     * operations.
     * 
     * @since 3.2
     */
    public <T> T performInTransaction(TransactionalOperation<T> op) {
        TransactionManager tm = injector.getInstance(TransactionManager.class);
        return tm.performInTransaction(op);
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
     * Provides access to the JDBC DataSource assigned to a given DataNode. A
     * null argument will work if there's only one DataNode configured.
     * <p>
     * Normally Cayenne applications don't need to access DataSource or any
     * other JDBC code directly, however in some unusual conditions it may be
     * needed, and this method provides a shortcut to raw JDBC.
     */
    public DataSource getDataSource(String dataNodeName) {
        DataDomain domain = getDataDomain();

        if (dataNodeName != null) {
            DataNode node = domain.getDataNode(dataNodeName);
            if (node == null) {
                throw new IllegalArgumentException("Unknown DataNode name: " + dataNodeName);
            }

            return node.getDataSource();
        }

        else {
            Collection<DataNode> nodes = domain.getDataNodes();
            if (nodes.size() != 1) {
                throw new IllegalArgumentException(
                        "If DataNode name is not specified, DataDomain must have exactly 1 DataNode. Actual node count: "
                                + nodes.size());
            }

            return nodes.iterator().next().getDataSource();
        }
    }
}

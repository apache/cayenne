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

import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptors;
import org.apache.cayenne.configuration.runtime.CoreModule;
import org.apache.cayenne.configuration.runtime.CoreModuleExtender;
import org.apache.cayenne.datasource.CayenneDataSource;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.ModuleLoader;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A convenience class to assemble custom {@link CayenneRuntime}. It allows to easily
 * configure custom modules, multiple config locations, or quickly create a
 * global {@link DataSource}.
 *
 * @since 5.0 renamed from ServerRuntimeBuilder and moved to {@code org.apache.cayenne.runtime} package
 */
public class CayenneRuntimeBuilder {

    static final String DEFAULT_NAME = "cayenne";

    private final String name;
    private final Collection<String> configs;
    private final List<Module> modules;
    private boolean autoLoadModules;

    private int autoNodeSuffix;
    private DataNodeDescriptor defaultNode;
    private final Map<DataNodeDescriptor, Set<String>> mapsByNode;

    @Deprecated
    private String jdbcUrl;

    @Deprecated
    private String jdbcDriver;

    @Deprecated
    private String jdbcUser;

    @Deprecated
    private String jdbcPassword;

    @Deprecated
    private int jdbcMinConnections;

    @Deprecated
    private int jdbcMaxConnections;

    @Deprecated
    private long maxQueueWaitTime;

    @Deprecated
    private String validationQuery;

    /**
     * Creates a builder with a fixed name of the DataDomain of the resulting
     * CayenneRuntime. Specifying explicit name is often needed for consistency
     * in runtimes merged from multiple configs, each having its own name.
     */
    protected CayenneRuntimeBuilder(String name) {
        this.configs = new LinkedHashSet<>();
        this.modules = new ArrayList<>();
        this.name = name;
        this.autoLoadModules = true;
        this.mapsByNode = new HashMap<>();
    }

    /**
     * Disables DI module autoloading. By default, autoloading is enabled based on {@link ModuleLoader} service provider
     * interface. If you decide to disable it, make sure you explicitly provide all the modules that you need.
     */
    public CayenneRuntimeBuilder disableModulesAutoLoading() {
        this.autoLoadModules = false;
        return this;
    }

    /**
     * @deprecated in favor of {@link #defaultDataNode(DataSource)}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public CayenneRuntimeBuilder dataSource(DataSource dataSource) {
        return defaultDataNode(dataSource);
    }

    /**
     * Will create a default DataNode based on the provided DataSource that will handle all DataMaps not explicitly
     * linked to other DataNodes.
     */
    public CayenneRuntimeBuilder defaultDataNode(DataSource dataSource) {
        return defaultDataNode(DataNodeDescriptor.of(nextAutoNodeName()).dataSource(dataSource).build());
    }

    public CayenneRuntimeBuilder addDataNode(DataNodeDescriptor node, String... linkedDataMaps) {
        mapsByNode.put(node, Set.of(linkedDataMaps));
        return this;
    }

    public CayenneRuntimeBuilder addDataNode(DataSource dataSource, String... linkedDataMaps) {
        DataNodeDescriptor node = DataNodeDescriptor.of(nextAutoNodeName()).dataSource(dataSource).build();
        mapsByNode.put(node, Set.of(linkedDataMaps));
        return this;
    }

    public CayenneRuntimeBuilder defaultDataNode(DataNodeDescriptor defaultNode) {
        this.defaultNode = defaultNode;
        return this;
    }

    private String nextAutoNodeName() {
        String base = this.name != null ? this.name : CayenneRuntimeBuilder.DEFAULT_NAME;
        return base + "-" + autoNodeSuffix++;
    }

    /**
     * Sets a database URL for the default DataSource.
     *
     * @deprecated in favor of {@link #defaultDataNode(DataSource)}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public CayenneRuntimeBuilder url(String url) {
        this.jdbcUrl = url;
        return this;
    }

    /**
     * Sets a driver Java class for the default DataSource.
     *
     * @deprecated in favor of {@link #defaultDataNode(DataSource)}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public CayenneRuntimeBuilder jdbcDriver(String driver) {
        this.jdbcDriver = driver;
        return this;
    }

    /**
     * Sets a validation query for the default DataSource.
     *
     * @param validationQuery a SQL string that returns some result. It will be used to
     *                        validate connections in the pool.
     * @deprecated in favor of {@link #defaultDataNode(DataSource)}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public CayenneRuntimeBuilder validationQuery(String validationQuery) {
        this.validationQuery = validationQuery;
        return this;
    }

    /**
     * @deprecated in favor of {@link #defaultDataNode(DataSource)}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public CayenneRuntimeBuilder maxQueueWaitTime(long maxQueueWaitTime) {
        this.maxQueueWaitTime = maxQueueWaitTime;
        return this;
    }

    /**
     * Sets a user name for the default DataSource.
     *
     * @deprecated in favor of {@link #defaultDataNode(DataSource)}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public CayenneRuntimeBuilder user(String user) {
        this.jdbcUser = user;
        return this;
    }

    /**
     * Sets a password for the default DataSource.
     *
     * @deprecated in favor of {@link #defaultDataNode(DataSource)}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public CayenneRuntimeBuilder password(String password) {
        this.jdbcPassword = password;
        return this;
    }

    /**
     * @deprecated in favor of {@link #defaultDataNode(DataSource)}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public CayenneRuntimeBuilder minConnections(int minConnections) {
        this.jdbcMinConnections = minConnections;
        return this;
    }

    /**
     * @deprecated in favor of {@link #defaultDataNode(DataSource)}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public CayenneRuntimeBuilder maxConnections(int maxConnections) {
        this.jdbcMaxConnections = maxConnections;
        return this;
    }

    public CayenneRuntimeBuilder addConfig(String configurationLocation) {
        configs.add(configurationLocation);
        return this;
    }

    public CayenneRuntimeBuilder addConfigs(String... configurationLocations) {
        if (configurationLocations != null) {
            configs.addAll(List.of(configurationLocations));
        }
        return this;
    }

    public CayenneRuntimeBuilder addConfigs(Collection<String> configurationLocations) {
        configs.addAll(configurationLocations);
        return this;
    }

    public CayenneRuntimeBuilder addModule(Module module) {
        modules.add(module);
        return this;
    }

    public CayenneRuntimeBuilder addModules(Collection<Module> modules) {
        this.modules.addAll(modules);
        return this;
    }

    public CayenneRuntime build() {

        Collection<Module> allModules = new ArrayList<>();

        // first load default or autoloaded modules...
        allModules.addAll(autoLoadModules ? autoLoadedModules() : defaultModules());

        // custom modules override default and autoloaded modules...
        allModules.addAll(this.modules);

        // builder modules override default, autoloaded and custom modules...
        allModules.addAll(builderModules());

        return new CayenneRuntime(allModules);
    }

    private Collection<? extends Module> autoLoadedModules() {
        return new ModuleLoader().load(CayenneRuntimeModuleProvider.class);
    }

    private Collection<? extends Module> defaultModules() {
        return Collections.singleton(new CoreModule());
    }

    private Collection<? extends Module> builderModules() {

        Collection<Module> modules = new ArrayList<>();

        if (!configs.isEmpty()) {
            modules.add(b -> {
                CoreModuleExtender extender = CoreModule.extend(b);
                configs.forEach(extender::addProjectLocation);
            });
        }

        String nameOverride = name;
        if (nameOverride == null) {
            // check if we need to force the default name ... we do when no configs or multiple configs are supplied.
            if (configs.size() != 1) {
                nameOverride = DEFAULT_NAME;
            }
        }

        if (nameOverride != null) {
            String finalNameOverride = nameOverride;
            modules.add(b -> CoreModule.extend(b).setProperty(Constants.DOMAIN_NAME_PROPERTY, finalNameOverride));
        }

        DataNodeDescriptor defaultNode = this.defaultNode != null ? this.defaultNode : defaultNodeFromConnectionInfo();
        modules.add(b -> b.bind(DataNodeDescriptors.class).toInstance(new DataNodeDescriptors(mapsByNode, defaultNode)));

        return modules;
    }

    private DataNodeDescriptor defaultNodeFromConnectionInfo() {

        if (jdbcUrl == null) {
            return null;
        }

        CayenneDataSource.Builder builder = CayenneDataSource.of(jdbcUrl)
                .driverClass(jdbcDriver)
                .userName(jdbcUser)
                .password(jdbcPassword)
                .maxQueueWaitTime(maxQueueWaitTime)
                .validationQuery(validationQuery);

        if (jdbcMinConnections >= 0 && jdbcMaxConnections >= 0) {
            builder.pool(jdbcMinConnections, jdbcMaxConnections);
        }

        return DataNodeDescriptor.of(nextAutoNodeName()).dataSource(builder.build()).build();
    }
}

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
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.datasource.DataSourceBuilder;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.MapBuilder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.ModuleLoader;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * A convenience class to assemble custom ServerRuntime. It allows to easily
 * configure custom modules, multiple config locations, or quickly create a
 * global DataSource.
 *
 * @since 4.0
 */
public class ServerRuntimeBuilder {

    static final String DEFAULT_NAME = "cayenne";

    private String name;
    private Collection<String> configs;
    private List<Module> modules;
    private DataSourceFactory dataSourceFactory;
    private String jdbcUrl;
    private String jdbcDriver;
    private String jdbcUser;
    private String jdbcPassword;
    private int jdbcMinConnections;
    private int jdbcMaxConnections;
    private long maxQueueWaitTime;
    private String validationQuery;
    private boolean autoLoadModules;

    /**
     * Creates a builder with a fixed name of the DataDomain of the resulting
     * ServerRuntime. Specifying explicit name is often needed for consistency
     * in runtimes merged from multiple configs, each having its own name.
     */
    protected ServerRuntimeBuilder(String name) {
        this.configs = new LinkedHashSet<>();
        this.modules = new ArrayList<>();
        this.name = name;
        this.autoLoadModules = true;
    }

    /**
     * Disables DI module auto-loading. By default auto-loading is enabled based on
     * {@link org.apache.cayenne.di.spi.ModuleLoader} service provider inetrface. If you decide to disable auto-loading,
     * make sure you provide all the modules that you need.
     *
     * @return this builder instance.
     */
    public ServerRuntimeBuilder disableModulesAutoLoading() {
        this.autoLoadModules = false;
        return this;
    }

    /**
     * Sets a DataSource that will override any DataSources found in the
     * mapping. If the mapping contains no DataNodes, and the DataSource is set
     * with this method, the builder would create a single default DataNode.
     *
     * @see DataSourceBuilder
     */
    public ServerRuntimeBuilder dataSource(DataSource dataSource) {
        this.dataSourceFactory = new FixedDataSourceFactory(dataSource);
        return this;
    }

    /**
     * Sets JNDI location for the default DataSource. If the mapping contains no
     * DataNodes, and the DataSource is set with this method, the builder would
     * create a single default DataNode.
     */
    public ServerRuntimeBuilder jndiDataSource(String location) {
        this.dataSourceFactory = new FixedJNDIDataSourceFactory(location);
        return this;
    }

    /**
     * Sets a database URL for the default DataSource.
     */
    public ServerRuntimeBuilder url(String url) {
        this.jdbcUrl = url;
        return this;
    }

    /**
     * Sets a driver Java class for the default DataSource.
     */
    public ServerRuntimeBuilder jdbcDriver(String driver) {
        // TODO: guess the driver from URL
        this.jdbcDriver = driver;
        return this;
    }

    /**
     * Sets a validation query for the default DataSource.
     *
     * @param validationQuery a SQL string that returns some result. It will be used to
     *                        validate connections in the pool.
     */
    public ServerRuntimeBuilder validationQuery(String validationQuery) {
        this.validationQuery = validationQuery;
        return this;
    }

    public ServerRuntimeBuilder maxQueueWaitTime(long maxQueueWaitTime) {
        this.maxQueueWaitTime = maxQueueWaitTime;
        return this;
    }

    /**
     * Sets a user name for the default DataSource.
     */
    public ServerRuntimeBuilder user(String user) {
        this.jdbcUser = user;
        return this;
    }

    /**
     * Sets a password for the default DataSource.
     */
    public ServerRuntimeBuilder password(String password) {
        this.jdbcPassword = password;
        return this;
    }

    public ServerRuntimeBuilder minConnections(int minConnections) {
        this.jdbcMinConnections = minConnections;
        return this;
    }

    public ServerRuntimeBuilder maxConnections(int maxConnections) {
        this.jdbcMaxConnections = maxConnections;
        return this;
    }

    public ServerRuntimeBuilder addConfig(String configurationLocation) {
        configs.add(configurationLocation);
        return this;
    }

    public ServerRuntimeBuilder addConfigs(String... configurationLocations) {
        if (configurationLocations != null) {
            configs.addAll(Arrays.asList(configurationLocations));
        }
        return this;
    }

    public ServerRuntimeBuilder addConfigs(Collection<String> configurationLocations) {
        configs.addAll(configurationLocations);
        return this;
    }

    public ServerRuntimeBuilder addModule(Module module) {
        modules.add(module);
        return this;
    }

    public ServerRuntimeBuilder addModules(Collection<Module> modules) {
        this.modules.addAll(modules);
        return this;
    }

    public ServerRuntime build() {

        Collection<Module> allModules = new ArrayList<>();

        // first load default or auto-loaded modules...
        allModules.addAll(autoLoadModules ? autoLoadedModules() : defaultModules());

        // custom modules override default and auto-loaded modules...
        allModules.addAll(this.modules);

        // builder modules override default, auto-loaded and custom modules...
        allModules.addAll(builderModules());

        return new ServerRuntime(allModules);
    }

    private Collection<? extends Module> autoLoadedModules() {
        return new ModuleLoader().load(CayenneServerModuleProvider.class);
    }

    private Collection<? extends Module> defaultModules() {
        return Collections.singleton(new ServerModule());
    }

    private Collection<? extends Module> builderModules() {

        Collection<Module> modules = new ArrayList<>();

        if (!configs.isEmpty()) {
            modules.add(binder -> {
                ListBuilder<String> locationsBinder = ServerModule.contributeProjectLocations(binder);
                for (String c : configs) {
                    locationsBinder.add(c);
                }
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

            final String finalNameOverride = nameOverride;
            modules.add(binder -> ServerModule.contributeProperties(binder).put(Constants.SERVER_DOMAIN_NAME_PROPERTY, finalNameOverride));
        }

        if (dataSourceFactory != null) {

            modules.add(binder -> {
                binder.bind(DataDomain.class).toProvider(SyntheticNodeDataDomainProvider.class);
                binder.bind(DataSourceFactory.class).toInstance(dataSourceFactory);
            });

        }
        // URL and driver are the minimal requirement for DelegatingDataSourceFactory to work
        else if (jdbcUrl != null && jdbcDriver != null) {
            modules.add(binder -> {
                binder.bind(DataDomain.class).toProvider(SyntheticNodeDataDomainProvider.class);
                MapBuilder<String> props = ServerModule.contributeProperties(binder)
                        .put(Constants.JDBC_DRIVER_PROPERTY, jdbcDriver).put(Constants.JDBC_URL_PROPERTY, jdbcUrl);

                if (jdbcUser != null) {
                    props.put(Constants.JDBC_USERNAME_PROPERTY, jdbcUser);
                }

                if (jdbcPassword != null) {
                    props.put(Constants.JDBC_PASSWORD_PROPERTY, jdbcPassword);
                }

                if (jdbcMinConnections > 0) {
                    props.put(Constants.JDBC_MIN_CONNECTIONS_PROPERTY, Integer.toString(jdbcMinConnections));
                }

                if (jdbcMaxConnections > 0) {
                    props.put(Constants.JDBC_MAX_CONNECTIONS_PROPERTY, Integer.toString(jdbcMaxConnections));
                }

                if (maxQueueWaitTime > 0) {
                    props.put(Constants.JDBC_MAX_QUEUE_WAIT_TIME, Long.toString(maxQueueWaitTime));
                }

                if (validationQuery != null) {
                    props.put(Constants.JDBC_VALIDATION_QUERY_PROPERTY, validationQuery);
                }
            });
        }

        return modules;
    }
}

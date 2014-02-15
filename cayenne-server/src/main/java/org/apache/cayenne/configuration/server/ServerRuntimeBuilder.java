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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import javax.sql.DataSource;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.MapBuilder;
import org.apache.cayenne.di.Module;

/**
 * A convenience class to assemble custom ServerRuntime. It allows to easily
 * configure custom modules, multiple config locations, or quickly create a
 * global DataSource.
 * 
 * @since 3.2
 */
public class ServerRuntimeBuilder {

    private Collection<String> configs;
    private List<Module> modules;
    private DataSourceFactory dataSourceFactory;
    private String jdbcUrl;
    private String jdbcDriver;
    private String jdbcUser;
    private String jdbcPassword;
    private int jdbcMinConnections;
    private int jdbcMaxConnections;

    public ServerRuntimeBuilder() {
        this.configs = new LinkedHashSet<String>();
        this.modules = new ArrayList<Module>();
    }

    public ServerRuntimeBuilder(String configurationLocation) {
        this();
        addConfig(configurationLocation);
    }

    /**
     * Sets a DataSource that will override any DataSources found in the
     * mapping. Moreover if the mapping contains no DataNodes, and the
     * DataSource is set with this method, the builder would create a single
     * default DataNode.
     */
    public ServerRuntimeBuilder dataSource(DataSource dataSource) {
        this.dataSourceFactory = new FixedDataSourceFactory(dataSource);
        return this;
    }

    public ServerRuntimeBuilder jndiDataSource(String location) {
        this.dataSourceFactory = new FixedJNDIDataSourceFactory(location);
        return this;
    }

    public ServerRuntimeBuilder url(String url) {
        this.jdbcUrl = url;
        return this;
    }

    public ServerRuntimeBuilder jdbcDriver(String driver) {
        // TODO: guess the driver from URL
        this.jdbcDriver = driver;
        return this;
    }

    public ServerRuntimeBuilder user(String user) {
        this.jdbcUser = user;
        return this;
    }

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

        buildModules();

        String[] configs = this.configs.toArray(new String[this.configs.size()]);
        Module[] modules = this.modules.toArray(new Module[this.modules.size()]);
        return new ServerRuntime(configs, modules);
    }

    private void buildModules() {

        if (dataSourceFactory != null) {

            prepend(new Module() {
                @Override
                public void configure(Binder binder) {
                    binder.bind(DataDomain.class).toProvider(SyntheticNodeDataDomainProvider.class);
                    binder.bind(DataSourceFactory.class).toInstance(dataSourceFactory);
                }
            });

        }
        // URL and driver are the minimal requirement for
        // DelegatingDataSourceFactory to work
        else if (jdbcUrl != null && jdbcDriver != null) {
            prepend(new Module() {
                @Override
                public void configure(Binder binder) {
                    binder.bind(DataDomain.class).toProvider(SyntheticNodeDataDomainProvider.class);
                    MapBuilder<Object> props = binder.bindMap(Constants.PROPERTIES_MAP)
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
                }
            });
        }
    }

    private void prepend(Module module) {
        // prepend any special modules BEFORE custom modules, to allow callers
        // to override our stuff
        modules.add(0, module);
    }
}

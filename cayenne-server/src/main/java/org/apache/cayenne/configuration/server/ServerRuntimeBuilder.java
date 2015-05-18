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
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import javax.sql.DataSource;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.datasource.DataSourceBuilder;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;

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

	public static ServerRuntimeBuilder builder() {
		return new ServerRuntimeBuilder();
	}

	public static ServerRuntimeBuilder builder(String name) {
		return new ServerRuntimeBuilder(name);
	}

	/**
	 * Creates an empty builder.
	 */
	public ServerRuntimeBuilder() {
		this(null);
	}

	/**
	 * Creates a builder with a fixed name of the DataDomain of the resulting
	 * ServerRuntime. Specifying explicit name is often needed for consistency
	 * in runtimes merged from multiple configs, each having its own name.
	 */
	public ServerRuntimeBuilder(String name) {
		this.configs = new LinkedHashSet<String>();
		this.modules = new ArrayList<Module>();
		this.name = name;
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

		buildModules();

		String[] configs = this.configs.toArray(new String[this.configs.size()]);
		Module[] modules = this.modules.toArray(new Module[this.modules.size()]);
		return new ServerRuntime(configs, modules);
	}

	private void buildModules() {

		String nameOverride = name;

		if (nameOverride == null) {
			// check if we need to force the default name ... we do when no
			// configs or multiple configs are supplied.
			if (configs.size() != 1) {
				nameOverride = DEFAULT_NAME;
			}
		}

		if (nameOverride != null) {

			final String finalNameOverride = nameOverride;
			prepend(new Module() {
				@Override
				public void configure(Binder binder) {
					binder.bindMap(Constants.PROPERTIES_MAP).put(Constants.SERVER_DOMAIN_NAME_PROPERTY,
							finalNameOverride);
				}
			});
		}

		if (dataSourceFactory != null) {

			prepend(new Module() {
				@Override
				public void configure(Binder binder) {
					binder.bind(DataDomain.class).toProvider(SyntheticNodeDataDomainProvider.class);
					binder.bind(DataSourceFactory.class).toInstance(dataSourceFactory);
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

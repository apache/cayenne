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
package org.apache.cayenne.configuration.rop.client;

import org.apache.cayenne.configuration.CayenneRuntime;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.remote.ClientConnection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static java.util.Arrays.asList;

/**
 * A user application entry point to Cayenne stack on the ROP client.
 * 
 * @since 3.1
 */
// TODO: module auto-loading and ClientRuntimeBuilder
public class ClientRuntime extends CayenneRuntime {

	private static Collection<Module> collectModules(Map<String, String> properties, Module... extraModules) {

		Collection<Module> modules = extraModules != null ? asList(extraModules) : Collections.<Module>emptyList();
		return collectModules(properties, modules);
	}

	private static Collection<Module> collectModules(Map<String, String> properties, Collection<Module> extraModules) {
		Collection<Module> modules = new ArrayList<>();
		modules.add(new ClientModule(properties));

		if(extraModules != null) {
			modules.addAll(extraModules);
		}

		return modules;
	}

	/**
	 * Creates a client runtime configuring it with a standard set of services
	 * contained in {@link ClientModule}. CayenneClientModule is created based
	 * on a set of properties that contain things like connection information,
	 * etc. Recognized property keys are defined in {@link ClientModule}. An
	 * optional array of extra modules may contain service overrides and/or user
	 * services.
	 */
	public ClientRuntime(Map<String, String> properties, Collection<Module> extraModules) {
		this(collectModules(properties, extraModules));
	}

	/**
	 * Creates a client runtime configuring it with a standard set of services
	 * contained in {@link ClientModule}. CayenneClientModule is created based
	 * on a set of properties that contain things like connection information,
	 * etc. Recognized property keys are defined in {@link ClientModule}. An
	 * optional collection of extra modules may contain service overrides and/or
	 * user services.
	 */
	public ClientRuntime(Map<String, String> properties, Module... extraModules) {
		this(collectModules(properties, extraModules));
	}

	/**
	 * @since 4.0
	 */
	protected ClientRuntime(Collection<Module> modules) {
		super(modules);
	}

	public ClientConnection getConnection() {
		return injector.getInstance(ClientConnection.class);
	}

}

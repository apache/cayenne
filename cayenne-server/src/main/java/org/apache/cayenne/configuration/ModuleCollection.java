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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;

/**
 * A module that decorates a collection of other modules. Used as a helper for
 * multi-module runtimes initialization.
 * 
 * @since 3.2
 */
public class ModuleCollection implements Module {

	private Collection<Module> modules;

	public ModuleCollection(Module... modules) {

		this.modules = new ArrayList<Module>();
		add(modules);
	}

	public ModuleCollection add(Module... modules) {
		if (modules != null) {
			for (Module m : modules) {
				addModule(m);
			}
		}

		return this;
	}

	public ModuleCollection add(Collection<Module> modules) {
		if (modules != null) {
			for (Module m : modules) {
				addModule(m);
			}
		}

		return this;
	}

	private void addModule(Module m) {
		if (m instanceof ModuleCollection) {
			// flatten
			add(((ModuleCollection) m).getModules());
		} else {
			this.modules.add(m);
		}
	}

	public Collection<Module> getModules() {
		return modules;
	}

	@Override
	public void configure(Binder binder) {
		for (Module m : modules) {
			m.configure(binder);
		}
	}
}

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
package org.apache.cayenne.cache.invalidation;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.configuration.server.ServerRuntimeBuilder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.junit.After;
import org.junit.Before;

public abstract class CacheInvalidationCase {

	protected ServerRuntime runtime;

	protected TableHelper e1;

	protected TableHelper e2;

	@Before
	public void startCayenne() throws Exception {
		this.runtime = configureCayenne().build();

		DBHelper dbHelper = new DBHelper(runtime.getDataSource());

		this.e1 = new TableHelper(dbHelper, "E1").setColumns("ID");
		this.e1.deleteAll();

		this.e2 = new TableHelper(dbHelper, "E2").setColumns("ID");
		this.e2.deleteAll();
	}

	protected abstract Module extendInvalidationModule();

	protected Module buildCustomModule() {
		return binder -> { };
	}

	protected ServerRuntimeBuilder configureCayenne() {
		return ServerRuntime.builder()
				.addModule(extendInvalidationModule())
				.addModule(buildCustomModule())
				.addConfig("cayenne-lifecycle.xml");
	}

	@After
	public void shutdownCayenne() {
		if (runtime != null) {
			runtime.shutdown();
		}
	}

}

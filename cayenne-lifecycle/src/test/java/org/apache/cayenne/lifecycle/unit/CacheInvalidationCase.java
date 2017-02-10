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
package org.apache.cayenne.lifecycle.unit;

import java.util.Collection;
import java.util.Collections;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.configuration.server.ServerRuntimeBuilder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.lifecycle.cache.CacheGroups;
import org.apache.cayenne.lifecycle.cache.CacheInvalidationModuleBuilder;
import org.apache.cayenne.lifecycle.cache.InvalidationFunction;
import org.apache.cayenne.lifecycle.cache.InvalidationHandler;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.junit.After;
import org.junit.Before;

public class CacheInvalidationCase {

	protected ServerRuntime runtime;

	protected TableHelper e1;

	@Before
	public void startCayenne() throws Exception {
		this.runtime = configureCayenne().build();

		DBHelper dbHelper = new DBHelper(runtime.getDataSource());

		this.e1 = new TableHelper(dbHelper, "E1").setColumns("ID");
		this.e1.deleteAll();
	}

	protected ServerRuntimeBuilder configureCayenne() {
		Module cacheInvalidationModule = CacheInvalidationModuleBuilder
				.builder()
				.invalidationHandler(G1InvalidationHandler.class)
				.build();

		return ServerRuntime.builder()
				.addModule(cacheInvalidationModule)
				.addConfig("cayenne-lifecycle.xml");
	}

	@After
	public void shutdownCayenne() {
		if (runtime != null) {
			runtime.shutdown();
		}
	}

	public static class G1InvalidationHandler implements InvalidationHandler {
		@Override
		public InvalidationFunction canHandle(Class<? extends Persistent> type) {
			return new InvalidationFunction() {
				@Override
				public Collection<String> apply(Persistent persistent) {
					return Collections.singleton("g1");
				}
			};
		}
	}

}

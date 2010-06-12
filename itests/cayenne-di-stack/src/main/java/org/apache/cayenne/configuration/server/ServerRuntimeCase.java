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
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.dbsync.CreateIfNoSchemaStrategy;
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategy;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;

public abstract class ServerRuntimeCase extends TestCase {

	static final Map<RuntimeName, ServerRuntime> runtimeCache;

	static {
		runtimeCache = new HashMap<RuntimeName, ServerRuntime>();
	}

	protected ServerRuntime runtime;

	@Override
	protected void setUp() throws Exception {

		RuntimeName name = getRuntimeName();
		assertNotNull(name);

		String location = "cayenne-" + name + ".xml";
		runtime = runtimeCache.get(location);
		if (runtime == null) {
			runtime = new ServerRuntime(location);
			runtimeCache.put(name, runtime);

			// setup schema

			// TODO: should that be drop/create?
			SchemaUpdateStrategy dbCreator = new CreateIfNoSchemaStrategy();
			dbCreator.updateSchema(getDataNode());
		}
	}

	@Override
	protected void tearDown() throws Exception {
		runtime = null;
	}

	protected abstract RuntimeName getRuntimeName();

	protected DBHelper getDbUtils() {

		return new DBHelper(getDataNode().getDataSource());
	}

	protected TableHelper getTableHelper(String tableName) {
		return new TableHelper(getDbUtils(), tableName);
	}

	private DataNode getDataNode() {
		Collection<DataNode> nodes = runtime.getDataDomain().getDataNodes();
		assertFalse("Can't find DataSource - no nodes configured", nodes
				.isEmpty());
		assertEquals("Can't find DataSource - multiple nodes found", 1, nodes
				.size());

		return nodes.iterator().next();
	}
}

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
package org.apache.cayenne.commitlog.unit;

import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.runtime.CayenneRuntimeBuilder;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.junit.After;
import org.junit.Before;

public class FlattenedRuntimeCase {

	protected CayenneRuntime runtime;

	protected TableHelper e3;
	protected TableHelper e4;
	protected TableHelper e34;

	@Before
	public void startCayenne() throws Exception {
		this.runtime = configureCayenne().build();

		DBHelper dbHelper = new DBHelper(runtime.getDataSource());

		this.e3 = new TableHelper(dbHelper, "E3").setColumns("ID");
		this.e4 = new TableHelper(dbHelper, "E4").setColumns("ID");
		this.e34 = new TableHelper(dbHelper, "E34").setColumns("E3_ID", "E4_ID");

		this.e34.deleteAll();
		this.e3.deleteAll();

	}

	protected CayenneRuntimeBuilder configureCayenne() {
		return CayenneRuntime.builder().addConfig("cayenne-lifecycle.xml");
	}

	@After
	public void shutdownCayenne() {
		if (runtime != null) {
			runtime.shutdown();
		}
	}

}

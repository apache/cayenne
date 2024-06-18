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

package org.apache.cayenne.dba.sqlserver;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.unit.SQLServerUnitDbAdapter;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.RuntimeCaseDataSourceFactory;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import java.sql.Connection;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class SQLServerSnifferIT extends RuntimeCase {

	@Inject
	private RuntimeCaseDataSourceFactory dataSourceFactory;

	@Inject
	private UnitDbAdapter accessStackAdapter;

	@Inject
	private AdhocObjectFactory objectFactory;

	@Test
	public void testCreateAdapter() throws Exception {

		SQLServerSniffer sniffer = new SQLServerSniffer(objectFactory);

		DbAdapter adapter;

		try (Connection c = dataSourceFactory.getSharedDataSource().getConnection()) {
			adapter = sniffer.createAdapter(c.getMetaData());
		}

		if (accessStackAdapter instanceof SQLServerUnitDbAdapter) {
			assertNotNull(adapter);
		} else {
			assertNull(adapter);
		}
	}
}

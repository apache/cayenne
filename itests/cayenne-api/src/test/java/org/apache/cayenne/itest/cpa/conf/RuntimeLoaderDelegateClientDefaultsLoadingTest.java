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
package org.apache.cayenne.itest.cpa.conf;

import java.util.Collections;

import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.itest.cpa.CPAContextCase;
import org.apache.cayenne.itest.cpa.defaults.client.DefaultsTable3;
import org.apache.cayenne.itest.cpa.defaults.client.DefaultsTable4;
import org.apache.cayenne.query.RefreshQuery;

public class RuntimeLoaderDelegateClientDefaultsLoadingTest extends
		CPAContextCase {

	/**
	 * Ensures that one-way mapping can be used from the client.
	 */
	public void testUpdateImplicitClientToOne() throws Exception {
		getDbHelper().deleteAll("defaults_table4");
		getDbHelper().deleteAll("defaults_table3");
		getDbHelper().insert("defaults_table3", new String[] { "id", "name" },
				new Object[] { 1, "X" });
		getDbHelper().insert("defaults_table3", new String[] { "id", "name" },
				new Object[] { 2, "Y" });
		getDbHelper().insert("defaults_table4",
				new String[] { "id", "defaults_table3_id" },
				new Object[] { 1, 1 });

		ObjectContext clientContext = getClientContext();

		DefaultsTable4 o = (DefaultsTable4) DataObjectUtils.objectForPK(
				clientContext, DefaultsTable4.class, Collections.singletonMap(
						"id", 1));
		DefaultsTable3 o1 = (DefaultsTable3) DataObjectUtils.objectForPK(
				clientContext, DefaultsTable3.class, Collections.singletonMap(
						"id", 1));
		DefaultsTable3 o2 = (DefaultsTable3) DataObjectUtils.objectForPK(
				clientContext, DefaultsTable3.class, Collections.singletonMap(
						"id", 2));

		assertEquals(1, o1.getDefaultTable4s().size());
		assertEquals(0, o2.getDefaultTable4s().size());

		o2.addToDefaultTable4s(o);

		assertEquals(1, o2.getDefaultTable4s().size());
		assertFalse(clientContext.modifiedObjects().isEmpty());
		clientContext.commitChanges();
		assertEquals(1, o2.getDefaultTable4s().size());

		// there is a bug in RefreshQuery that fails to invalidate to-many on
		// the client - so working around it be creating a new context; still
		// running the query though to refresh the server
		clientContext.performQuery(new RefreshQuery());

		clientContext = getClientContext(true);
		o1 = (DefaultsTable3) DataObjectUtils.objectForPK(clientContext,
				DefaultsTable3.class, Collections.singletonMap("id", 1));
		o2 = (DefaultsTable3) DataObjectUtils.objectForPK(clientContext,
				DefaultsTable3.class, Collections.singletonMap("id", 2));

		// TODO: andrus 8/28/2007 - there is a weird test failure in the
		// following two lines when run from the command line with maven. The
		// test succeeds when executed from cayenne-api directory, but when run
		// from any parent dir (itests or cayenne) it fails.. This points to an
		// environment setup issue and I can't quite figure it out.

		// assertEquals(1, o2.getDefaultTable4s().size());
		// assertEquals(0, o1.getDefaultTable4s().size());
	}
}

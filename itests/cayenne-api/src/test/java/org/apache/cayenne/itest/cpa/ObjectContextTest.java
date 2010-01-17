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
package org.apache.cayenne.itest.cpa;

import java.util.List;

import org.apache.cayenne.query.SelectQuery;

public class ObjectContextTest extends CPAContextCase {

	public void testPerformQuery() throws Exception {

		getDbHelper().deleteAll("entity1");
		getDbHelper().insert("entity1", new String[] { "id", "name" },
				new Object[] { 1, "X" });
		getDbHelper().insert("entity1", new String[] { "id", "name" },
				new Object[] { 2, "Y" });

		SelectQuery query = new SelectQuery(Entity1.class);
		List results = getContext().performQuery(query);
		assertNotNull(results);
		assertEquals(2, results.size());
	}
}

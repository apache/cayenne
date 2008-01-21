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
package org.apache.cayenne.itest.pojo;

import java.util.List;

import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.query.SelectQuery;

public class SimpleObjectTest extends PojoContextCase {

	public void testInsert() throws Exception {
		Entity1 o = (Entity1) context.newObject(Entity1.class);
		o.setName("X");
		context.commitChanges();
		assertEquals("X", getDbHelper().getObject("entity1", "name"));
	}

	public void testSelect() throws Exception {
		getDbHelper().deleteAll("entity1");
		getDbHelper().insert("entity1", new String[] { "id", "name" },
				new Object[] { 5, "Y" });

		SelectQuery q = new SelectQuery(Entity1.class);
		List results = context.performQuery(q);
		assertEquals(1, results.size());

		Entity1 o = (Entity1) results.get(0);
		assertEquals("Y", o.getName());
		assertEquals(5, DataObjectUtils.intPKForObject((Persistent) o));
	}

	public void testUpdate() throws Exception {
		getDbHelper().deleteAll("entity1");
		getDbHelper().insert("entity1", new String[] { "id", "name" },
				new Object[] { 5, "Y" });

		SelectQuery q = new SelectQuery(Entity1.class);
		List results = context.performQuery(q);
		assertEquals(1, results.size());

		Entity1 o = (Entity1) results.get(0);
		o.setName(o.getName() + "-U");
		context.commitChanges();
		assertEquals("Y-U", getDbHelper().getObject("entity1", "name"));
	}
	
	public void testDelete() throws Exception {
		getDbHelper().deleteAll("entity1");
		getDbHelper().insert("entity1", new String[] { "id", "name" },
				new Object[] { 5, "Y" });

		SelectQuery q = new SelectQuery(Entity1.class);
		List results = context.performQuery(q);
		assertEquals(1, results.size());

		Entity1 o = (Entity1) results.get(0);
		context.deleteObject(o);
		context.commitChanges();
		assertEquals(0, getDbHelper().getRowCount("entity1"));
	}
}

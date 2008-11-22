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
package org.apache.cayenne.jpa.itest.ch4;

import java.util.List;

import javax.persistence.Query;

import org.apache.cayenne.itest.jpa.EntityManagerCase;
import org.apache.cayenne.jpa.itest.ch4.entity.SimpleEntity;

public class _4_2_1_SelectStatementTest extends EntityManagerCase {

	public void testSelectFrom() throws Exception {
		getDbHelper().deleteAll("SimpleEntity");

		getDbHelper().insert("SimpleEntity",
				new String[] { "id", "property1" }, new Object[] { 15, "XXX" });

		Query query = getEntityManager().createQuery(
				"select x from SimpleEntity x");
		assertNotNull(query);
		List result = query.getResultList();
		assertNotNull(result);
		assertEquals(1, result.size());
		assertTrue(result.get(0) instanceof SimpleEntity);
		assertEquals("XXX", ((SimpleEntity) result.get(0)).getProperty1());
	}

	public void testSelectFromWhere() throws Exception {
		getDbHelper().deleteAll("SimpleEntity");

		getDbHelper().insert("SimpleEntity",
				new String[] { "id", "property1" }, new Object[] { 15, "XXX" });
		getDbHelper().insert("SimpleEntity",
				new String[] { "id", "property1" }, new Object[] { 16, "YYY" });

		Query query = getEntityManager().createQuery(
				"select x from SimpleEntity x where x.property1 = 'YYY'");
		assertNotNull(query);
		List result = query.getResultList();
		assertNotNull(result);
		assertEquals(1, result.size());
		assertTrue(result.get(0) instanceof SimpleEntity);
		assertEquals("YYY", ((SimpleEntity) result.get(0)).getProperty1());
	}
}

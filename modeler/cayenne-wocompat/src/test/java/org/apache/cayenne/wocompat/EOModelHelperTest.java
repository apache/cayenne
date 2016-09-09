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

package org.apache.cayenne.wocompat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class EOModelHelperTest {

	protected EOModelHelper helper;

	@Before
	public void setUp() throws Exception {
		URL url = getClass().getClassLoader().getResource("wotests/art.eomodeld/");
		assertNotNull(url);
		helper = new EOModelHelper(url);
	}

	@Test
	public void testModelNames() throws Exception {
		Iterator names = helper.modelNames();

		// collect to list and then analyze
		List list = new ArrayList();
		while (names.hasNext()) {
			list.add(names.next());
		}

		assertEquals(8, list.size());
		assertTrue(list.contains("Artist"));
		assertTrue(list.contains("Painting"));
		assertTrue(list.contains("ExhibitType"));
	}

	@Test
	public void testQueryNames() throws Exception {
		Iterator artistNames = helper.queryNames("Artist");
		assertFalse(artistNames.hasNext());

		Iterator etNames = helper.queryNames("ExhibitType");
		assertTrue(etNames.hasNext());

		// collect to list and then analyze
		List list = new ArrayList();
		while (etNames.hasNext()) {
			list.add(etNames.next());
		}

		assertEquals(2, list.size());
		assertTrue(list.contains("FetchAll"));
		assertTrue(list.contains("TestQuery"));
	}

	@Test
	public void testQueryPListMap() throws Exception {
		assertNull(helper.queryPListMap("Artist", "AAA"));
		assertNull(helper.queryPListMap("ExhibitType", "AAA"));

		Map query = helper.queryPListMap("ExhibitType", "FetchAll");
		assertNotNull(query);
		assertFalse(query.isEmpty());
	}

	@Test
	public void testLoadQueryIndex() throws Exception {
		Map index = helper.loadQueryIndex("ExhibitType");
		assertNotNull(index);
		assertTrue(index.containsKey("FetchAll"));
	}

	@Test
	public void testOpenQueryStream() throws Exception {
		try (InputStream in = helper.openQueryStream("ExhibitType");) {
			assertNotNull(in);
		}
	}

	@Test
	public void testOpenNonExistentQueryStream() throws Exception {
		try {
			helper.openQueryStream("Artist");
			fail("Exception expected - artist has no fetch spec.");
		} catch (IOException ioex) {
			// expected...
		}
	}
}

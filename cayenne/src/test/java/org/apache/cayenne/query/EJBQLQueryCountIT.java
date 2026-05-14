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
package org.apache.cayenne.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class EJBQLQueryCountIT {

	@RegisterExtension
	static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

	protected TableHelper tArtist;
	protected TableHelper tPainting;
	protected TableHelper tGallery;

	@BeforeEach
	public void before() throws Exception {
		tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME");

		tPainting = env.table("PAINTING", "PAINTING_ID", "ARTIST_ID", "PAINTING_TITLE");
	}

	@Test
	public void simpleCount() throws Exception {
		tArtist.insert(1, "A1");
		tArtist.insert(2, "A2");
		tArtist.insert(3, "A3");

		EJBQLQuery query = new EJBQLQuery("SELECT COUNT(a) FROM Artist a");

		// this should be simply a count of painting/artist joins
		assertEquals(Collections.singletonList(3L), env.context().performQuery(query));
	}

	@Test
	public void toOne() throws Exception {
		tArtist.insert(1, "A1");
		tArtist.insert(2, "A2");
		tArtist.insert(3, "A3");

		tPainting.insert(1, 1, "P1");
		tPainting.insert(2, 1, "P2");
		tPainting.insert(4, 2, "P1");

		EJBQLQuery query = new EJBQLQuery("SELECT COUNT(p.toArtist) FROM Painting p");

		// this should be simply a count of painting/artist joins
		assertEquals(Collections.singletonList(3L), env.context().performQuery(query));
	}

	@Test
	public void distinctToOne() throws Exception {
		tArtist.insert(1, "A1");
		tArtist.insert(2, "A2");
		tArtist.insert(3, "A3");

		tPainting.insert(1, 1, "P1");
		tPainting.insert(2, 1, "P2");
		tPainting.insert(4, 2, "P1");

		EJBQLQuery query = new EJBQLQuery("SELECT COUNT(DISTINCT p.toArtist) FROM Painting p");
		// this should be a count of artists that have paintings
		assertEquals(Collections.singletonList(2L), env.context().performQuery(query));
	}
	
	@Test
	public void distinctToOneAttribute() throws Exception {
		tArtist.insert(1, "A1");
		tArtist.insert(2, "A1");
		tArtist.insert(3, "A1");

		tPainting.insert(1, 1, "P1");
		tPainting.insert(2, 1, "P2");
		tPainting.insert(4, 2, "P1");

		EJBQLQuery query = new EJBQLQuery("SELECT COUNT(DISTINCT p.toArtist.artistName) FROM Painting p");
		// this should be a count of artists that have paintings
		assertEquals(Collections.singletonList(1L), env.context().performQuery(query));
	}
	
}

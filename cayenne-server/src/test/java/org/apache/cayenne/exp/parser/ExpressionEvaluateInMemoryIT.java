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

package org.apache.cayenne.exp.parser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Types;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class ExpressionEvaluateInMemoryIT extends ServerCase {

	@Inject
	private ServerRuntime runtime;

	@Inject
	private DataContext context;

	@Inject
	protected DBHelper dbHelper;

	protected TableHelper tArtist;
	protected TableHelper tPainting;

	@Before
	public void setUp() {
		tArtist = new TableHelper(dbHelper, "ARTIST");
		tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

		tPainting = new TableHelper(dbHelper, "PAINTING");
		tPainting.setColumns("PAINTING_ID", "ARTIST_ID", "PAINTING_TITLE", "ESTIMATED_PRICE").setColumnTypes(
				Types.INTEGER, Types.BIGINT, Types.VARCHAR, Types.DECIMAL);
	}

	protected void createTwoArtistsThreePaintings() throws Exception {

		tArtist.insert(1, "artist1");
		tArtist.insert(2, "artist2");
		tPainting.insert(1, 1, "P1", 3000);
		tPainting.insert(2, 2, "P2", 3000);
		tPainting.insert(3, null, "P3", 3000);
	}

	@Test
	public void testEvaluateOBJ_PATH_ObjEntity() {
		ASTObjPath node = new ASTObjPath("paintingArray.paintingTitle");

		ObjEntity ae = runtime.getDataDomain().getEntityResolver().getObjEntity(Artist.class);

		Object target = node.evaluate(ae);
		assertTrue(target instanceof ObjAttribute);
	}

	@Test
	public void testEvaluateDB_PATH_DbEntity() {
		Expression e = ExpressionFactory.exp("db:paintingArray.PAINTING_TITLE");

		ObjEntity ae = runtime.getDataDomain().getEntityResolver().getObjEntity(Artist.class);
		DbEntity ade = ae.getDbEntity();

		Object objTarget = e.evaluate(ae);
		assertTrue(objTarget instanceof DbAttribute);

		Object dbTarget = e.evaluate(ade);
		assertTrue(dbTarget instanceof DbAttribute);
	}

	@Test
	public void testEvaluateEQUAL_TODataObject() {
		Artist a1 = (Artist) context.newObject("Artist");
		Artist a2 = (Artist) context.newObject("Artist");
		Painting p1 = (Painting) context.newObject("Painting");
		Painting p2 = (Painting) context.newObject("Painting");
		Painting p3 = (Painting) context.newObject("Painting");

		a1.setArtistName("a1");
		a2.setArtistName("a2");
		p1.setPaintingTitle("p1");
		p2.setPaintingTitle("p2");
		p3.setPaintingTitle("p3");

		context.commitChanges();

		p1.setToArtist(a1);
		p2.setToArtist(a2);

		Expression e = new ASTEqual(new ASTObjPath("toArtist"), a1);

		assertTrue(e.match(p1));
		assertFalse(e.match(p2));
		assertFalse(e.match(p3));
	}

	@Test
	public void testEvaluateEQUAL_TO_Temp_ObjectId() {
		Artist a1 = (Artist) context.newObject("Artist");
		Artist a2 = (Artist) context.newObject("Artist");
		Painting p1 = (Painting) context.newObject("Painting");
		Painting p2 = (Painting) context.newObject("Painting");
		Painting p3 = (Painting) context.newObject("Painting");

		p1.setToArtist(a1);
		p2.setToArtist(a2);

		Expression e = new ASTEqual(new ASTObjPath("toArtist"), a1.getObjectId());

		assertTrue(e.match(p1));
		assertFalse(e.match(p2));
		assertFalse(e.match(p3));
	}

	@Test
	public void testEvaluateEQUAL_TO_Id() throws Exception {

		createTwoArtistsThreePaintings();

		Artist a1 = Cayenne.objectForPK(context, Artist.class, 1);
		Painting p1 = Cayenne.objectForPK(context, Painting.class, 1);
		Painting p2 = Cayenne.objectForPK(context, Painting.class, 2);
		Painting p3 = Cayenne.objectForPK(context, Painting.class, 3);

		Expression e = new ASTEqual(new ASTObjPath("toArtist"), Cayenne.intPKForObject(a1));

		assertTrue(e.match(p1));
		assertFalse(e.match(p2));
		assertFalse(e.match(p3));
	}

}

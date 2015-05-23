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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class ASTDbPathIT extends ServerCase {

	@Inject
	private ObjectContext context;

	@Test
	public void testEvaluate_DataObject() {

		Artist a1 = context.newObject(Artist.class);
		a1.setArtistName("a1");
		context.commitChanges();

		Expression idExp = ExpressionFactory.exp("db:ARTIST_ID");
		assertEquals(Cayenne.longPKForObject(a1), idExp.evaluate(a1));

		Expression columnExp = ExpressionFactory.exp("db:ARTIST_NAME");
		assertEquals("a1", columnExp.evaluate(a1));
	}

	@Test
	public void testEvaluate_DbEntity() {
		Expression e = ExpressionFactory.exp("db:paintingArray.PAINTING_TITLE");

		ObjEntity ae = context.getEntityResolver().getObjEntity(Artist.class);
		DbEntity ade = ae.getDbEntity();

		Object objTarget = e.evaluate(ae);
		assertTrue(objTarget instanceof DbAttribute);

		Object dbTarget = e.evaluate(ade);
		assertTrue(dbTarget instanceof DbAttribute);
	}

}

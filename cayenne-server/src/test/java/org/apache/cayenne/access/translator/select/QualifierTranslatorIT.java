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

package org.apache.cayenne.access.translator.select;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.FunctionExpressionFactory;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.query.MockQuery;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Exhibit;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.ServerCaseDataSourceFactory;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class QualifierTranslatorIT extends ServerCase {

	@Inject
	private DataNode node;

	@Inject
	private ServerCaseDataSourceFactory dataSourceFactory;

	// TODO: not an integration test; extract into *Test
	@Test
	public void testNonQualifiedQuery() throws Exception {
		TstQueryAssembler qa = new TstQueryAssembler(new MockQuery(), node.getAdapter(), node.getEntityResolver());

		try {
			new QualifierTranslator(qa).appendPart(new StringBuilder());
			fail();
		} catch (ClassCastException ccex) {
			// exception expected
		}
	}

	// TODO: not an integration test; extract into *Test
	@Test
	public void testNullQualifier() throws Exception {
		TstQueryAssembler qa = new TstQueryAssembler(new SelectQuery<Object>(), node.getAdapter(),
				node.getEntityResolver());

		StringBuilder out = new StringBuilder();
		new QualifierTranslator(qa).appendPart(out);
		assertEquals(0, out.length());
	}

	@Test
	public void testBinary_In1() throws Exception {
		doExpressionTest(Exhibit.class, "toGallery.galleryName in ('g1', 'g2', 'g3')", "ta.GALLERY_NAME IN (?, ?, ?)");
	}

	@Test
	public void testBinary_In2() throws Exception {
		Expression exp = ExpressionFactory.inExp("toGallery.galleryName",
				Arrays.asList(new Object[] { "g1", "g2", "g3" }));
		doExpressionTest(Exhibit.class, exp, "ta.GALLERY_NAME IN (?, ?, ?)");
	}

	@Test
	public void testBinary_In3() throws Exception {
		Expression exp = ExpressionFactory.inExp("toGallery.galleryName", new Object[] { "g1", "g2", "g3" });
		doExpressionTest(Exhibit.class, exp, "ta.GALLERY_NAME IN (?, ?, ?)");
	}

	@Test
	public void testBinary_Like() throws Exception {
		doExpressionTest(Exhibit.class, "toGallery.galleryName like 'a%'", "ta.GALLERY_NAME LIKE ?");
	}

	@Test
	public void testBinary_LikeIgnoreCase() throws Exception {
		doExpressionTest(Exhibit.class, "toGallery.galleryName likeIgnoreCase 'a%'",
				"UPPER(ta.GALLERY_NAME) LIKE UPPER(?)");
	}

	@Test
	public void testBinary_IsNull() throws Exception {
		doExpressionTest(Exhibit.class, "toGallery.galleryName = null", "ta.GALLERY_NAME IS NULL");
	}

	@Test
	public void testBinary_IsNotNull() throws Exception {
		doExpressionTest(Exhibit.class, "toGallery.galleryName != null", "ta.GALLERY_NAME IS NOT NULL");
	}

	@Test
	public void testTernary_Between() throws Exception {
		doExpressionTest(Painting.class, "estimatedPrice between 3000 and 15000", "ta.ESTIMATED_PRICE BETWEEN ? AND ?");
	}

	@Test
	public void testExtras() throws Exception {
		ObjectId oid1 = new ObjectId("Gallery", "GALLERY_ID", 1);
		ObjectId oid2 = new ObjectId("Gallery", "GALLERY_ID", 2);
		Gallery g1 = new Gallery();
		Gallery g2 = new Gallery();
		g1.setObjectId(oid1);
		g2.setObjectId(oid2);

		Expression e1 = ExpressionFactory.matchExp("toGallery", g1);
		Expression e2 = e1.orExp(ExpressionFactory.matchExp("toGallery", g2));

		doExpressionTest(Exhibit.class, e2, "(ta.GALLERY_ID = ?) OR (ta.GALLERY_ID = ?)");
	}

	@Test
	public void testTrim() throws Exception {
		Expression exp = FunctionExpressionFactory.trimExp(Artist.ARTIST_NAME.path());
		Property<String> property = Property.create("trimmedName", exp, String.class);

		doExpressionTest(Artist.class, property.like("P%"), "TRIM(ta.ARTIST_NAME) LIKE ?");
	}

	@Test
	public void testConcat() throws Exception {
		Expression exp = FunctionExpressionFactory.concatExp("artistName", "dateOfBirth");

		Property<String> property = Property.create("concatNameAndDate", exp, String.class);

		doExpressionTest(Artist.class, property.like("P%"), "CONCAT(ta.ARTIST_NAME, ta.DATE_OF_BIRTH) LIKE ?");
	}

	private void doExpressionTest(Class<?> queryType, String qualifier, String expectedSQL) throws Exception {
		doExpressionTest(queryType, ExpressionFactory.exp(qualifier), expectedSQL);
	}

	private void doExpressionTest(Class<?> queryType, Expression qualifier, String expectedSQL) throws Exception {

		SelectQuery<?> q = new SelectQuery<>(queryType);
		q.setQualifier(qualifier);

		TstQueryAssembler qa = new TstQueryAssembler(q, node.getAdapter(), node.getEntityResolver());

		StringBuilder out = new StringBuilder();
		String translated = new QualifierTranslator(qa).appendPart(out).toString();
		assertEquals(expectedSQL, translated);

	}
}

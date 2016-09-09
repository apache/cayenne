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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.List;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.ArtistExhibit;
import org.apache.cayenne.testdo.testmap.CompoundPainting;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DefaultSelectTranslatorIT extends ServerCase {

	@Inject
	private DataContext context;

	@Inject
	private UnitDbAdapter unitAdapter;

	@Inject
	private DBHelper dbHelper;

	@Inject
	private JdbcEventLogger logger;

	@Inject
	private DataNode dataNode;

	/**
	 * Tests query creation with qualifier and ordering.
	 */
	@Test
	public void testCreateSqlString1() throws Exception {
		// query with qualifier and ordering
		SelectQuery<Artist> q = new SelectQuery<Artist>(Artist.class, ExpressionFactory.likeExp("artistName", "a%"));
		q.addOrdering("dateOfBirth", SortOrder.ASCENDING);

		String generatedSql = new DefaultSelectTranslator(q, dataNode.getAdapter(), dataNode.getEntityResolver())
				.getSql();

		// do some simple assertions to make sure all parts are in
		assertNotNull(generatedSql);
		assertTrue(generatedSql.startsWith("SELECT "));
		assertTrue(generatedSql.indexOf(" FROM ") > 0);
		assertTrue(generatedSql.indexOf(" WHERE ") > generatedSql.indexOf(" FROM "));
		assertTrue(generatedSql.indexOf(" ORDER BY ") > generatedSql.indexOf(" WHERE "));
	}

	/**
	 * Tests query creation with qualifier and ordering.
	 */
	@Test
	public void testDbEntityQualifier() throws Exception {

		SelectQuery<Artist> q = new SelectQuery<Artist>(Artist.class);
		final DbEntity entity = context.getEntityResolver().getDbEntity("ARTIST");
		final DbEntity middleEntity = context.getEntityResolver().getDbEntity("ARTIST_GROUP");

		DefaultSelectTranslator transl = new DefaultSelectTranslator(q, dataNode.getAdapter(),
				dataNode.getEntityResolver());

		entity.setQualifier(ExpressionFactory.exp("ARTIST_NAME = \"123\""));
		middleEntity.setQualifier(ExpressionFactory.exp("GROUP_ID = 1987"));

		try {

			String generatedSql = transl.getSql();

			// do some simple assertions to make sure all parts are in
			assertNotNull(generatedSql);
			assertTrue(generatedSql.startsWith("SELECT "));
			assertTrue(generatedSql.indexOf(" FROM ") > 0);
			if (generatedSql.contains("RTRIM")) {
				assertTrue(generatedSql.indexOf("ARTIST_NAME) = ") > generatedSql.indexOf("RTRIM("));
			} else if (generatedSql.contains("TRIM")) {
				assertTrue(generatedSql.indexOf("ARTIST_NAME) = ") > generatedSql.indexOf("TRIM("));
			} else {
				assertTrue(generatedSql.indexOf("ARTIST_NAME = ") > 0);
			}
		} finally {
			entity.setQualifier(null);
			middleEntity.setQualifier(null);
		}
	}

	@Test
	public void testDbEntityQualifier_OuterJoin() throws Exception {

		SelectQuery<Painting> q = SelectQuery.query(Painting.class);
		q.addOrdering("toArtist+.artistName", SortOrder.ASCENDING);

		final DbEntity entity = context.getEntityResolver().getDbEntity("ARTIST");
		final DbEntity middleEntity = context.getEntityResolver().getDbEntity("ARTIST_GROUP");

		DefaultSelectTranslator transl = new DefaultSelectTranslator(q, dataNode.getAdapter(),
				dataNode.getEntityResolver());

		entity.setQualifier(ExpressionFactory.exp("ARTIST_NAME = \"123\""));
		middleEntity.setQualifier(ExpressionFactory.exp("GROUP_ID = 1987"));

		try {

			String generatedSql = transl.getSql();

			// do some simple assertions to make sure all parts are in
			assertNotNull(generatedSql);
			assertTrue(generatedSql.startsWith("SELECT "));
			assertTrue(generatedSql.indexOf(" FROM ") > 0);
			if (generatedSql.contains("RTRIM")) {
				assertTrue(generatedSql.indexOf("ARTIST_NAME) = ") > generatedSql.indexOf("RTRIM("));
			} else if (generatedSql.contains("TRIM")) {
				assertTrue(generatedSql.indexOf("ARTIST_NAME) = ") > generatedSql.indexOf("TRIM("));
			} else {
				assertTrue(generatedSql.indexOf("ARTIST_NAME = ") > 0);
			}

		} finally {
			entity.setQualifier(null);
			middleEntity.setQualifier(null);
		}
	}

	@Test
	public void testDbEntityQualifier_FlattenedRel() throws Exception {

		SelectQuery<Artist> q = new SelectQuery<Artist>(Artist.class, ExpressionFactory.matchExp("groupArray.name",
				"bar"));

		final DbEntity entity = context.getEntityResolver().getDbEntity("ARTIST");
		final DbEntity middleEntity = context.getEntityResolver().getDbEntity("ARTIST_GROUP");

		DefaultSelectTranslator transl = new DefaultSelectTranslator(q, dataNode.getAdapter(),
				dataNode.getEntityResolver());

		entity.setQualifier(ExpressionFactory.exp("ARTIST_NAME = \"123\""));
		middleEntity.setQualifier(ExpressionFactory.exp("GROUP_ID = 1987"));

		try {

			String generatedSql = transl.getSql();

			// do some simple assertions to make sure all parts are in
			assertNotNull(generatedSql);
			assertTrue(generatedSql.startsWith("SELECT "));
			assertTrue(generatedSql.indexOf(" FROM ") > 0);
			if (generatedSql.contains("RTRIM")) {
				assertTrue(generatedSql.indexOf("ARTIST_NAME) = ") > generatedSql.indexOf("RTRIM("));
			} else if (generatedSql.contains("TRIM")) {
				assertTrue(generatedSql.indexOf("ARTIST_NAME) = ") > generatedSql.indexOf("TRIM("));
			} else {
				assertTrue(generatedSql.indexOf("ARTIST_NAME = ") > 0);
			}

		} finally {
			entity.setQualifier(null);
			middleEntity.setQualifier(null);
		}
	}

	@Test
	public void testDbEntityQualifier_RelatedMatch() throws Exception {

		SelectQuery<Artist> q = new SelectQuery(Painting.class,
				ExpressionFactory.matchExp("toArtist.artistName", "foo"));

		final DbEntity entity = context.getEntityResolver().getDbEntity("ARTIST");
		final DbEntity middleEntity = context.getEntityResolver().getDbEntity("ARTIST_GROUP");

		DefaultSelectTranslator transl = new DefaultSelectTranslator(q, dataNode.getAdapter(),
				dataNode.getEntityResolver());

		entity.setQualifier(ExpressionFactory.exp("ARTIST_NAME = \"123\""));
		middleEntity.setQualifier(ExpressionFactory.exp("GROUP_ID = 1987"));

		try {

			String generatedSql = transl.getSql();

			// do some simple assertions to make sure all parts are in
			assertNotNull(generatedSql);
			assertTrue(generatedSql.startsWith("SELECT "));
			assertTrue(generatedSql.indexOf(" FROM ") > 0);
			if (generatedSql.contains("RTRIM")) {
				assertTrue(generatedSql.indexOf("ARTIST_NAME) = ") > generatedSql.indexOf("RTRIM("));
			} else if (generatedSql.contains("TRIM")) {
				assertTrue(generatedSql.indexOf("ARTIST_NAME) = ") > generatedSql.indexOf("TRIM("));
			} else {
				assertTrue(generatedSql.indexOf("ARTIST_NAME = ") > 0);
			}

		} finally {
			entity.setQualifier(null);
			middleEntity.setQualifier(null);
		}
	}

	/**
	 * Tests query creation with "distinct" specified.
	 */
	@Test
	public void testCreateSqlString2() throws Exception {
		// query with "distinct" set
		SelectQuery q = new SelectQuery(Artist.class);
		q.setDistinct(true);

		String generatedSql = new DefaultSelectTranslator(q, dataNode.getAdapter(), dataNode.getEntityResolver())
				.getSql();

		// do some simple assertions to make sure all parts are in
		assertNotNull(generatedSql);
		assertTrue(generatedSql.startsWith("SELECT DISTINCT"));
	}

	/**
	 * Test aliases when the same table used in more then 1 relationship. Check
	 * translation of relationship path "ArtistExhibit.toArtist.artistName" and
	 * "ArtistExhibit.toExhibit.toGallery.paintingArray.toArtist.artistName".
	 */
	@Test
	public void testCreateSqlString5() throws Exception {
		// query with qualifier and ordering
		SelectQuery q = new SelectQuery(ArtistExhibit.class);
		q.setQualifier(ExpressionFactory.likeExp("toArtist.artistName", "a%"));
		q.andQualifier(ExpressionFactory.likeExp("toExhibit.toGallery.paintingArray.toArtist.artistName", "a%"));

		String generatedSql = new DefaultSelectTranslator(q, dataNode.getAdapter(), dataNode.getEntityResolver())
				.getSql();
		// logObj.warn("Query: " + generatedSql);

		// do some simple assertions to make sure all parts are in
		assertNotNull(generatedSql);
		assertTrue(generatedSql.startsWith("SELECT "));
		assertTrue(generatedSql.indexOf(" FROM ") > 0);
		assertTrue(generatedSql.indexOf(" WHERE ") > generatedSql.indexOf(" FROM "));

		// check that there are 2 distinct aliases for the ARTIST table
		int ind1 = generatedSql.indexOf("ARTIST t", generatedSql.indexOf(" FROM "));
		assertTrue(ind1 > 0);

		int ind2 = generatedSql.indexOf("ARTIST t", ind1 + 1);
		assertTrue(ind2 > 0);

		assertTrue(generatedSql.charAt(ind1 + "ARTIST t".length()) != generatedSql.charAt(ind2 + "ARTIST t".length()));
	}

	/**
	 * Test aliases when the same table used in more then 1 relationship. Check
	 * translation of relationship path "ArtistExhibit.toArtist.artistName" and
	 * "ArtistExhibit.toArtist.paintingArray.paintingTitle".
	 */
	@Test
	public void testCreateSqlString6() throws Exception {
		// query with qualifier and ordering
		SelectQuery q = new SelectQuery(ArtistExhibit.class);
		q.setQualifier(ExpressionFactory.likeExp("toArtist.artistName", "a%"));
		q.andQualifier(ExpressionFactory.likeExp("toArtist.paintingArray.paintingTitle", "p%"));

		String generatedSql = new DefaultSelectTranslator(q, dataNode.getAdapter(), dataNode.getEntityResolver())
				.getSql();

		// do some simple assertions to make sure all parts are in
		assertNotNull(generatedSql);
		assertTrue(generatedSql.startsWith("SELECT "));
		assertTrue(generatedSql.indexOf(" FROM ") > 0);
		assertTrue(generatedSql.indexOf(" WHERE ") > generatedSql.indexOf(" FROM "));

		// check that there is only one distinct alias for the ARTIST
		// table
		int ind1 = generatedSql.indexOf("ARTIST t", generatedSql.indexOf(" FROM "));
		assertTrue(ind1 > 0);

		int ind2 = generatedSql.indexOf("ARTIST t", ind1 + 1);
		assertTrue(generatedSql, ind2 < 0);
	}

	/**
	 * Test query when qualifying on the same attribute more than once. Check
	 * translation "Artist.dateOfBirth > ? AND Artist.dateOfBirth < ?".
	 */
	@Test
	public void testCreateSqlString7() throws Exception {
		SelectQuery q = new SelectQuery(Artist.class);
		q.setQualifier(ExpressionFactory.greaterExp("dateOfBirth", new Date()));
		q.andQualifier(ExpressionFactory.lessExp("dateOfBirth", new Date()));

		String generatedSql = new DefaultSelectTranslator(q, dataNode.getAdapter(), dataNode.getEntityResolver())
				.getSql();
		// logObj.warn("Query: " + generatedSql);

		// do some simple assertions to make sure all parts are in
		assertNotNull(generatedSql);
		assertTrue(generatedSql.startsWith("SELECT "));

		int i1 = generatedSql.indexOf(" FROM ");
		assertTrue(i1 > 0);

		int i2 = generatedSql.indexOf(" WHERE ");
		assertTrue(i2 > i1);

		int i3 = generatedSql.indexOf("DATE_OF_BIRTH", i2 + 1);
		assertTrue(i3 > i2);

		int i4 = generatedSql.indexOf("DATE_OF_BIRTH", i3 + 1);
		assertTrue("No second DOB comparison: " + i4 + ", " + i3, i4 > i3);
	}

	/**
	 * Test query when qualifying on the same attribute accessed over
	 * relationship, more than once. Check translation
	 * "Painting.toArtist.dateOfBirth > ? AND Painting.toArtist.dateOfBirth <
	 * ?".
	 */
	@Test
	public void testCreateSqlString8() throws Exception {
		SelectQuery q = new SelectQuery();
		q.setRoot(Painting.class);
		q.setQualifier(ExpressionFactory.greaterExp("toArtist.dateOfBirth", new Date()));
		q.andQualifier(ExpressionFactory.lessExp("toArtist.dateOfBirth", new Date()));

		String generatedSql = new DefaultSelectTranslator(q, dataNode.getAdapter(), dataNode.getEntityResolver())
				.getSql();

		// do some simple assertions to make sure all parts are in
		assertNotNull(generatedSql);
		assertTrue(generatedSql.startsWith("SELECT "));

		int i1 = generatedSql.indexOf(" FROM ");
		assertTrue(i1 > 0);

		int i2 = generatedSql.indexOf(" WHERE ");
		assertTrue(i2 > i1);

		int i3 = generatedSql.indexOf("DATE_OF_BIRTH", i2 + 1);
		assertTrue(i3 > i2);

		int i4 = generatedSql.indexOf("DATE_OF_BIRTH", i3 + 1);
		assertTrue("No second DOB comparison: " + i4 + ", " + i3, i4 > i3);
	}

	@Test
	public void testCreateSqlString9() throws Exception {
		// query for a compound ObjEntity with qualifier
		SelectQuery q = new SelectQuery(CompoundPainting.class, ExpressionFactory.likeExp("artistName", "a%"));

		String sql = new DefaultSelectTranslator(q, dataNode.getAdapter(), dataNode.getEntityResolver()).getSql();

		// do some simple assertions to make sure all parts are in
		assertNotNull(sql);
		assertTrue(sql.startsWith("SELECT "));

		int i1 = sql.indexOf(" FROM ");
		assertTrue(i1 > 0);

		int i2 = sql.indexOf("PAINTING");
		assertTrue(i2 > 0);

		int i3 = sql.indexOf("ARTIST");
		assertTrue(i3 > 0);

		int i4 = sql.indexOf("GALLERY");
		assertTrue(i4 > 0);

		int i5 = sql.indexOf("PAINTING_INFO");
		assertTrue(i5 > 0);

		int i6 = sql.indexOf("ARTIST_NAME");
		assertTrue(i6 > 0);

		int i7 = sql.indexOf("ESTIMATED_PRICE");
		assertTrue(i7 > 0);

		int i8 = sql.indexOf("GALLERY_NAME");
		assertTrue(i8 > 0);

		int i9 = sql.indexOf("PAINTING_TITLE");
		assertTrue(i9 > 0);

		int i10 = sql.indexOf("TEXT_REVIEW");
		assertTrue(i10 > 0);

		int i11 = sql.indexOf("PAINTING_ID");
		assertTrue(i11 > 0);

		int i12 = sql.indexOf("ARTIST_ID");
		assertTrue(i12 > 0);

		int i13 = sql.indexOf("GALLERY_ID");
		assertTrue(i13 > 0);

	}

	@Test
	public void testCreateSqlString10() throws Exception {
		// query with to-many joint prefetches
		SelectQuery q = new SelectQuery(Artist.class);
		q.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY).setSemantics(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

		DefaultSelectTranslator transl = new DefaultSelectTranslator(q, dataNode.getAdapter(),
				dataNode.getEntityResolver());
		String sql = transl.getSql();
		assertNotNull(sql);
		assertTrue(sql.startsWith("SELECT "));

		int i1 = sql.indexOf("ARTIST_ID");
		assertTrue(sql, i1 > 0);

		int i2 = sql.indexOf("FROM");
		assertTrue(sql, i2 > 0);

		assertTrue(sql, sql.indexOf("PAINTING_ID") > 0);

		// assert we have one join
		assertEquals(1, transl.joinStack.size());
	}

	@Test
	public void testCreateSqlString11() throws Exception {
		// query with joint prefetches and other joins
		SelectQuery q = new SelectQuery(Artist.class, ExpressionFactory.exp("paintingArray.paintingTitle = 'a'"));
		q.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY).setSemantics(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

		DefaultSelectTranslator transl = new DefaultSelectTranslator(q, dataNode.getAdapter(),
				dataNode.getEntityResolver());

		transl.getSql();

		// assert we only have one join
		assertEquals(2, transl.joinStack.size());
	}

	@Test
	public void testCreateSqlString12() throws Exception {
		// query with to-one joint prefetches
		SelectQuery q = new SelectQuery(Painting.class);
		q.addPrefetch(Painting.TO_ARTIST_PROPERTY).setSemantics(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

		DefaultSelectTranslator transl = new DefaultSelectTranslator(q, dataNode.getAdapter(),
				dataNode.getEntityResolver());

		String sql = transl.getSql();
		assertNotNull(sql);
		assertTrue(sql.startsWith("SELECT "));

		int i1 = sql.indexOf("ARTIST_ID");
		assertTrue(sql, i1 > 0);

		int i2 = sql.indexOf("FROM");
		assertTrue(sql, i2 > 0);

		assertTrue(sql, sql.indexOf("PAINTING_ID") > 0);

		// assert we have one join
		assertEquals(1, transl.joinStack.size());
	}

	@Test
	public void testCreateSqlString13() throws Exception {
		// query with invalid joint prefetches
		SelectQuery q = new SelectQuery(Painting.class);
		q.addPrefetch("invalid.invalid").setSemantics(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

		try {
			new DefaultSelectTranslator(q, dataNode.getAdapter(), dataNode.getEntityResolver()).getSql();
			fail("Invalid jointPrefetch must have thrown...");
		} catch (ExpressionException e) {
			// expected
		}
	}

	@Test
	public void testCreateSqlStringWithQuoteSqlIdentifiers() throws Exception {

		try {
			SelectQuery q = new SelectQuery(Artist.class);
			DbEntity entity = context.getEntityResolver().getDbEntity("ARTIST");
			entity.getDataMap().setQuotingSQLIdentifiers(true);
			q.addOrdering("dateOfBirth", SortOrder.ASCENDING);

			String charStart = unitAdapter.getIdentifiersStartQuote();
			String charEnd = unitAdapter.getIdentifiersEndQuote();

			String s = new DefaultSelectTranslator(q, dataNode.getAdapter(), dataNode.getEntityResolver()).getSql();
			assertTrue(s.startsWith("SELECT "));
			int iFrom = s.indexOf(" FROM ");
			assertTrue(iFrom > 0);
			int artistName = s.indexOf(charStart + "t0" + charEnd + "." + charStart + "ARTIST_NAME" + charEnd);
			assertTrue(artistName > 0 && artistName < iFrom);
			int artistId = s.indexOf(charStart + "t0" + charEnd + "." + charStart + "ARTIST_ID" + charEnd);
			assertTrue(artistId > 0 && artistId < iFrom);
			int dateOfBirth = s.indexOf(charStart + "t0" + charEnd + "." + charStart + "DATE_OF_BIRTH" + charEnd);
			assertTrue(dateOfBirth > 0 && dateOfBirth < iFrom);
			int iArtist = s.indexOf(charStart + "ARTIST" + charEnd + " " + charStart + "t0" + charEnd);
			assertTrue(iArtist > iFrom);
			int iOrderBy = s.indexOf(" ORDER BY ");
			int dateOfBirth2 = s.indexOf(charStart + "t0" + charEnd + "." + charStart + "DATE_OF_BIRTH" + charEnd,
					iOrderBy);
			assertTrue(iOrderBy > iArtist);
			assertTrue(dateOfBirth2 > iOrderBy);

		} finally {
			DbEntity entity = context.getEntityResolver().getDbEntity("ARTIST");
			entity.getDataMap().setQuotingSQLIdentifiers(false);
		}

	}

	@Test
	public void testCreateSqlStringWithQuoteSqlIdentifiers2() throws Exception {

		try {
			SelectQuery q = new SelectQuery(Artist.class);
			DbEntity entity = context.getEntityResolver().getDbEntity("ARTIST");
			entity.getDataMap().setQuotingSQLIdentifiers(true);
			q.setQualifier(ExpressionFactory.greaterExp("dateOfBirth", new Date()));
			q.andQualifier(ExpressionFactory.lessExp("dateOfBirth", new Date()));

			String charStart = unitAdapter.getIdentifiersStartQuote();
			String charEnd = unitAdapter.getIdentifiersEndQuote();

			String s = new DefaultSelectTranslator(q, dataNode.getAdapter(), dataNode.getEntityResolver()).getSql();

			assertTrue(s.startsWith("SELECT "));
			int iFrom = s.indexOf(" FROM ");
			assertTrue(iFrom > 0);
			int artistName = s.indexOf(charStart + "t0" + charEnd + "." + charStart + "ARTIST_NAME" + charEnd);
			assertTrue(artistName > 0 && artistName < iFrom);
			int artistId = s.indexOf(charStart + "t0" + charEnd + "." + charStart + "ARTIST_ID" + charEnd);
			assertTrue(artistId > 0 && artistId < iFrom);
			int dateOfBirth = s.indexOf(charStart + "t0" + charEnd + "." + charStart + "DATE_OF_BIRTH" + charEnd);
			assertTrue(dateOfBirth > 0 && dateOfBirth < iFrom);
			int iArtist = s.indexOf(charStart + "ARTIST" + charEnd + " " + charStart + "t0" + charEnd);
			assertTrue(iArtist > iFrom);
			int iWhere = s.indexOf(" WHERE ");
			assertTrue(iWhere > iArtist);

			int dateOfBirth2 = s.indexOf(charStart + "t0" + charEnd + "." + charStart + "DATE_OF_BIRTH" + charEnd
					+ " > ?");
			assertTrue(dateOfBirth2 > iWhere);

			int iAnd = s.indexOf(" AND ");
			assertTrue(iAnd > iWhere);
			int dateOfBirth3 = s.indexOf(charStart + "t0" + charEnd + "." + charStart + "DATE_OF_BIRTH" + charEnd
					+ " < ?");
			assertTrue(dateOfBirth3 > iAnd);

		} finally {
			DbEntity entity = context.getEntityResolver().getDbEntity("ARTIST");
			entity.getDataMap().setQuotingSQLIdentifiers(false);
		}
	}

	@Test
	public void testCreateSqlStringWithQuoteSqlIdentifiers3() throws Exception {

		// query with joint prefetches and other joins
		// and with QuoteSqlIdentifiers = true
		try {
			SelectQuery q = new SelectQuery(Artist.class, ExpressionFactory.exp("paintingArray.paintingTitle = 'a'"));
			q.addPrefetch(Artist.PAINTING_ARRAY.joint());

			DbEntity entity = context.getEntityResolver().getDbEntity("ARTIST");
			entity.getDataMap().setQuotingSQLIdentifiers(true);

			String charStart = unitAdapter.getIdentifiersStartQuote();
			String charEnd = unitAdapter.getIdentifiersEndQuote();

			String s = new DefaultSelectTranslator(q, dataNode.getAdapter(), dataNode.getEntityResolver()).getSql();

			assertTrue(s.startsWith("SELECT DISTINCT "));
			int iFrom = s.indexOf(" FROM ");
			assertTrue(iFrom > 0);
			int artistName = s.indexOf(charStart + "t0" + charEnd + "." + charStart + "ARTIST_NAME" + charEnd);
			assertTrue(artistName > 0 && artistName < iFrom);
			int artistId = s.indexOf(charStart + "t0" + charEnd + "." + charStart + "ARTIST_ID" + charEnd);
			assertTrue(artistId > 0 && artistId < iFrom);
			int dateOfBirth = s.indexOf(charStart + "t0" + charEnd + "." + charStart + "DATE_OF_BIRTH" + charEnd);
			assertTrue(dateOfBirth > 0 && dateOfBirth < iFrom);
			int estimatedPrice = s.indexOf(charStart + "t1" + charEnd + "." + charStart + "ESTIMATED_PRICE" + charEnd);
			assertTrue(estimatedPrice > 0 && estimatedPrice < iFrom);
			int paintingDescription = s.indexOf(charStart + "t1" + charEnd + "." + charStart + "PAINTING_DESCRIPTION"
					+ charEnd);
			assertTrue(paintingDescription > 0 && paintingDescription < iFrom);
			int paintingTitle = s.indexOf(charStart + "t1" + charEnd + "." + charStart + "PAINTING_TITLE" + charEnd);
			assertTrue(paintingTitle > 0 && paintingTitle < iFrom);
			int artistIdT1 = s.indexOf(charStart + "t1" + charEnd + "." + charStart + "ARTIST_ID" + charEnd);
			assertTrue(artistIdT1 > 0 && artistIdT1 < iFrom);
			int galleryId = s.indexOf(charStart + "t1" + charEnd + "." + charStart + "GALLERY_ID" + charEnd);
			assertTrue(galleryId > 0 && galleryId < iFrom);
			int paintingId = s.indexOf(charStart + "t1" + charEnd + "." + charStart + "PAINTING_ID" + charEnd);
			assertTrue(paintingId > 0 && paintingId < iFrom);
			int iArtist = s.indexOf(charStart + "ARTIST" + charEnd + " " + charStart + "t0" + charEnd);
			assertTrue(iArtist > iFrom);
			int iLeftJoin = s.indexOf("LEFT JOIN");
			assertTrue(iLeftJoin > iFrom);
			int iPainting = s.indexOf(charStart + "PAINTING" + charEnd + " " + charStart + "t1" + charEnd);
			assertTrue(iPainting > iLeftJoin);
			int iOn = s.indexOf(" ON ");
			assertTrue(iOn > iLeftJoin);
			int iArtistId = s.indexOf(charStart + "t0" + charEnd + "." + charStart + "ARTIST_ID" + charEnd, iLeftJoin);
			assertTrue(iArtistId > iOn);
			int iArtistIdT1 = s
					.indexOf(charStart + "t1" + charEnd + "." + charStart + "ARTIST_ID" + charEnd, iLeftJoin);
			assertTrue(iArtistIdT1 > iOn);
			int i = s.indexOf("=", iLeftJoin);
			assertTrue(iArtistIdT1 > i || iArtistId > i);
			int iJoin = s.indexOf("JOIN");
			assertTrue(iJoin > iLeftJoin);
			int iPainting2 = s.indexOf(charStart + "PAINTING" + charEnd + " " + charStart + "t2" + charEnd);
			assertTrue(iPainting2 > iJoin);
			int iOn2 = s.indexOf(" ON ");
			assertTrue(iOn2 > iJoin);
			int iArtistId2 = s.indexOf(charStart + "t0" + charEnd + "." + charStart + "ARTIST_ID" + charEnd, iJoin);
			assertTrue(iArtistId2 > iOn2);
			int iArtistId2T2 = s.indexOf(charStart + "t2" + charEnd + "." + charStart + "ARTIST_ID" + charEnd, iJoin);
			assertTrue(iArtistId2T2 > iOn2);
			int i2 = s.indexOf("=", iJoin);
			assertTrue(iArtistId2T2 > i2 || iArtistId2 > i2);
			int iWhere = s.indexOf(" WHERE ");
			assertTrue(iWhere > iJoin);

			int paintingTitle2 = s.indexOf(charStart + "t2" + charEnd + "." + charStart + "PAINTING_TITLE" + charEnd
					+ " = ?");
			assertTrue(paintingTitle2 > iWhere);

		} finally {
			DbEntity entity = context.getEntityResolver().getDbEntity("ARTIST");
			entity.getDataMap().setQuotingSQLIdentifiers(false);
		}
	}

	@Test
	public void testCreateSqlStringWithQuoteSqlIdentifiers4() throws Exception {

		// query with to-one joint prefetches
		// and with QuoteSqlIdentifiers = true
		try {
			SelectQuery q = new SelectQuery(Painting.class);
			q.addPrefetch(Painting.TO_ARTIST_PROPERTY).setSemantics(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

			DbEntity entity = context.getEntityResolver().getDbEntity("PAINTING");
			entity.getDataMap().setQuotingSQLIdentifiers(true);

			String charStart = unitAdapter.getIdentifiersStartQuote();
			String charEnd = unitAdapter.getIdentifiersEndQuote();

			String s = new DefaultSelectTranslator(q, dataNode.getAdapter(), dataNode.getEntityResolver()).getSql();

			assertTrue(s.startsWith("SELECT "));
			int iFrom = s.indexOf(" FROM ");
			assertTrue(iFrom > 0);

			int paintingDescription = s.indexOf(charStart + "t0" + charEnd + "." + charStart + "PAINTING_DESCRIPTION"
					+ charEnd);
			assertTrue(paintingDescription > 0 && paintingDescription < iFrom);
			int paintingTitle = s.indexOf(charStart + "t0" + charEnd + "." + charStart + "PAINTING_TITLE" + charEnd);
			assertTrue(paintingTitle > 0 && paintingTitle < iFrom);
			int artistIdT1 = s.indexOf(charStart + "t0" + charEnd + "." + charStart + "ARTIST_ID" + charEnd);
			assertTrue(artistIdT1 > 0 && artistIdT1 < iFrom);
			int estimatedPrice = s.indexOf(charStart + "t0" + charEnd + "." + charStart + "ESTIMATED_PRICE" + charEnd);
			assertTrue(estimatedPrice > 0 && estimatedPrice < iFrom);
			int galleryId = s.indexOf(charStart + "t0" + charEnd + "." + charStart + "GALLERY_ID" + charEnd);
			assertTrue(galleryId > 0 && galleryId < iFrom);
			int paintingId = s.indexOf(charStart + "t0" + charEnd + "." + charStart + "PAINTING_ID" + charEnd);
			assertTrue(paintingId > 0 && paintingId < iFrom);
			int artistName = s.indexOf(charStart + "t1" + charEnd + "." + charStart + "ARTIST_NAME" + charEnd);
			assertTrue(artistName > 0 && artistName < iFrom);
			int artistId = s.indexOf(charStart + "t1" + charEnd + "." + charStart + "ARTIST_ID" + charEnd);
			assertTrue(artistId > 0 && artistId < iFrom);
			int dateOfBirth = s.indexOf(charStart + "t1" + charEnd + "." + charStart + "DATE_OF_BIRTH" + charEnd);
			assertTrue(dateOfBirth > 0 && dateOfBirth < iFrom);
			int iPainting = s.indexOf(charStart + "PAINTING" + charEnd + " " + charStart + "t0" + charEnd);
			assertTrue(iPainting > iFrom);

			int iLeftJoin = s.indexOf("LEFT JOIN");
			assertTrue(iLeftJoin > iFrom);
			int iArtist = s.indexOf(charStart + "ARTIST" + charEnd + " " + charStart + "t1" + charEnd);
			assertTrue(iArtist > iLeftJoin);
			int iOn = s.indexOf(" ON ");
			assertTrue(iOn > iLeftJoin);
			int iArtistId = s.indexOf(charStart + "t0" + charEnd + "." + charStart + "ARTIST_ID" + charEnd, iLeftJoin);
			assertTrue(iArtistId > iOn);
			int iArtistIdT1 = s
					.indexOf(charStart + "t1" + charEnd + "." + charStart + "ARTIST_ID" + charEnd, iLeftJoin);
			assertTrue(iArtistIdT1 > iOn);
			int i = s.indexOf("=", iLeftJoin);
			assertTrue(iArtistIdT1 > i || iArtistId > i);

		} finally {
			DbEntity entity = context.getEntityResolver().getDbEntity("PAINTING");
			entity.getDataMap().setQuotingSQLIdentifiers(false);
		}
	}

	/**
	 * Tests columns generated for a simple object query.
	 */
	@Test
	public void testBuildResultColumns1() throws Exception {
		SelectQuery q = new SelectQuery(Painting.class);
		DefaultSelectTranslator tr = new DefaultSelectTranslator(q, dataNode.getAdapter(), dataNode.getEntityResolver());

		List<?> columns = tr.buildResultColumns();

		// all DbAttributes must be included
		DbEntity entity = context.getEntityResolver().getDbEntity("PAINTING");
		for (final DbAttribute a : entity.getAttributes()) {
			ColumnDescriptor c = new ColumnDescriptor(a, "t0");
			assertTrue("No descriptor for " + a + ", columns: " + columns, columns.contains(c));
		}
	}

	/**
	 * Tests columns generated for an object query with joint prefetch.
	 */
	@Test
	public void testBuildResultColumns2() throws Exception {
		SelectQuery q = new SelectQuery(Painting.class);
		q.addPrefetch(Painting.TO_ARTIST_PROPERTY).setSemantics(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
		DefaultSelectTranslator tr = new DefaultSelectTranslator(q, dataNode.getAdapter(), dataNode.getEntityResolver());

		List<?> columns = tr.buildResultColumns();

		// assert root entity columns
		DbEntity entity = context.getEntityResolver().getDbEntity("PAINTING");
		for (final DbAttribute a : entity.getAttributes()) {
			ColumnDescriptor c = new ColumnDescriptor(a, "t0");
			assertTrue("No descriptor for " + a + ", columns: " + columns, columns.contains(c));
		}

		// assert joined columns
		DbEntity joined = context.getEntityResolver().getDbEntity("ARTIST");
		for (final DbAttribute a : joined.getAttributes()) {

			// skip ARTIST PK, it is joined from painting
			if (Artist.ARTIST_ID_PK_COLUMN.equals(a.getName())) {
				continue;
			}

			ColumnDescriptor c = new ColumnDescriptor(a, "t1");
			c.setDataRowKey("toArtist." + a.getName());
			assertTrue("No descriptor for " + a + ", columns: " + columns, columns.contains(c));
		}
	}

}

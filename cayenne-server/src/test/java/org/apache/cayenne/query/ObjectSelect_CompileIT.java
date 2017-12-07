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
package org.apache.cayenne.query;

import java.util.Arrays;
import java.util.Collection;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class ObjectSelect_CompileIT extends ServerCase {

	@Inject
	private EntityResolver resolver;

	@Test
	public void testCreateReplacementQuery_Bare() {

		// use only a minimal number of attributes, with null/defaults for
		// everything else
		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		Query replacement = q.createReplacementQuery(resolver);
		assertThat(replacement, instanceOf(SelectQuery.class));

		@SuppressWarnings("unchecked")
		SelectQuery<Artist> selectQuery = (SelectQuery<Artist>) replacement;
		assertNull(selectQuery.getQualifier());
		assertEquals(Artist.class, selectQuery.getRoot());
		assertEquals(0, selectQuery.getOrderings().size());
		assertNull(selectQuery.getPrefetchTree());

		assertEquals(QueryCacheStrategy.NO_CACHE, selectQuery.getCacheStrategy());
		assertNull(selectQuery.getCacheGroup());
		assertEquals(0, selectQuery.getFetchLimit());
		assertEquals(0, selectQuery.getFetchOffset());
		assertEquals(0, selectQuery.getPageSize());
		assertEquals(0, selectQuery.getStatementFetchSize());
	}

	@Test
	public void testCreateReplacementQuery_Full() {

		// add all possible attributes to the query and make sure they got propagated
		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("me"))
				.orderBy(Artist.DATE_OF_BIRTH.asc(), Artist.ARTIST_NAME.desc()).prefetch(Artist.PAINTING_ARRAY.joint())
				.localCache("cg2").limit(46).offset(9).pageSize(6).statementFetchSize(789);

		Query replacement = q.createReplacementQuery(resolver);
		assertThat(replacement, instanceOf(SelectQuery.class));

		@SuppressWarnings("unchecked")
		SelectQuery<Artist> selectQuery = (SelectQuery<Artist>) replacement;
		assertEquals("artistName = \"me\"", selectQuery.getQualifier().toString());

		assertEquals(2, selectQuery.getOrderings().size());
		assertArrayEquals(new Object[] { Artist.DATE_OF_BIRTH.asc(), Artist.ARTIST_NAME.desc() }, selectQuery
				.getOrderings().toArray());

		PrefetchTreeNode prefetch = selectQuery.getPrefetchTree();
		assertNotNull(prefetch);
		assertEquals(1, prefetch.getChildren().size());

		PrefetchTreeNode childPrefetch = prefetch.getNode(Artist.PAINTING_ARRAY.getName());
		assertEquals(Artist.PAINTING_ARRAY.getName(), childPrefetch.getName());
		assertEquals(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS, childPrefetch.getSemantics());

		assertEquals(QueryCacheStrategy.LOCAL_CACHE, selectQuery.getCacheStrategy());
		assertEquals("cg2", selectQuery.getCacheGroup());
		assertEquals(46, selectQuery.getFetchLimit());
		assertEquals(9, selectQuery.getFetchOffset());
		assertEquals(6, selectQuery.getPageSize());
		assertEquals(789, selectQuery.getStatementFetchSize());
	}

	@Test
	public void testCreateReplacementQuery_RootClass() {
		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		@SuppressWarnings("rawtypes")
		SelectQuery qr = (SelectQuery) q.createReplacementQuery(resolver);
		assertEquals(Artist.class, qr.getRoot());
		assertFalse(qr.isFetchingDataRows());
	}

	@Test
	public void testCreateReplacementQuery_RootDataRow() {
		ObjectSelect<DataRow> q = ObjectSelect.dataRowQuery(Artist.class);

		@SuppressWarnings("rawtypes")
		SelectQuery qr = (SelectQuery) q.createReplacementQuery(resolver);
		assertEquals(Artist.class, qr.getRoot());
		assertTrue(qr.isFetchingDataRows());
	}

	@Test
	public void testCreateReplacementQuery_RootDbEntity() {
		ObjectSelect<DataRow> q = ObjectSelect.dbQuery("ARTIST");

		@SuppressWarnings("rawtypes")
		SelectQuery qr = (SelectQuery) q.createReplacementQuery(resolver);
		assertEquals(resolver.getDbEntity("ARTIST"), qr.getRoot());
		assertTrue(qr.isFetchingDataRows());
	}

	@Test
	public void testCreateReplacementQuery_RootObjEntity() {
		ObjectSelect<CayenneDataObject> q = ObjectSelect.query(CayenneDataObject.class, "Artist");

		@SuppressWarnings("rawtypes")
		SelectQuery qr = (SelectQuery) q.createReplacementQuery(resolver);
		assertEquals(resolver.getObjEntity(Artist.class), qr.getRoot());
		assertFalse(qr.isFetchingDataRows());
	}

	@Test(expected = CayenneRuntimeException.class)
	public void testCreateReplacementQuery_RootAbscent() {
		ObjectSelect<DataRow> q = ObjectSelect.dataRowQuery(Artist.class).entityName(null);
		q.createReplacementQuery(resolver);
	}

	@Test
	public void testCreateReplacementQuery_DataRows() {
		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		@SuppressWarnings("rawtypes")
		SelectQuery selectQuery1 = (SelectQuery) q.createReplacementQuery(resolver);
		assertFalse(selectQuery1.isFetchingDataRows());

		q.fetchDataRows();

		@SuppressWarnings("rawtypes")
		SelectQuery selectQuery2 = (SelectQuery) q.createReplacementQuery(resolver);
		assertTrue(selectQuery2.isFetchingDataRows());
	}

	@Test
	public void testCreateReplacementQuery_Columns() {
		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		SelectQuery selectQuery1 = (SelectQuery) q.createReplacementQuery(resolver);
		assertNull(selectQuery1.getColumns());

		ColumnSelect<Object[]> newQ = q.columns(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH);

		SelectQuery selectQuery2 = (SelectQuery) newQ.createReplacementQuery(resolver);
		assertNotNull(selectQuery2.getColumns());

		Collection<Property<?>> properties = Arrays.<Property<?>>asList(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH);
		assertEquals(properties, selectQuery2.getColumns());
	}

}

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

import java.util.Arrays;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.FunctionExpressionFactory;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.ServerCaseDataSourceFactory;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class OrderingTranslatorIT extends ServerCase {

	@Inject
	private DataNode node;

	@Inject
	private ServerCaseDataSourceFactory dataSourceFactory;

	/**
	 * Tests ascending ordering on string attribute.
	 */
	@Test
	public void testAppendPart1() throws Exception {
		Ordering o1 = new Ordering("artistName", SortOrder.ASCENDING);
		doTestAppendPart("ta.ARTIST_NAME", o1);
	}

	/**
	 * Tests descending ordering on string attribute.
	 */
	@Test
	public void testAppendPart2() throws Exception {
		Ordering o1 = new Ordering("artistName", SortOrder.DESCENDING);
		doTestAppendPart("ta.ARTIST_NAME DESC", o1);
	}

	@Test
	public void testAppendPart3() throws Exception {

		Ordering o1 = new Ordering("artistName", SortOrder.DESCENDING);
		Ordering o2 = new Ordering("paintingArray.estimatedPrice", SortOrder.ASCENDING);

		doTestAppendPart("ta.ARTIST_NAME DESC, ta.ESTIMATED_PRICE", o1, o2);
	}

	/**
	 * Tests ascending case-insensitive ordering on string attribute.
	 */
	@Test
	public void testAppendPart4() throws Exception {
		Ordering o1 = new Ordering("artistName", SortOrder.ASCENDING_INSENSITIVE);
		doTestAppendPart("UPPER(ta.ARTIST_NAME)", o1);
	}

	@Test
	public void testAppendPart5() throws Exception {

		Ordering o1 = new Ordering("artistName", SortOrder.DESCENDING_INSENSITIVE);
		Ordering o2 = new Ordering("paintingArray.estimatedPrice", SortOrder.ASCENDING);

		doTestAppendPart("UPPER(ta.ARTIST_NAME) DESC, ta.ESTIMATED_PRICE", o1, o2);
	}

	@Test
	public void testAppendPart6() throws Exception {
		Ordering o1 = new Ordering("artistName", SortOrder.ASCENDING_INSENSITIVE);
		Ordering o2 = new Ordering("paintingArray.estimatedPrice", SortOrder.ASCENDING_INSENSITIVE);

		doTestAppendPart("UPPER(ta.ARTIST_NAME), UPPER(ta.ESTIMATED_PRICE)", o1, o2);
	}

	@Test
	public void testAppendFunctionExpression1() throws Exception {
		Ordering o1 = new Ordering(FunctionExpressionFactory.absExp("paintingArray.estimatedPrice"));

		doTestAppendPart("ABS(ta.ESTIMATED_PRICE)", o1);
	}

	@Test
	public void testAppendFunctionExpression2() throws Exception {
		Ordering o1 = new Ordering(FunctionExpressionFactory.countExp(ExpressionFactory.pathExp("dateOfBirth")), SortOrder.ASCENDING_INSENSITIVE);
		Ordering o2 = new Ordering(FunctionExpressionFactory.sqrtExp("paintingArray.estimatedPrice"), SortOrder.DESCENDING);

		doTestAppendPart("UPPER(COUNT(ta.DATE_OF_BIRTH)), SQRT(ta.ESTIMATED_PRICE) DESC", o1, o2);
	}

	@Test(expected = CayenneRuntimeException.class)
	public void testAppendIllegalExpression() throws Exception {
		Ordering o1 = new Ordering(ExpressionFactory.and(ExpressionFactory.expTrue(), ExpressionFactory.expFalse()));
		// should throw exception
		doTestAppendPart("TRUE AND FALSE", o1);
	}

	private void doTestAppendPart(String expectedSQL, Ordering... orderings) {

		SelectQuery<Artist> q = SelectQuery.query(Artist.class);
		q.addOrderings(Arrays.asList(orderings));

		TstQueryAssembler assembler = new TstQueryAssembler(q, node.getAdapter(), node.getEntityResolver());
		StringBuilder out = new StringBuilder();
		String translated = new OrderingTranslator(assembler).appendPart(out).toString();

		assertEquals(expectedSQL, translated);
	}
}

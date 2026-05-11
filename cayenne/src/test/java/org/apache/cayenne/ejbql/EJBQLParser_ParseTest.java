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
package org.apache.cayenne.ejbql;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EJBQLParser_ParseTest {

	private EJBQLParser parser;

	@BeforeEach
	public void before() {
		parser = EJBQLParserFactory.getParser();
	}

	@Test
	public void dbPath() {
		EJBQLExpression select = parser.parse("select p from Painting p WHERE db:p.toArtist.ARTIST_NAME = 'a'");
		assertNotNull(select);
	}

	@Test
	public void enumPath() {
		EJBQLExpression select = parser
				.parse("select p from Painting p WHERE p.toArtist.ARTIST_NAME = enum:org.apache.cayenne.ejbql.EJBQLEnum1.X");
		assertNotNull(select);
	}

	@Test
	public void implicitOuterJoin() {
		EJBQLExpression select = parser
				.parse("SELECT a FROM Artist a WHERE a.paintingArray+.toGallery.galleryName = 'gallery2'");
		assertNotNull(select);
	}

	/**
	 * This should not parse because there are multiple non-bracketed
	 * parameters.
	 */
	@Test
	public void inWithMultipleStringPositionalParameter_withoutBrackets() {
		assertThrows(EJBQLException.class, () ->
				parser.parse("select p from Painting p WHERE p.toArtist IN ?1, ?2"));
	}
}

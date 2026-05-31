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
package org.apache.cayenne.access.translator.sqltemplate.directive;

import org.apache.cayenne.access.MockOperationObserver;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for Result directive to check if we could use ResultDirective optionally.
 */
public class ResultDirectiveIT {

	@RegisterExtension
	static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

	private CayenneRuntime runtime;

	@BeforeEach
	public void before() throws SQLException {
		runtime = env.runtime();
		env.table("ARTIST").setColumns("ARTIST_ID", "ARTIST_NAME").insert(1L, "ArtistToTestResult");
	}

	@Test
	public void withoutResultDirective() throws Exception {
		String sql = "SELECT ARTIST_ID, ARTIST_NAME FROM ARTIST";
		Map<String, Object> selectResult = selectForQuery(sql);

		assertEquals(1L, selectResult.get("ARTIST_ID"));
		assertEquals("ArtistToTestResult", selectResult.get("ARTIST_NAME"));
	}

	@Test
	public void withOnlyResultDirective() throws Exception {
		String sql = "SELECT #result('ARTIST_ID' 'java.lang.Integer'), #result('ARTIST_NAME' 'java.lang.String')"
				+ " FROM ARTIST";
		Map<String, Object> selectResult = selectForQuery(sql);

		// TODO: is that correct to use Long (coming from DbAttribute) type for
		// ARTIST_ID instead of Integer (coming from #result(..))?
		assertEquals(1L, selectResult.get("ARTIST_ID"));
		assertEquals("ArtistToTestResult", selectResult.get("ARTIST_NAME").toString().trim());
	}

	@Test
	public void withMixedDirectiveUse1() throws Exception {
		String sql = "SELECT ARTIST_ID, #result('ARTIST_NAME' 'java.lang.String') FROM ARTIST";
		Map<String, Object> selectResult = selectForQuery(sql);

		assertEquals(1L, selectResult.get("ARTIST_ID"));
		assertEquals("ArtistToTestResult", selectResult.get("ARTIST_NAME").toString().trim());
	}

	@Test
	public void withMixedDirectiveUse2() throws Exception {
		String sql = "SELECT #result('ARTIST_ID' 'java.lang.Integer'), ARTIST_NAME FROM ARTIST";
		Map<String, Object> selectResult = selectForQuery(sql);

		assertEquals(1L, selectResult.get("ARTIST_ID"));
		assertEquals("ArtistToTestResult", selectResult.get("ARTIST_NAME"));
	}

	private Map<String, Object> selectForQuery(String sql) {
		SQLTemplate template = new SQLTemplate(Artist.class, sql);
		template.setColumnNamesCapitalization(CapsStrategy.UPPER);
		MockOperationObserver observer = new MockOperationObserver();
		runtime.getDataDomain().getDefaultNode().performQueries(Collections.singletonList(template), observer);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> data = observer.rowsForQuery(template);
		assertEquals(1, data.size());
		Map<String, Object> row = data.get(0);
		return row;
	}

}

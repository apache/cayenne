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

package org.apache.cayenne.dbsync.merge.token.db;

import java.sql.Types;
import java.util.List;

import org.apache.cayenne.dbsync.merge.MergeCase;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.junit.Assume;
import org.junit.Test;

public class SetPrimaryKeyToDbIT extends MergeCase {

	@Test
	public void test() throws Exception {
		Assume.assumeTrue(accessStackAdapter.supportsColumnTypeReengineering());
		dropTableIfPresent("NEW_TABLE");
		assertTokensAndExecute(0, 0);

		DbEntity dbEntity1 = new DbEntity("NEW_TABLE");

		DbAttribute e1col1 = new DbAttribute("ID1", Types.INTEGER, dbEntity1);
		e1col1.setMandatory(true);
		e1col1.setPrimaryKey(true);
		dbEntity1.addAttribute(e1col1);
		map.addDbEntity(dbEntity1);

		assertTokensAndExecute(1, 0);
		assertTokensAndExecute(0, 0);

		DbAttribute e1col2 = new DbAttribute("ID2", Types.INTEGER, dbEntity1);
		e1col2.setMandatory(true);
		dbEntity1.addAttribute(e1col2);

		assertTokensAndExecute(2, 0);
		assertTokensAndExecute(0, 0);

		e1col1.setPrimaryKey(false);
		e1col2.setPrimaryKey(true);

		assertTokensAndExecute(1, 0);
		assertTokensAndExecute(0, 0);
	}

	@Test
	public void testCaseSensitiveNaming() throws Exception {
		// Mariadb don't support the same attributes name in different cases.
		Assume.assumeTrue(accessStackAdapter.supportsCaseSensitiveLike());
		map.setQuotingSQLIdentifiers(true);
		List<MergerToken> tokens = syncDBForCaseSensitiveTest();
		dropTableIfPresent("NEW_TABLE");
		assertTokensAndExecute(0, 0, true);

		DbEntity dbEntity1 = new DbEntity("NEW_TABLE");

		DbAttribute e1col1 = new DbAttribute("ID1", Types.INTEGER, dbEntity1);
		e1col1.setMandatory(true);
		e1col1.setPrimaryKey(true);
		dbEntity1.addAttribute(e1col1);

		//to prevent postgresql from creating pk_table
		setPrimaryKeyGeneratorDBGenerate(dbEntity1);
		map.addDbEntity(dbEntity1);

		assertTokensAndExecute(1, 0, true);
		assertTokensAndExecute(0, 0, true);

		DbAttribute e1col2 = new DbAttribute("id1", Types.INTEGER, dbEntity1);
		e1col2.setMandatory(true);
		dbEntity1.addAttribute(e1col2);

		assertTokensAndExecute(2, 0, true);
		assertTokensAndExecute(0, 0, true);

		e1col1.setPrimaryKey(false);
		e1col2.setPrimaryKey(true);

		assertTokensAndExecute(1, 0, true);
		assertTokensAndExecute(0, 0, true);

		//clear entity
		map.removeDbEntity(dbEntity1.getName());
		dropTableIfPresent("NEW_TABLE");

		assertTokensAndExecute(0, 0, true);
		reverseSyncDBForCaseSensitiveTest(tokens);
		map.setQuotingSQLIdentifiers(false);
	}
}

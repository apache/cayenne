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

package org.apache.cayenne.dbsync.merge.token.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.dbsync.merge.MergeCase;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.junit.Assume;
import org.junit.Test;

public class CreateTableToModelIT extends MergeCase {

	@Test
	public void testAddTable() throws Exception {
		dropTableIfPresent("NEW_TABLE");
		assertTokensAndExecute(0, 0);

		DbEntity dbEntity = new DbEntity("NEW_TABLE");

		DbAttribute column1 = new DbAttribute("ID", Types.INTEGER, dbEntity);
		column1.setMandatory(true);
		column1.setPrimaryKey(true);
		dbEntity.addAttribute(column1);

		DbAttribute column2 = new DbAttribute("NAME", Types.VARCHAR, dbEntity);
		column2.setMaxLength(10);
		column2.setMandatory(false);
		dbEntity.addAttribute(column2);

		// for the new entity to the db
		execute(mergerFactory().createCreateTableToDb(dbEntity));

		List<MergerToken> tokens = createMergeTokens();
		assertEquals(1, tokens.size());
		MergerToken token = tokens.get(0);
		if (token.getDirection().isToDb()) {
			token = token.createReverse(mergerFactory());
		}
		assertTrue(token.getClass().getName(), token instanceof CreateTableToModel);

		execute(token);

		ObjEntity objEntity = null;
		for (ObjEntity candidate : map.getObjEntities()) {
			if (dbEntity.getName().equalsIgnoreCase(candidate.getDbEntityName())) {
				objEntity = candidate;
				break;
			}
		}
		assertNotNull(objEntity);

		assertEquals(objEntity.getClassName(), map.getDefaultPackage() + "." + objEntity.getName());
		assertEquals(objEntity.getSuperClassName(), map.getDefaultSuperclass());
		assertEquals(objEntity.getClientClassName(), map.getDefaultClientPackage() + "." + objEntity.getName());
		assertEquals(objEntity.getClientSuperClassName(), map.getDefaultClientSuperclass());

		assertEquals(1, objEntity.getAttributes().size());
		assertEquals("java.lang.String", objEntity.getAttributes().iterator().next().getType());

		// clear up
		// fix psql case issue
		map.removeDbEntity(objEntity.getDbEntity().getName(), true);
		map.removeObjEntity(objEntity.getName(), true);
		map.removeDbEntity(dbEntity.getName(), true);
		resolver.refreshMappingCache();
		assertNull(map.getObjEntity(objEntity.getName()));
		assertNull(map.getDbEntity(dbEntity.getName()));
		assertFalse(map.getDbEntities().contains(dbEntity));

		assertTokensAndExecute(1, 0);
		assertTokensAndExecute(0, 0);
	}

	@Test
	public void testAddTableCaseSensitive() throws Exception {
		Assume.assumeTrue(accessStackAdapter.supportsCaseSensitiveLike());
		map.setQuotingSQLIdentifiers(true);
		List<MergerToken> syncTokens = syncDBForCaseSensitiveTest();
		dropTableIfPresent("NEW_TABLE");
		dropTableIfPresent("NEW_table");
		assertTokensAndExecute(0, 0);

		DbEntity dbEntity = new DbEntity("NEW_TABLE");

		DbAttribute column1 = new DbAttribute("ID", Types.INTEGER, dbEntity);
		column1.setMandatory(true);
		column1.setPrimaryKey(true);
		dbEntity.addAttribute(column1);

		DbAttribute column2 = new DbAttribute("NAME", Types.VARCHAR, dbEntity);
		column2.setMaxLength(10);
		column2.setMandatory(false);
		dbEntity.addAttribute(column2);


		DbEntity dbEntity1 = new DbEntity("NEW_table");

		DbAttribute column3 = new DbAttribute("ID", Types.INTEGER, dbEntity1);
		column3.setMandatory(true);
		column3.setPrimaryKey(true);
		dbEntity1.addAttribute(column3);

		DbAttribute column4 = new DbAttribute("NUMBER_ID", Types.INTEGER, dbEntity1);
		column4.setMaxLength(10);
		column4.setMandatory(false);
		dbEntity1.addAttribute(column4);

		//to prevent postgresql from creating pk_table
		setPrimaryKeyGeneratorDBGenerate(dbEntity);
		map.addDbEntity(dbEntity);

		//to prevent postgresql from creating pk_table
		setPrimaryKeyGeneratorDBGenerate(dbEntity1);
		map.addDbEntity(dbEntity1);
		execute(mergerFactory().createCreateTableToDb(dbEntity));
		execute(mergerFactory().createCreateTableToDb(dbEntity1));
		map.removeDbEntity(dbEntity.getName());
		map.removeDbEntity(dbEntity1.getName());

		List<MergerToken> tokens = createMergeTokensWithoutEmptyFilter(true);
		List<MergerToken> reverseTokens = new ArrayList<>();
		for (MergerToken token : tokens) {
			if (token.getDirection().isToDb()) {
				reverseTokens.add(token.createReverse(mergerFactory()));
			}
		}
		execute(filterNotValid(reverseTokens));

		ObjEntity objEntity1 = null;
		for (ObjEntity candidate : map.getObjEntities()) {
			if (dbEntity1.getName().equals(candidate.getDbEntityName())) {
				objEntity1 = candidate;
				break;
			}
		}
		assertNotNull(objEntity1);

		assertEquals(objEntity1.getClassName(), map.getDefaultPackage() + "." + objEntity1.getName());
		assertEquals(objEntity1.getSuperClassName(), map.getDefaultSuperclass());
		assertEquals(objEntity1.getClientClassName(), map.getDefaultClientPackage() + "." + objEntity1.getName());
		assertEquals(objEntity1.getClientSuperClassName(), map.getDefaultClientSuperclass());

		assertEquals(1, objEntity1.getAttributes().size());
		assertTrue("java.lang.Integer".equals(objEntity1.getAttributes().iterator().next().getType()) ||
				"java.math.BigDecimal".equals(objEntity1.getAttributes().iterator().next().getType()));

		// clear up
		map.removeDbEntity(objEntity1.getDbEntity().getName(), true);
		map.removeObjEntity(objEntity1.getName(), true);
		map.removeDbEntity(dbEntity1.getName(), true);

		ObjEntity objEntity = null;
		for (ObjEntity candidate : map.getObjEntities()) {
			if (dbEntity.getName().equals(candidate.getDbEntityName())) {
				objEntity = candidate;
				break;
			}
		}
		map.removeDbEntity(objEntity.getDbEntity().getName(), true);
		map.removeObjEntity(objEntity.getName(), true);
		map.removeDbEntity(dbEntity.getName(), true);
		resolver.refreshMappingCache();
		assertNull(map.getObjEntity(objEntity.getName()));
		assertNull(map.getDbEntity(dbEntity.getName()));
		assertFalse(map.getDbEntities().contains(dbEntity));
		assertNull(map.getObjEntity(objEntity1.getName()));
		assertNull(map.getDbEntity(dbEntity1.getName()));
		assertFalse(map.getDbEntities().contains(dbEntity1));

		assertTokensAndExecute(2, 0, true);
		assertTokensAndExecute(0, 0, true);
		reverseSyncDBForCaseSensitiveTest(syncTokens);
		map.setQuotingSQLIdentifiers(false);
	}

	@Test
	public void testAddTableCaseSensitiveWithRelationship() throws Exception {
		Assume.assumeTrue(accessStackAdapter.supportsCaseSensitiveLike());
		map.setQuotingSQLIdentifiers(true);
		List<MergerToken> syncTokens = syncDBForCaseSensitiveTest();
		dropTableIfPresent("NEW_TABLE");
		dropTableIfPresent("NEW_table");
		assertTokensAndExecute(0, 0);

		DbEntity dbEntity1 = new DbEntity("NEW_TABLE");
		DbAttribute column1 = new DbAttribute("ID", Types.INTEGER, dbEntity1);
		column1.setMandatory(true);
		column1.setPrimaryKey(true);
		dbEntity1.addAttribute(column1);

		DbAttribute column2 = new DbAttribute("NAME", Types.VARCHAR, dbEntity1);
		column2.setMaxLength(10);
		column2.setMandatory(false);
		dbEntity1.addAttribute(column2);

		setPrimaryKeyGeneratorDBGenerate(dbEntity1);
		map.addDbEntity(dbEntity1);

		DbEntity dbEntity2 = new DbEntity("NEW_table");

		DbAttribute column3 = new DbAttribute("ID", Types.INTEGER, dbEntity2);
		column3.setMandatory(true);
		column3.setPrimaryKey(true);
		dbEntity2.addAttribute(column3);

		DbAttribute column4 = new DbAttribute("NUMBER", Types.INTEGER, dbEntity2);
		column4.setMaxLength(10);
		column4.setMandatory(false);
		dbEntity2.addAttribute(column4);

		setPrimaryKeyGeneratorDBGenerate(dbEntity2);
		map.addDbEntity(dbEntity2);

		// create db relationships
		DbRelationship rel1To2 = new DbRelationship("rel1To2");
		rel1To2.setSourceEntity(dbEntity1);
		rel1To2.setTargetEntityName(dbEntity2);
		rel1To2.setToMany(true);
		rel1To2.addJoin(new DbJoin(rel1To2, column1.getName(), column3.getName()));
		dbEntity1.addRelationship(rel1To2);

		DbRelationship rel2To1 = new DbRelationship("rel2To1");
		rel2To1.setSourceEntity(dbEntity2);
		rel2To1.setTargetEntityName(dbEntity1);
		rel2To1.setToMany(false);
		rel2To1.addJoin(new DbJoin(rel2To1, column3.getName(), column1.getName()));
		dbEntity2.addRelationship(rel2To1);

		// for the new entity to the db
		execute(mergerFactory().createCreateTableToDb(dbEntity1));
		execute(mergerFactory().createCreateTableToDb(dbEntity2));
		map.removeDbEntity(dbEntity1.getName());
		map.removeDbEntity(dbEntity2.getName());

		List<MergerToken> tokens = createMergeTokensWithoutEmptyFilter(true);
		List<MergerToken> reverseTokens = new ArrayList<>();
		for (MergerToken token : tokens) {
			if (token.getDirection().isToDb()) {
				reverseTokens.add(token.createReverse(mergerFactory()));
			}
		}
		execute(filterNotValid(reverseTokens));

		ObjEntity objEntity1 = null;
		ObjEntity objEntity2 = null;
		for (ObjEntity candidate : map.getObjEntities()) {
			if (dbEntity1.getName().equals(candidate.getDbEntityName())) {
				objEntity1 = candidate;
				continue;
			}
			if (dbEntity2.getName().equals(candidate.getDbEntityName())) {
				objEntity2 = candidate;
			}
		}

		assertNotNull(objEntity1);
		assertNotNull(objEntity2);
		assertEquals(0, objEntity1.getRelationships().size());
		assertEquals(0, objEntity2.getRelationships().size());

		//clear entity
		map.removeDbEntity(dbEntity1.getName());
		map.removeDbEntity(dbEntity2.getName());
		map.removeObjEntity(objEntity1.getName(), true);
		map.removeObjEntity(objEntity2.getName(), true);
		dropTableIfPresent("NEW_table");
		dropTableIfPresent("NEW_TABLE");

		assertTokensAndExecute(0, 0, true);
		reverseSyncDBForCaseSensitiveTest(syncTokens);
		map.setQuotingSQLIdentifiers(false);
	}
}

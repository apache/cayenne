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
package org.apache.cayenne.merge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.sql.Types;
import java.util.List;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.junit.Test;

public class DropRelationshipToModelIT extends MergeCase {

	@Test
	public void testForeignKey() throws Exception {
		dropTableIfPresent("NEW_TABLE");
		dropTableIfPresent("NEW_TABLE2");

		assertTokensAndExecute(0, 0);

		DbEntity dbEntity1 = new DbEntity("NEW_TABLE");

		DbAttribute e1col1 = new DbAttribute("ID", Types.INTEGER, dbEntity1);
		e1col1.setMandatory(true);
		e1col1.setPrimaryKey(true);
		dbEntity1.addAttribute(e1col1);

		DbAttribute e1col2 = new DbAttribute("NAME", Types.VARCHAR, dbEntity1);
		e1col2.setMaxLength(10);
		e1col2.setMandatory(false);
		dbEntity1.addAttribute(e1col2);

		map.addDbEntity(dbEntity1);

		DbEntity dbEntity2 = new DbEntity("NEW_TABLE2");
		DbAttribute e2col1 = new DbAttribute("ID", Types.INTEGER, dbEntity2);
		e2col1.setMandatory(true);
		e2col1.setPrimaryKey(true);
		dbEntity2.addAttribute(e2col1);
		DbAttribute e2col2 = new DbAttribute("FK", Types.INTEGER, dbEntity2);
		dbEntity2.addAttribute(e2col2);
		DbAttribute e2col3 = new DbAttribute("NAME", Types.VARCHAR, dbEntity2);
		e2col3.setMaxLength(10);
		dbEntity2.addAttribute(e2col3);

		map.addDbEntity(dbEntity2);

		// create db relationships
		DbRelationship rel1To2 = new DbRelationship("rel1To2");
		rel1To2.setSourceEntity(dbEntity1);
		rel1To2.setTargetEntity(dbEntity2);
		rel1To2.setToMany(true);
		rel1To2.addJoin(new DbJoin(rel1To2, e1col1.getName(), e2col2.getName()));
		dbEntity1.addRelationship(rel1To2);
		DbRelationship rel2To1 = new DbRelationship("rel2To1");
		rel2To1.setSourceEntity(dbEntity2);
		rel2To1.setTargetEntity(dbEntity1);
		rel2To1.setToMany(false);
		rel2To1.addJoin(new DbJoin(rel2To1, e2col2.getName(), e1col1.getName()));
		dbEntity2.addRelationship(rel2To1);
		assertSame(rel1To2, rel2To1.getReverseRelationship());
		assertSame(rel2To1, rel1To2.getReverseRelationship());

		assertTokensAndExecute(4, 0);
		assertTokensAndExecute(0, 0);

		// create ObjEntities
		ObjEntity objEntity1 = new ObjEntity("NewTable");
		objEntity1.setDbEntity(dbEntity1);
		ObjAttribute oatr1 = new ObjAttribute("name");
		oatr1.setDbAttributePath(e1col2.getName());
		oatr1.setType("java.lang.String");
		objEntity1.addAttribute(oatr1);
		map.addObjEntity(objEntity1);
		ObjEntity objEntity2 = new ObjEntity("NewTable2");
		objEntity2.setDbEntity(dbEntity2);
		ObjAttribute o2a1 = new ObjAttribute("name");
		o2a1.setDbAttributePath(e2col3.getName());
		o2a1.setType("java.lang.String");
		objEntity2.addAttribute(o2a1);
		map.addObjEntity(objEntity2);

		// create ObjRelationships
		assertEquals(0, objEntity1.getRelationships().size());
		assertEquals(0, objEntity2.getRelationships().size());
		ObjRelationship objRel1To2 = new ObjRelationship("objRel1To2");
		objRel1To2.addDbRelationship(rel1To2);
		objRel1To2.setSourceEntity(objEntity1);
		objRel1To2.setTargetEntity(objEntity2);
		objEntity1.addRelationship(objRel1To2);
		ObjRelationship objRel2To1 = new ObjRelationship("objRel2To1");
		objRel2To1.addDbRelationship(rel2To1);
		objRel2To1.setSourceEntity(objEntity2);
		objRel2To1.setTargetEntity(objEntity1);
		objEntity2.addRelationship(objRel2To1);
		assertEquals(1, objEntity1.getRelationships().size());
		assertEquals(1, objEntity2.getRelationships().size());
		assertSame(objRel1To2, objRel2To1.getReverseRelationship());
		assertSame(objRel2To1, objRel1To2.getReverseRelationship());

        // remove relationship and fk from model, merge to db and read to model
        dbEntity2.removeRelationship(rel2To1.getName());
        dbEntity1.removeRelationship(rel1To2.getName());
        dbEntity2.removeAttribute(e2col2.getName());
        List<MergerToken> tokens = createMergeTokens();
        assertTokens(tokens, 3, 0);
        for (MergerToken token : tokens) {
            if (token.getDirection().isToDb()) {
                execute(token);
            }
        }
        assertTokensAndExecute(0, 0);
        dbEntity2.addRelationship(rel2To1);
        dbEntity1.addRelationship(rel1To2);
        dbEntity2.addAttribute(e2col2);

		// try do use the merger to remove the relationship in the model
		tokens = createMergeTokens();
		assertTokens(tokens, 2, 0);
		// TODO: reversing the following two tokens should also reverse the
		// order
		MergerToken token0 = tokens.get(0).createReverse(mergerFactory());
		MergerToken token1 = tokens.get(1).createReverse(mergerFactory());
		if (!(token0 instanceof DropRelationshipToModel && token1 instanceof DropColumnToModel || token1 instanceof DropRelationshipToModel
				&& token0 instanceof DropColumnToModel)) {
			fail();
		}
		execute(token0);
		execute(token1);

		// check after merging
		assertNull(dbEntity2.getAttribute(e2col2.getName()));
		assertEquals(0, dbEntity1.getRelationships().size());
		assertEquals(0, dbEntity2.getRelationships().size());
		assertEquals(0, objEntity1.getRelationships().size());
		assertEquals(0, objEntity2.getRelationships().size());

		// clear up
		dbEntity1.removeRelationship(rel1To2.getName());
		dbEntity2.removeRelationship(rel2To1.getName());
		map.removeObjEntity(objEntity1.getName(), true);
		map.removeDbEntity(dbEntity1.getName(), true);
		map.removeObjEntity(objEntity2.getName(), true);
		map.removeDbEntity(dbEntity2.getName(), true);
		resolver.refreshMappingCache();
		assertNull(map.getObjEntity(objEntity1.getName()));
		assertNull(map.getDbEntity(dbEntity1.getName()));
		assertNull(map.getObjEntity(objEntity2.getName()));
		assertNull(map.getDbEntity(dbEntity2.getName()));
		assertFalse(map.getDbEntities().contains(dbEntity1));
		assertFalse(map.getDbEntities().contains(dbEntity2));

		assertTokensAndExecute(2, 0);
		assertTokensAndExecute(0, 0);
	}
}

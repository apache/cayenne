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

package org.apache.cayenne.map;

import org.apache.cayenne.unit.CayenneCase;

/**
 * @author Andrus Adamchik
 * @deprecated since 3.0M2 (scheduled for removal in 3.0M3)
 */
public class DerivedDbEntityTest extends CayenneCase {
    protected DerivedDbEntity ent;
    protected DataMap map;

    public void setUp() throws Exception {
        ent = new DerivedDbEntity("abc");
        map = new DataMap();
        map.addDbEntity(ent);
    }

    public void testParentEntity() throws Exception {
    	assertNull(ent.getParentEntity());
    	map.addDbEntity(ent);
    	DbEntity parent = new DbEntity("xyz");
    	map.addDbEntity(parent);
    	
    	ent.setParentEntity(parent);
    	
    	assertSame(parent, ent.getParentEntity());
    }
    
    public void testGroupByAttributes() throws Exception {
    	DerivedDbAttribute at = new DerivedDbAttribute();
    	at.setName("abc");
        ent.addAttribute(at);
    	assertEquals(0, ent.getGroupByAttributes().size());
    	
    	at.setGroupBy(true);
    	assertEquals(1, ent.getGroupByAttributes().size());
    }
    
    public void testFullyQualifiedName1() throws Exception {
    	assignParent();
    	
    	ent.setName("derived");
    	ent.getParentEntity().setName("parent");
    	assertEquals(ent.getParentEntity().getFullyQualifiedName(), ent.getFullyQualifiedName());
    }
    
    public void testFullyQualifiedName2() throws Exception {
    	assignParent();
    	
    	ent.setName("derived");
    	ent.getParentEntity().setName("parent");
    	ent.getParentEntity().setSchema("parent_schema");
    	assertEquals(ent.getParentEntity().getFullyQualifiedName(), ent.getFullyQualifiedName());
    }
    
    public void testResetToParentView1() throws Exception {
    	assignParent();
    	
    	DerivedDbAttribute at = new DerivedDbAttribute();
    	at.setName("abc");
    	
    	DbRelationship rel = new DbRelationship();
    	rel.setName("xyz");
    	
    	ent.addAttribute(at);
    	ent.addRelationship(rel);
    	
    	ent.resetToParentView();
    	
    	assertEquals(0, ent.getAttributes().size());
    	assertEquals(0, ent.getRelationships().size());
    }
    
    public void testResetToParentView2() throws Exception {
    	assignParent();
    	
    	DbAttribute at = new DbAttribute();
    	at.setName("abc");
    	
    	DbRelationship rel = new DbRelationship();
    	rel.setName("xyz");
    	
    	ent.getParentEntity().addAttribute(at);
    	ent.getParentEntity().addRelationship(rel);
    	
    	ent.resetToParentView();
    	
    	assertEquals(1, ent.getAttributes().size());
    	assertEquals(1, ent.getRelationships().size());
    }
    
    protected void assignParent() {
    	DbEntity parent = new DbEntity("qwerty");
    	map.addDbEntity(parent);
    	ent.setParentEntity(parent);
    }
}


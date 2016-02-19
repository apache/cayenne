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
package org.apache.cayenne.map.naming;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.map.QueryDescriptor;
import org.junit.Assert;
import org.junit.Test;

public class NameCheckersTest {

    @Test
    public void testObjEntityAttributes() throws Exception {
        NameCheckers maker = NameCheckers.objAttribute;
        ObjEntity namingContainer = new ObjEntity();

        String baseName = maker.baseName();
        String name = DefaultUniqueNameGenerator.generate(maker, namingContainer);
        assertEquals(baseName, name);
        namingContainer.addAttribute(new ObjAttribute(name));

        name = DefaultUniqueNameGenerator.generate(maker, namingContainer);
        assertEquals(baseName + "1", name);
        namingContainer.addAttribute(new ObjAttribute(name));

        name = DefaultUniqueNameGenerator.generate(maker, namingContainer);
        assertEquals(baseName + "2", name);
        namingContainer.addAttribute(new ObjAttribute(name));

        name = DefaultUniqueNameGenerator.generate(maker, namingContainer);
        assertEquals(baseName + "3", name);
        namingContainer.addAttribute(new ObjAttribute(name));

        maker = NameCheckers.objRelationship;
        baseName = maker.baseName();
        name = DefaultUniqueNameGenerator.generate(maker, namingContainer);
        assertEquals(baseName, name);
        namingContainer.addRelationship(new ObjRelationship(name));

        name = DefaultUniqueNameGenerator.generate(maker, namingContainer);
        assertEquals(baseName + "1", name);
        namingContainer.addRelationship(new ObjRelationship(name));

        maker = NameCheckers.objCallbackMethod;
        baseName = maker.baseName();
        name = DefaultUniqueNameGenerator.generate(maker, namingContainer);
        assertEquals(baseName, name);
        namingContainer.addRelationship(new ObjRelationship(name));
    }

    @Test
    public void testEntity () {
        DataMap map = new DataMap();

        map.addDbEntity(new DbEntity("name"));
        checkNameAndOther(map, NameCheckers.dbEntity, "name");

        map.addObjEntity(new ObjEntity("name"));
        checkNameAndOther(map, NameCheckers.objEntity, "name");

        map.addProcedure(new Procedure("name"));
        checkNameAndOther(map, NameCheckers.procedure, "name");

        QueryDescriptor query = QueryDescriptor.selectQueryDescriptor();
        query.setName("name");
        map.addQueryDescriptor(query);
        checkNameAndOther(map, NameCheckers.query, "name");
    }

    @Test
    public void testProject() throws Exception {
        assertFalse(NameCheckers.dataChannelDescriptor.isNameInUse(null, null));
    }

    @Test
    public void testDbEntity() throws Exception {
        DbEntity dbEntity = new DbEntity();

        dbEntity.addRelationship(new DbRelationship("name"));
        checkNameAndOther(dbEntity, NameCheckers.dbRelationship, "name");
    }

    @Test
    public void testProcedureAttr() throws Exception {
        Procedure procedure = new Procedure();

        procedure.addCallParameter(new ProcedureParameter("name"));
        checkNameAndOther(procedure, NameCheckers.procedureParameter, "name");
    }

    @Test
    public void testEmbeddableAttr() throws Exception {
        Embeddable embeddable = new Embeddable();

        embeddable.addAttribute(new EmbeddableAttribute("name"));
        checkNameAndOther(embeddable, NameCheckers.embeddableAttribute, "name");
    }

    @Test
    public void testDatanode() throws Exception {
        DataChannelDescriptor descriptor = new DataChannelDescriptor();

        descriptor.getDataMaps().add(new DataMap("name"));
        checkNameAndOther(descriptor, NameCheckers.dataMap, "name");

        descriptor.getNodeDescriptors().add(new DataNodeDescriptor("name"));
        checkNameAndOther(descriptor, NameCheckers.dataNodeDescriptor, "name");
    }

    @Test
    public void testDataMap() throws Exception {
        DataDomain dataDomain = new DataDomain("name");

        dataDomain.addDataMap(new DataMap("name"));
        checkNameAndOther(dataDomain, NameCheckers.dataMap, "name");

        assertFalse(NameCheckers.dataMap.isNameInUse(null, "name"));
        assertFalse(NameCheckers.dataMap.isNameInUse(1, "name"));
    }

    private void checkNameAndOther(Object namingContainer, NameCheckers maker, String newName) {
        assertTrue(maker.isNameInUse(namingContainer, newName));
        assertEquals(newName + "1", DefaultUniqueNameGenerator.generate(maker,namingContainer, newName));
        assertEquals("other" + newName, DefaultUniqueNameGenerator.generate(maker,namingContainer, "other" + newName));
    }

    @Test
    public void testOverlappingAttributeAndCallbackNames() throws Exception {
        ObjEntity namingContainer = new ObjEntity();

        namingContainer.addAttribute(new ObjAttribute("myName"));
        Assert.assertEquals("getMyName1", DefaultUniqueNameGenerator.generate(NameCheckers.objCallbackMethod, namingContainer, "getMyName"));

        namingContainer.getCallbackMap().getPostAdd().addCallbackMethod("getSecondName");
        Assert.assertEquals("SecondName1", DefaultUniqueNameGenerator.generate(NameCheckers.objAttribute, namingContainer, "SecondName"));
        Assert.assertEquals("secondName1", DefaultUniqueNameGenerator.generate(NameCheckers.objAttribute, namingContainer, "secondName"));
        Assert.assertEquals("SecondName1", DefaultUniqueNameGenerator.generate(NameCheckers.objRelationship, namingContainer, "SecondName"));
        Assert.assertEquals("secondName1", DefaultUniqueNameGenerator.generate(NameCheckers.objRelationship, namingContainer, "secondName"));
    }

    @Test
    public void testAttributeDifferentInFirstLetterCases() throws Exception {
        ObjEntity namingContainer = new ObjEntity();

        namingContainer.addAttribute(new ObjAttribute("myName"));
        Assert.assertTrue(NameCheckers.objAttribute.isNameInUse(namingContainer, "myName"));
        Assert.assertFalse(NameCheckers.objAttribute.isNameInUse(namingContainer, "MyName"));

        namingContainer.getCallbackMap().getPostAdd().addCallbackMethod("getSecondName");
        Assert.assertEquals("SecondName1", DefaultUniqueNameGenerator.generate(NameCheckers.objAttribute, namingContainer, "SecondName"));
        Assert.assertEquals("secondName1", DefaultUniqueNameGenerator.generate(NameCheckers.objAttribute, namingContainer, "secondName"));
    }

    @Test
    public void testEmbeddable() {
        DataMap map = new DataMap();

        map.addEmbeddable(new Embeddable("name"));
        Assert.assertTrue(NameCheckers.embeddable.isNameInUse(map, "name"));
        Assert.assertEquals("name1", DefaultUniqueNameGenerator.generate(NameCheckers.embeddable, map, "name"));
        Assert.assertFalse(NameCheckers.embeddable.isNameInUse(map, "other-name"));

        map.setDefaultPackage("package");
        Assert.assertFalse(NameCheckers.embeddable.isNameInUse(map, "name"));
        Assert.assertEquals("package.name", DefaultUniqueNameGenerator.generate(NameCheckers.embeddable, map, "name"));
        map.addEmbeddable(new Embeddable("package.name"));

        Assert.assertTrue(NameCheckers.embeddable.isNameInUse(map, "name"));
        Assert.assertEquals("package.name1", DefaultUniqueNameGenerator.generate(NameCheckers.embeddable, map, "name"));
        Assert.assertFalse(NameCheckers.embeddable.isNameInUse(map, "other-name"));
    }
}
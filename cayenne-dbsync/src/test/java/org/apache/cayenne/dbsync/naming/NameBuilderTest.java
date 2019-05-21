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
package org.apache.cayenne.dbsync.naming;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
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
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NameBuilderTest {

    @Test
    public void testName_Root() {
        assertEquals("project", NameBuilder.builder(new DataChannelDescriptor()).name());
    }

    @Test
    public void testName_DataChannelDescriptorContext() throws Exception {
        DataChannelDescriptor descriptor = new DataChannelDescriptor();

        DataMap m0 = new DataMap();
        m0.setName(NameBuilder.builder(m0).in(descriptor).name());
        assertEquals("datamap", m0.getName());
        descriptor.getDataMaps().add(m0);

        DataMap m1 = new DataMap();
        m1.setName(NameBuilder.builder(m1).in(descriptor).name());
        assertEquals("datamap1", m1.getName());
        descriptor.getDataMaps().add(m1);

        DataNodeDescriptor nd0 = new DataNodeDescriptor();
        nd0.setName(NameBuilder.builder(nd0).in(descriptor).name());
        assertEquals("datanode", nd0.getName());
        descriptor.getNodeDescriptors().add(nd0);

        DataNodeDescriptor nd1 = new DataNodeDescriptor();
        nd1.setName(NameBuilder.builder(nd1).in(descriptor).name());
        assertEquals("datanode1", nd1.getName());
        descriptor.getNodeDescriptors().add(nd1);
    }


    @Test
    public void testName_DataMapContext() {
        DataMap map = new DataMap();
        map.setDefaultPackage("com.foo");

        DbEntity de0 = new DbEntity();
        de0.setName(NameBuilder.builder(de0).in(map).name());
        assertEquals("db_entity", de0.getName());
        map.addDbEntity(de0);

        DbEntity de1 = new DbEntity();
        de1.setName(NameBuilder.builder(de1).in(map).name());
        assertEquals("db_entity1", de1.getName());
        map.addDbEntity(de1);

        ObjEntity oe0 = new ObjEntity();
        oe0.setName(NameBuilder.builder(oe0).in(map).name());
        assertEquals("ObjEntity", oe0.getName());
        map.addObjEntity(oe0);

        ObjEntity oe1 = new ObjEntity();
        oe1.setName(NameBuilder.builder(oe1).in(map).name());
        assertEquals("ObjEntity1", oe1.getName());
        map.addObjEntity(oe1);

        ObjEntity oe2 = new ObjEntity();
        oe2.setName(NameBuilder.builder(oe0).in(map).baseName("db_entity").name());
        assertEquals("Should not conflict with similarly named DbEntity", "Db_entity", oe2.getName());
        map.addObjEntity(oe2);

        Procedure p0 = new Procedure();
        p0.setName(NameBuilder.builder(p0).in(map).name());
        assertEquals("procedure", p0.getName());
        map.addProcedure(p0);

        Procedure p1 = new Procedure();
        p1.setName(NameBuilder.builder(p1).in(map).name());
        assertEquals("procedure1", p1.getName());
        map.addProcedure(p1);

        Procedure p2 = new Procedure();
        p2.setName(NameBuilder.builder(p1).in(map).baseName("db_enity").name());
        assertEquals("Should not conflict with similarly named DbEntity", "db_enity", p2.getName());
        map.addProcedure(p2);

        QueryDescriptor q0 = QueryDescriptor.selectQueryDescriptor();
        q0.setName(NameBuilder.builder(q0).in(map).name());
        assertEquals("query", q0.getName());
        map.addQueryDescriptor(q0);

        QueryDescriptor q1 = QueryDescriptor.ejbqlQueryDescriptor();
        q1.setName(NameBuilder.builder(q1).in(map).name());
        assertEquals("query1", q1.getName());
        map.addQueryDescriptor(q1);

        Embeddable e0 = new Embeddable();
        e0.setClassName("com.foo." + NameBuilder.builder(e0).in(map).name());
        assertEquals("com.foo.Embeddable", e0.getClassName());
        map.addEmbeddable(e0);

        Embeddable e1 = new Embeddable();
        e1.setClassName("com.foo." + NameBuilder.builder(e1).in(map).name());
        assertEquals("com.foo.Embeddable1", e1.getClassName());
        map.addEmbeddable(e1);
    }

    @Test
    public void testName_ObjEntityContext() {

        ObjEntity entity = new ObjEntity();

        entity.getCallbackMap().getPostAdd().addCallbackMethod("getMe");

        ObjAttribute a0 = new ObjAttribute();
        String na0 = NameBuilder.builder(a0).in(entity).name();
        assertEquals("untitledAttr", na0);
        a0.setName(na0);
        entity.addAttribute(a0);

        ObjAttribute a1 = new ObjAttribute();
        String na1 = NameBuilder.builder(a1).in(entity).name();
        assertEquals("untitledAttr1", na1);
        a1.setName(na1);
        entity.addAttribute(a1);

        ObjAttribute a2 = new ObjAttribute();
        String na2 = NameBuilder.builder(a2).in(entity).baseName("me").name();
        assertEquals("Conflict with callback method was not detected", "me1", na2);
        a2.setName(na2);
        entity.addAttribute(a2);

        ObjRelationship r0 = new ObjRelationship();
        String nr0 = NameBuilder.builder(r0).in(entity).name();
        assertEquals("untitledRel", nr0);
        r0.setName(nr0);
        entity.addRelationship(r0);

        ObjRelationship r1 = new ObjRelationship();
        String nr1 = NameBuilder.builder(r1).in(entity).name();
        assertEquals("untitledRel1", nr1);
        r1.setName(nr1);
        entity.addRelationship(r1);
    }

    @Test
    public void testName_DbEntityContext() {
        DbEntity entity = new DbEntity();

        DbAttribute a0 = new DbAttribute();
        String na0 = NameBuilder.builder(a0).in(entity).name();
        assertEquals("untitledAttr", na0);
        a0.setName(na0);
        entity.addAttribute(a0);

        DbAttribute a1 = new DbAttribute();
        String na1 = NameBuilder.builder(a1).in(entity).name();
        assertEquals("untitledAttr1", na1);
        a1.setName(na1);
        entity.addAttribute(a1);

        DbRelationship r0 = new DbRelationship();
        String nr0 = NameBuilder.builder(r0).in(entity).name();
        assertEquals("untitledRel", nr0);
        r0.setName(nr0);
        entity.addRelationship(r0);

        DbRelationship r1 = new DbRelationship();
        String nr1 = NameBuilder.builder(r1).in(entity).name();
        assertEquals("untitledRel1", nr1);
        r1.setName(nr1);
        entity.addRelationship(r1);
    }

    @Test
    public void testName_ProcedureContext() {
        Procedure procedure = new Procedure();

        ProcedureParameter p0 = new ProcedureParameter();
        p0.setName(NameBuilder.builder(p0).in(procedure).name());
        assertEquals("UntitledProcedureParameter", p0.getName());
        procedure.addCallParameter(p0);

        ProcedureParameter p1 = new ProcedureParameter();
        p1.setName(NameBuilder.builder(p1).in(procedure).name());
        assertEquals("UntitledProcedureParameter1", p1.getName());
        procedure.addCallParameter(p1);
    }

    @Test
    public void testName_EmbeddableContext() {
        Embeddable embeddable = new Embeddable();

        EmbeddableAttribute ea0 = new EmbeddableAttribute();
        ea0.setName(NameBuilder.builder(ea0).in(embeddable).name());
        assertEquals("untitledAttr", ea0.getName());
        embeddable.addAttribute(ea0);

        EmbeddableAttribute ea1 = new EmbeddableAttribute();
        ea1.setName(NameBuilder.builder(ea1).in(embeddable).name());
        assertEquals("untitledAttr1", ea1.getName());
        embeddable.addAttribute(ea1);
    }

    @Test
    public void testName_UncapitalizeAttributeNames() throws Exception {

        ObjEntity entity = new ObjEntity();

        ObjAttribute a0 = new ObjAttribute();
        String na0 = NameBuilder.builder(a0).in(entity).baseName("myName").name();
        assertEquals("myName", na0);
        a0.setName(na0);
        entity.addAttribute(a0);

        ObjAttribute a1 = new ObjAttribute();
        String na1 = NameBuilder.builder(a1).in(entity).baseName("MyName").name();
        assertEquals("myName1", na1);
        a1.setName(na1);
        entity.addAttribute(a1);
    }

    @Test
    public void testName_Callbacks_ObjEntityContext() {

        ObjEntity entity = new ObjEntity();

        String c0 = NameBuilder.builderForCallbackMethod(entity).name();
        assertEquals("onEvent", c0);
        entity.getCallbackMap().getPostAdd().addCallbackMethod(c0);

        String c1 = NameBuilder.builderForCallbackMethod(entity).name();
        assertEquals("onEvent1", c1);
        entity.getCallbackMap().getPostAdd().addCallbackMethod(c1);

        entity.addAttribute(new ObjAttribute("untitledAttr"));

        String c3 = NameBuilder.builderForCallbackMethod(entity).baseName("getUntitledAttr").name();
        assertEquals("getUntitledAttr1", c3);
        entity.getCallbackMap().getPostAdd().addCallbackMethod(c3);
    }
}
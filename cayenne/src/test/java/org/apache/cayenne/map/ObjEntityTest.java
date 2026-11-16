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
package org.apache.cayenne.map;

import org.apache.cayenne.util.XMLEncoder;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

public class ObjEntityTest {

    @Test
    public void encodeAsXML() {
        ObjEntity entity = new ObjEntity("X");
        entity.setClassName("org.example.Xc");

        // intentionally randomize attribute order .. must be saved in alphabetical order by name
        entity.addAttribute(new ObjAttribute("a2", "java.lang.String", entity));
        entity.addAttribute(new ObjAttribute("a1", "int", entity));
        entity.addAttribute(new ObjAttribute("a3", "long", entity));

        // relationships are saved outside the entity, so should be ignored in this test
        entity.addRelationship(new ObjRelationship("r1"));

        StringWriter out = new StringWriter();
        XMLEncoder encoder = new XMLEncoder(new PrintWriter(out));
        entity.encodeAsXML(encoder);

        String ls = System.lineSeparator();

        assertEquals("<objEntity name=\"X\" className=\"org.example.Xc\">" + ls +
                "<objAttribute name=\"a1\" type=\"int\"/>" + ls +
                "<objAttribute name=\"a2\" type=\"java.lang.String\"/>" + ls +
                "<objAttribute name=\"a3\" type=\"long\"/>" + ls +
                "</objEntity>" + ls, out.toString());
    }

    @Test
    public void attributeOrder() {
        ObjEntity entity = new ObjEntity("X");
        entity.setClassName("org.example.Xc");

        entity.addAttribute(new ObjAttribute("a2", "java.lang.String", entity));
        entity.addAttribute(new ObjAttribute("a1", "int", entity));

        entity.addAttribute(new EmbeddedAttribute("a3", "long", entity));

        // relationships are saved outside the entity, so should be ignored in this test
        entity.addRelationship(new ObjRelationship("r1"));

        StringWriter out = new StringWriter();
        XMLEncoder encoder = new XMLEncoder(new PrintWriter(out));
        entity.encodeAsXML(encoder);

        String ls = System.lineSeparator();

        assertEquals("<objEntity name=\"X\" className=\"org.example.Xc\">" + ls +
                "<embeddedAttribute name=\"a3\" type=\"long\"/>" + ls +
                "<objAttribute name=\"a1\" type=\"int\"/>" + ls +
                "<objAttribute name=\"a2\" type=\"java.lang.String\"/>" + ls +
                "</objEntity>" + ls, out.toString());
    }

    @Test
    public void encodeAsXMLComments() {
        ObjEntity entity = new ObjEntity("X");
        entity.setClassName("org.example.Xc");
        entity.setComment("<X> & co");

        ObjAttribute a1 = new ObjAttribute("a1", "int", entity);
        a1.setComment("a1 comment");
        entity.addAttribute(a1);
        entity.addAttribute(new ObjAttribute("a2", "int", entity));

        EmbeddedAttribute a3 = new EmbeddedAttribute("a3", "org.example.E", entity);
        a3.setComment("a3 comment");
        entity.addAttribute(a3);

        StringWriter out = new StringWriter();
        XMLEncoder encoder = new XMLEncoder(new PrintWriter(out));
        entity.encodeAsXML(encoder);

        String ls = System.lineSeparator();

        // a comment goes first, and an element with no comment stays empty
        assertEquals("<objEntity name=\"X\" className=\"org.example.Xc\">" + ls +
                "<comment>&lt;X&gt; &amp; co</comment>" + ls +
                "<embeddedAttribute name=\"a3\" type=\"org.example.E\">" + ls +
                "<comment>a3 comment</comment>" + ls +
                "</embeddedAttribute>" + ls +
                "<objAttribute name=\"a1\" type=\"int\">" + ls +
                "<comment>a1 comment</comment>" + ls +
                "</objAttribute>" + ls +
                "<objAttribute name=\"a2\" type=\"int\"/>" + ls +
                "</objEntity>" + ls, out.toString());
    }

    @Test
    public void inheritedAttributeComment() {
        DataMap map = new DataMap("m");

        ObjEntity superEntity = new ObjEntity("Super");
        ObjAttribute attribute = new ObjAttribute("a1", "int", superEntity);
        attribute.setComment("a1 comment");
        superEntity.addAttribute(attribute);
        EmbeddedAttribute embedded = new EmbeddedAttribute("e1", "org.example.E", superEntity);
        embedded.setComment("e1 comment");
        superEntity.addAttribute(embedded);
        map.addObjEntity(superEntity);

        ObjEntity subEntity = new ObjEntity("Sub");
        subEntity.setSuperEntityName("Super");
        map.addObjEntity(subEntity);

        // the inherited attributes are copies, made per call
        ObjAttribute inherited = subEntity.getAttribute("a1");
        assertNotSame(attribute, inherited);
        assertEquals("a1 comment", inherited.getComment());

        ObjAttribute inheritedFromMap = subEntity.getAttributeMap().get("a1");
        assertNotSame(attribute, inheritedFromMap);
        assertEquals("a1 comment", inheritedFromMap.getComment());

        ObjAttribute inheritedEmbedded = subEntity.getAttributeMap().get("e1");
        assertInstanceOf(EmbeddedAttribute.class, inheritedEmbedded);
        assertNotSame(embedded, inheritedEmbedded);
        assertEquals("e1 comment", inheritedEmbedded.getComment());
    }

}

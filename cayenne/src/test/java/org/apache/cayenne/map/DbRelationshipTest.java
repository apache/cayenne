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

import org.apache.cayenne.project.BaseProjectNodeVisitor;
import org.apache.cayenne.util.XMLEncoder;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DbRelationshipTest {

    @Test
    public void encodeAsXMLFlags() {
        DbRelationship relationship = relationship();
        relationship.addJoin(new DbJoin(relationship, "A_ID", "B_ID"));
        relationship.setForeignKey(true);

        String ls = System.lineSeparator();
        assertEquals("<dbRelationship name=\"r1\" source=\"A\" target=\"B\" foreignKey=\"true\">" + ls +
                "<dbAttributePair source=\"A_ID\" target=\"B_ID\"/>" + ls +
                "</dbRelationship>" + ls, encode(relationship));

        relationship.setForeignKey(false);
        relationship.setToMany(true);
        assertEquals("<dbRelationship name=\"r1\" source=\"A\" target=\"B\" toMany=\"true\">" + ls +
                "<dbAttributePair source=\"A_ID\" target=\"B_ID\"/>" + ls +
                "</dbRelationship>" + ls, encode(relationship));
    }

    @Test
    public void encodeAsXMLSkipsEmptyJoins() {
        DbRelationship relationship = relationship();
        relationship.addJoin(new DbJoin(relationship, "A_ID", "B_ID"));

        // empty join, e.g. added in the Modeler join table and never filled in; must not be saved
        relationship.addJoin(new DbJoin(relationship));

        String ls = System.lineSeparator();
        assertEquals("<dbRelationship name=\"r1\" source=\"A\" target=\"B\">" + ls +
                "<dbAttributePair source=\"A_ID\" target=\"B_ID\"/>" + ls +
                "</dbRelationship>" + ls, encode(relationship));
    }

    @Test
    public void encodeAsXMLKeepsHalfFilledJoins() {
        DbRelationship relationship = relationship();

        // a join with only one side set still carries user data and must be saved
        relationship.addJoin(new DbJoin(relationship, "A_ID", null));

        String ls = System.lineSeparator();
        assertEquals("<dbRelationship name=\"r1\" source=\"A\" target=\"B\">" + ls +
                "<dbAttributePair source=\"A_ID\"/>" + ls +
                "</dbRelationship>" + ls, encode(relationship));
    }

    @Test
    public void encodeAsXMLWithOnlyEmptyJoins() {
        DbRelationship relationship = relationship();
        relationship.addJoin(new DbJoin(relationship));

        String ls = System.lineSeparator();
        assertEquals("<dbRelationship name=\"r1\" source=\"A\" target=\"B\"/>" + ls, encode(relationship));
    }

    @Test
    public void isToPK() {
        DbRelationship relationship = relationship();
        DbEntity target = relationship.getTargetEntity();

        DbAttribute id = new DbAttribute("ID");
        id.setPrimaryKey(true);
        DbAttribute seq = new DbAttribute("SEQ");
        seq.setPrimaryKey(true);
        DbAttribute name = new DbAttribute("NAME");
        target.addAttribute(id);
        target.addAttribute(seq);
        target.addAttribute(name);

        // no joins
        assertFalse(relationship.isToPrimaryKey());

        // a subset of a composite PK
        relationship.addJoin(new DbJoin(relationship, "A_ID", "ID"));
        assertFalse(relationship.isToPrimaryKey());

        // the same PK column twice is not the full PK either
        relationship.addJoin(new DbJoin(relationship, "A_ID2", "ID"));
        assertFalse(relationship.isToPrimaryKey());

        // the full PK
        relationship.removeJoin(relationship.getJoins().getLast());
        relationship.addJoin(new DbJoin(relationship, "A_SEQ", "SEQ"));
        assertTrue(relationship.isToPrimaryKey());

        // an extra join to a non-PK column
        relationship.addJoin(new DbJoin(relationship, "A_NAME", "NAME"));
        assertFalse(relationship.isToPrimaryKey());
        relationship.removeJoin(relationship.getJoins().getLast());
        assertTrue(relationship.isToPrimaryKey());

        // a to-many is never to the full PK, whatever its joins
        relationship.setToMany(true);
        assertFalse(relationship.isToPrimaryKey());
    }

    @Test
    public void isIdentifying() {
        DbRelationship relationship = relationship();
        DbEntity source = relationship.getSourceEntity();

        DbAttribute masterId = new DbAttribute("MASTER_ID");
        masterId.setPrimaryKey(true);
        DbAttribute seq = new DbAttribute("SEQ");
        seq.setPrimaryKey(true);
        DbAttribute ownerId = new DbAttribute("OWNER_ID");
        source.addAttribute(masterId);
        source.addAttribute(seq);
        source.addAttribute(ownerId);

        DbAttribute id = new DbAttribute("ID");
        id.setPrimaryKey(true);
        relationship.getTargetEntity().addAttribute(id);

        // no joins
        relationship.setForeignKey(true);
        assertFalse(relationship.isIdentifying());

        // the FK is a part of the source PK
        relationship.addJoin(new DbJoin(relationship, "MASTER_ID", "ID"));
        assertTrue(relationship.isIdentifying());

        // only the FK side can be identifying
        relationship.setForeignKey(false);
        assertFalse(relationship.isIdentifying());
        relationship.setForeignKey(true);

        // a join from a non-PK column
        relationship.addJoin(new DbJoin(relationship, "OWNER_ID", "ID"));
        assertFalse(relationship.isIdentifying());
        relationship.removeJoin(relationship.getJoins().getLast());

        // a join from an unknown column
        relationship.addJoin(new DbJoin(relationship, "NO_SUCH", "ID"));
        assertFalse(relationship.isIdentifying());
    }

    @Test
    public void isReflexive() {
        DbRelationship relationship = relationship();
        DbEntity source = relationship.getSourceEntity();

        DbAttribute id = new DbAttribute("ID");
        id.setPrimaryKey(true);
        source.addAttribute(id);
        source.addAttribute(new DbAttribute("PARENT_ID"));
        source.addAttribute(new DbAttribute("NAME"));

        // a FK to another entity
        relationship.setForeignKey(true);
        relationship.addJoin(new DbJoin(relationship, "PARENT_ID", "ID"));
        assertFalse(relationship.isReflexive());

        // a FK to the own PK
        relationship.setTargetEntityName("A");
        assertTrue(relationship.isReflexive());

        // only the FK side is reflexive
        relationship.setForeignKey(false);
        assertFalse(relationship.isReflexive());
        relationship.setForeignKey(true);

        // a self-reference to a non-PK column
        relationship.removeJoin(relationship.getJoins().getFirst());
        relationship.addJoin(new DbJoin(relationship, "PARENT_ID", "NAME"));
        assertFalse(relationship.isReflexive());

        // no joins
        relationship.removeJoin(relationship.getJoins().getFirst());
        assertFalse(relationship.isReflexive());
    }

    @Test
    public void isToPKNoTargetPK() {
        DbRelationship relationship = relationship();
        relationship.getTargetEntity().addAttribute(new DbAttribute("NAME"));
        relationship.addJoin(new DbJoin(relationship, "A_NAME", "NAME"));
        assertFalse(relationship.isToPrimaryKey());
    }

    private DbRelationship relationship() {
        DataMap map = new DataMap("M");
        DbEntity source = new DbEntity("A");
        DbEntity target = new DbEntity("B");
        map.addDbEntity(source);
        map.addDbEntity(target);

        DbRelationship relationship = new DbRelationship("r1");
        relationship.setSourceEntity(source);
        relationship.setTargetEntityName("B");
        source.addRelationship(relationship);

        return relationship;
    }

    private String encode(DbRelationship relationship) {
        StringWriter out = new StringWriter();
        relationship.encodeAsXML(new XMLEncoder(new PrintWriter(out)), new EncoderDummyVisitor());
        return out.toString();
    }

    private static class EncoderDummyVisitor extends BaseProjectNodeVisitor<Object> {

        @Override
        public Object visitDbRelationship(DbRelationship relationship) {
            return null;
        }
    }
}

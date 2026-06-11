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

import org.apache.cayenne.configuration.BaseConfigurationNodeVisitor;
import org.apache.cayenne.util.XMLEncoder;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DbRelationshipTest {

    @Test
    public void encodeAsXMLSkipsEmptyJoins() {
        DbRelationship relationship = relationship();
        relationship.addJoin(new DbJoin(relationship, "A_ID", "B_ID"));

        // empty join, e.g. added in the Modeler join table and never filled in; must not be saved
        relationship.addJoin(new DbJoin(relationship));

        String ls = System.lineSeparator();
        assertEquals("<db-relationship name=\"r1\" source=\"A\" target=\"B\">" + ls +
                "<db-attribute-pair source=\"A_ID\" target=\"B_ID\"/>" + ls +
                "</db-relationship>" + ls, encode(relationship));
    }

    @Test
    public void encodeAsXMLKeepsHalfFilledJoins() {
        DbRelationship relationship = relationship();

        // a join with only one side set still carries user data and must be saved
        relationship.addJoin(new DbJoin(relationship, "A_ID", null));

        String ls = System.lineSeparator();
        assertEquals("<db-relationship name=\"r1\" source=\"A\" target=\"B\">" + ls +
                "<db-attribute-pair source=\"A_ID\"/>" + ls +
                "</db-relationship>" + ls, encode(relationship));
    }

    @Test
    public void encodeAsXMLWithOnlyEmptyJoins() {
        DbRelationship relationship = relationship();
        relationship.addJoin(new DbJoin(relationship));

        String ls = System.lineSeparator();
        assertEquals("<db-relationship name=\"r1\" source=\"A\" target=\"B\"/>" + ls, encode(relationship));
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

    private static class EncoderDummyVisitor extends BaseConfigurationNodeVisitor<Object> {

        @Override
        public Object visitDbRelationship(DbRelationship relationship) {
            return null;
        }
    }
}

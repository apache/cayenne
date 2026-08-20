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
import java.sql.Types;

import static org.junit.jupiter.api.Assertions.*;

public class DbEntityTest {

    @Test
    public void tableAliasBase() {
        assertEquals("ur", new DbEntity("USER_ROLE").getTableAliasBase());
        assertEquals("a", new DbEntity("ARTIST").getTableAliasBase());
        assertEquals("aeg", new DbEntity("ARTIST_EXHIBIT_GROUP").getTableAliasBase());
        // mixed case, no underscores -> first letter of the single part
        assertEquals("a", new DbEntity("ArtistLazy").getTableAliasBase());
    }

    @Test
    public void tableAliasBaseReservedWord() {
        // "IV_SUB1" -> "is", which is a SQL reserved word, so a numeric suffix is appended
        assertEquals("is1", new DbEntity("IV_SUB1").getTableAliasBase());
        // "ORDER" -> "o" is fine, but a name abbreviating to a reserved word is escaped
        assertEquals("or1", new DbEntity("OWNER_ROLE").getTableAliasBase());
    }

    @Test
    public void tableAliasBaseFallback() {
        assertEquals("t", new DbEntity().getTableAliasBase());
        assertEquals("t", new DbEntity("").getTableAliasBase());
        // does not start with a letter -> fall back to a safe alias
        assertEquals("t", new DbEntity("1_TABLE").getTableAliasBase());
    }

    @Test
    public void tableAliasBaseInvalidatedOnRename() {
        DbEntity entity = new DbEntity("USER_ROLE");
        assertEquals("ur", entity.getTableAliasBase());

        entity.setName("PAINTING");
        assertEquals("p", entity.getTableAliasBase());
    }

    @Test
    public void encodeAsXML() {
        DbEntity entity = new DbEntity("X");
        entity.setSchema("s");
        entity.setCatalog("c");

        // intentionally randomize attribute order .. must be saved in alphabetical order by name
        entity.addAttribute(new DbAttribute("a2", Types.VARCHAR, entity));
        entity.addAttribute(new DbAttribute("a1", Types.INTEGER, entity));
        entity.addAttribute(new DbAttribute("a3", Types.BIGINT, entity));

        // relationships are saved outside the entity, so should be ignored in this test
        entity.addRelationship(new DbRelationship("r1"));

        StringWriter out = new StringWriter();
        XMLEncoder encoder = new XMLEncoder(new PrintWriter(out));
        entity.encodeAsXML(encoder, new EncoderDummyVisitor());

        String ls = System.lineSeparator();

        assertEquals("<db-entity name=\"X\" schema=\"s\" catalog=\"c\">" + ls +
                "<db-attribute name=\"a1\" type=\"INTEGER\"/>" + ls +
                "<db-attribute name=\"a2\" type=\"VARCHAR\"/>" + ls +
                "<db-attribute name=\"a3\" type=\"BIGINT\"/>" + ls +
                "</db-entity>" + ls, out.toString());
    }

    private class EncoderDummyVisitor extends BaseConfigurationNodeVisitor<Object> {

        @Override
        public Object visitDbEntity(DbEntity entity) {
            return null;
        }

        @Override
        public Object visitDbAttribute(DbAttribute attribute) {
            return null;
        }

        @Override
        public Object visitDbRelationship(DbRelationship relationship) {
            return null;
        }
    }
}

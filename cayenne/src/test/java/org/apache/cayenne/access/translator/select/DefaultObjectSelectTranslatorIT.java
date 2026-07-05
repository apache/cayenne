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

package org.apache.cayenne.access.translator.select;

import org.apache.cayenne.access.translator.TranslatedSelect;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultObjectSelectTranslatorIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void simpleSql() {
        ObjectSelect<Artist> select = ObjectSelect.query(Artist.class);
        TranslatedSelect translator = new DbAdapterDelegatedSelectTranslator().translate(select, env.dataNode().getAdapter(), env.context().getEntityResolver());

        String sql = translator.sql();
        assertTrue(sql.startsWith("SELECT "));
        assertTrue(sql.contains("ARTIST_NAME"));
        assertTrue(sql.contains("DATE_OF_BIRTH"));
        assertTrue(sql.contains("ARTIST_ID"));
        assertTrue(sql.indexOf("FROM ARTIST") > sql.indexOf("ARTIST_ID"));

        assertEquals(0, translator.bindings().length);
        assertEquals(3, translator.resultColumns().length);
        assertEquals("ARTIST_NAME", translator.resultColumns()[0].dataRowName());
        assertEquals("DATE_OF_BIRTH", translator.resultColumns()[1].dataRowName());
        assertEquals("ARTIST_ID", translator.resultColumns()[2].dataRowName());
        assertFalse(translator.hasJoins());
        assertFalse(translator.suppressingDistinct());
    }

    @Test
    public void selectWithComplexWhere() {
        ObjectSelect<Artist> select = ObjectSelect.query(Artist.class, Artist.ARTIST_NAME.eq("artist")
                .andExp(Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).eq("painting")));

        TranslatedSelect translator = new DbAdapterDelegatedSelectTranslator().translate(select, env.dataNode().getAdapter(), env.context().getEntityResolver());

        String sql = translator.sql();
        assertTrue(sql.startsWith("SELECT DISTINCT"));
        assertTrue(sql.contains("a.ARTIST_NAME"));
        assertTrue(sql.contains("a.DATE_OF_BIRTH"));
        assertTrue(sql.contains("a.ARTIST_ID"));
        assertTrue(sql.indexOf("FROM ARTIST a") > sql.indexOf("a.ARTIST_ID"));
        assertTrue(sql.indexOf("JOIN PAINTING p") > sql.indexOf("FROM ARTIST a"));
        assertTrue(sql.indexOf("WHERE") > sql.indexOf("JOIN PAINTING p"));

        assertEquals(2, translator.bindings().length);
        assertEquals("ARTIST_NAME", translator.bindings()[0].attribute().getName());
        assertEquals("PAINTING_TITLE", translator.bindings()[1].attribute().getName());

        assertEquals(3, translator.resultColumns().length);
        assertEquals("ARTIST_NAME", translator.resultColumns()[0].dataRowName());
        assertEquals("DATE_OF_BIRTH", translator.resultColumns()[1].dataRowName());
        assertEquals("ARTIST_ID", translator.resultColumns()[2].dataRowName());

        assertTrue(translator.hasJoins());
        assertFalse(translator.suppressingDistinct());
    }

    @Test
    public void selectWithJointPrefetch() {
        ObjectSelect<Painting> select = ObjectSelect.query(Painting.class).prefetch(Painting.TO_ARTIST.joint());

        TranslatedSelect translator = new DbAdapterDelegatedSelectTranslator().translate(select, env.dataNode().getAdapter(), env.context().getEntityResolver());

        String sql = translator.sql();

        assertTrue(sql.startsWith("SELECT "));
        assertTrue(sql.contains("p.ESTIMATED_PRICE"));
        assertTrue(sql.contains("p.PAINTING_DESCRIPTION"));
        assertTrue(sql.contains("p.PAINTING_TITLE"));
        assertTrue(sql.contains("p.ARTIST_ID"));
        assertTrue(sql.contains("p.GALLERY_ID"));
        assertTrue(sql.contains("p.PAINTING_ID"));
        assertTrue(sql.contains("a.ARTIST_ID"));
        assertTrue(sql.contains("a.DATE_OF_BIRTH"));
        assertTrue(sql.contains("a.ARTIST_NAME"));
        assertTrue(sql.indexOf("FROM PAINTING p") > sql.indexOf("a.ARTIST_NAME"));
        assertTrue(sql.indexOf("LEFT JOIN ARTIST a") > sql.indexOf("FROM PAINTING p"));

        assertEquals(0, translator.bindings().length);

        assertEquals(9, translator.resultColumns().length);
        assertEquals("ESTIMATED_PRICE", translator.resultColumns()[0].dataRowName());
        assertEquals("PAINTING_DESCRIPTION", translator.resultColumns()[1].dataRowName());
        assertEquals("PAINTING_TITLE", translator.resultColumns()[2].dataRowName());
        assertEquals("ARTIST_ID", translator.resultColumns()[3].dataRowName());
        assertEquals("GALLERY_ID", translator.resultColumns()[4].dataRowName());
        assertEquals("PAINTING_ID", translator.resultColumns()[5].dataRowName());

        assertEquals("toArtist.ARTIST_ID", translator.resultColumns()[6].dataRowName());
        assertEquals("toArtist.DATE_OF_BIRTH", translator.resultColumns()[7].dataRowName());
        assertEquals("toArtist.ARTIST_NAME", translator.resultColumns()[8].dataRowName());

        assertTrue(translator.hasJoins());
        assertFalse(translator.suppressingDistinct());
    }
}
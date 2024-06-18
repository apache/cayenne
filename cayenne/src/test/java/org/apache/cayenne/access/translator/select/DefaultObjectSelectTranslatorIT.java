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

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.2
 */
@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DefaultObjectSelectTranslatorIT extends RuntimeCase {

    @Inject
    DataContext context;

    @Inject
    DbAdapter adapter;

    @Test
    public void simpleSql() {
        ObjectSelect<Artist> select = ObjectSelect.query(Artist.class);
        DefaultSelectTranslator translator = new DefaultSelectTranslator(select, adapter, context.getEntityResolver());

        String sql = translator.getSql();
        assertTrue(sql.startsWith("SELECT "));
        assertTrue(sql.contains("t0.ARTIST_NAME"));
        assertTrue(sql.contains("t0.DATE_OF_BIRTH"));
        assertTrue(sql.contains("t0.ARTIST_ID"));
        assertTrue(sql.indexOf("FROM ARTIST t0") > sql.indexOf("t0.ARTIST_ID"));

        assertEquals(0, translator.getBindings().length);
        assertEquals(3, translator.getResultColumns().length);
        assertEquals("ARTIST_NAME", translator.getResultColumns()[0].getDataRowKey());
        assertEquals("DATE_OF_BIRTH", translator.getResultColumns()[1].getDataRowKey());
        assertEquals("ARTIST_ID", translator.getResultColumns()[2].getDataRowKey());
        assertFalse(translator.hasJoins());
        assertFalse(translator.isSuppressingDistinct());
    }

    @Test
    public void selectWithComplexWhere() {
        ObjectSelect<Artist> select = ObjectSelect.query(Artist.class, Artist.ARTIST_NAME.eq("artist")
                .andExp(Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).eq("painting")));

        DefaultSelectTranslator translator = new DefaultSelectTranslator(select, adapter, context.getEntityResolver());

        String sql = translator.getSql();
        assertTrue(sql.startsWith("SELECT DISTINCT"));
        assertTrue(sql.contains("t0.ARTIST_NAME"));
        assertTrue(sql.contains("t0.DATE_OF_BIRTH"));
        assertTrue(sql.contains("t0.ARTIST_ID"));
        assertTrue(sql.indexOf("FROM ARTIST t0") > sql.indexOf("t0.ARTIST_ID"));
        assertTrue(sql.indexOf("JOIN PAINTING t1") > sql.indexOf("FROM ARTIST t0"));
        assertTrue(sql.indexOf("WHERE") > sql.indexOf("JOIN PAINTING t1"));

        assertEquals(2, translator.getBindings().length);
        assertEquals("ARTIST_NAME", translator.getBindings()[0].getAttribute().getName());
        assertEquals("PAINTING_TITLE", translator.getBindings()[1].getAttribute().getName());

        assertEquals(3, translator.getResultColumns().length);
        assertEquals("ARTIST_NAME", translator.getResultColumns()[0].getDataRowKey());
        assertEquals("DATE_OF_BIRTH", translator.getResultColumns()[1].getDataRowKey());
        assertEquals("ARTIST_ID", translator.getResultColumns()[2].getDataRowKey());

        assertTrue(translator.hasJoins());
        assertFalse(translator.isSuppressingDistinct());
    }

    @Test
    public void selectWithJointPrefetch() {
        ObjectSelect<Painting> select = ObjectSelect.query(Painting.class).prefetch(Painting.TO_ARTIST.joint());

        DefaultSelectTranslator translator = new DefaultSelectTranslator(select, adapter, context.getEntityResolver());

        String sql = translator.getSql();

        assertTrue(sql.startsWith("SELECT "));
        assertTrue(sql.contains("t0.ESTIMATED_PRICE"));
        assertTrue(sql.contains("t0.PAINTING_DESCRIPTION"));
        assertTrue(sql.contains("t0.PAINTING_TITLE"));
        assertTrue(sql.contains("t0.ARTIST_ID"));
        assertTrue(sql.contains("t0.GALLERY_ID"));
        assertTrue(sql.contains("t0.PAINTING_ID"));
        assertTrue(sql.contains("t1.ARTIST_ID"));
        assertTrue(sql.contains("t1.DATE_OF_BIRTH"));
        assertTrue(sql.contains("t1.ARTIST_NAME"));
        assertTrue(sql.indexOf("FROM PAINTING t0") > sql.indexOf("t1.ARTIST_NAME"));
        assertTrue(sql.indexOf("LEFT JOIN ARTIST t1") > sql.indexOf("FROM PAINTING t0"));

        assertEquals(0, translator.getBindings().length);

        assertEquals(9, translator.getResultColumns().length);
        assertEquals("ESTIMATED_PRICE", translator.getResultColumns()[0].getDataRowKey());
        assertEquals("PAINTING_DESCRIPTION", translator.getResultColumns()[1].getDataRowKey());
        assertEquals("PAINTING_TITLE", translator.getResultColumns()[2].getDataRowKey());
        assertEquals("ARTIST_ID", translator.getResultColumns()[3].getDataRowKey());
        assertEquals("GALLERY_ID", translator.getResultColumns()[4].getDataRowKey());
        assertEquals("PAINTING_ID", translator.getResultColumns()[5].getDataRowKey());

        assertEquals("toArtist.ARTIST_ID", translator.getResultColumns()[6].getDataRowKey());
        assertEquals("toArtist.DATE_OF_BIRTH", translator.getResultColumns()[7].getDataRowKey());
        assertEquals("toArtist.ARTIST_NAME", translator.getResultColumns()[8].getDataRowKey());

        assertTrue(translator.hasJoins());
        assertFalse(translator.isSuppressingDistinct());
    }
}
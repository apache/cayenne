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

package org.apache.cayenne;

import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CayenneIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    protected TableHelper tArtist;
    protected TableHelper tPainting;

    @BeforeEach
    public void setUp() throws Exception {
        tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME");
        tPainting = env.table("PAINTING", "PAINTING_ID", "ARTIST_ID", "PAINTING_TITLE");
    }

    private void createOneArtist() throws Exception {
        tArtist.insert(33002, "artist2");
    }

    private void createTwoArtists() throws Exception {
        tArtist.insert(33001, "artist1");
        tArtist.insert(33002, "artist2");
    }

    @Deprecated
    @Test
    public void readNestedProperty_ToMany() throws Exception {

        tArtist.insert(1, "a");
        tPainting.insert(1, 1, "a1");
        tPainting.insert(2, 1, "a2");

        Artist a = Cayenne.objectForPK(env.context(), Artist.class, 1);
        Collection<String> titles = (Collection<String>) Cayenne.readNestedProperty(
                a,
                "paintingArray.paintingTitle");

        assertEquals(2, titles.size());
        assertTrue(titles.contains("a1"));
        assertTrue(titles.contains("a2"));

        int size = (Integer) Cayenne.readNestedProperty(a, "paintingArray.@size");
        assertEquals(2, size);
    }

    @Test
    public void makePath() {
        assertEquals("", Cayenne.makePath());
        assertEquals("a", Cayenne.makePath("a"));
        assertEquals("a.b", Cayenne.makePath("a", "b"));
    }

    @Test
    @SuppressWarnings("removal")
    public void objectForQuery() throws Exception {
        createOneArtist();

        ObjectId id = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 33002);

        assertNull(env.context().getObjectStore().getObject(id));

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class).where(Artist.SELF.eqId(id));
        Object object = Cayenne.objectForQuery(env.context(), query);

        assertNotNull(object);
        assertInstanceOf(Artist.class, object);
        assertEquals("artist2", ((Artist) object).getArtistName());
    }

    @Test
    public void objectForSelect() throws Exception {
        createOneArtist();

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .where(ExpressionFactory.matchDbExp("ARTIST_NAME", "artist2"));

        Artist object = env.context().selectOne(query);

        assertNotNull(object);
        assertEquals("artist2", object.getArtistName());
    }

    @Test
    public void objectForQueryNoObject() {

        ObjectId id = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 44001);

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class).where(Artist.SELF.eqId(id));
        assertNull(env.context().selectOne(query));
    }

    @Deprecated
    @Test
    public void noObjectForPK() throws Exception {
        createOneArtist();

        // use bogus non-existent PK
        Object object = Cayenne.objectForPK(env.context(), Artist.class, 44001);
        assertNull(object);
    }

    @Deprecated
    @Test
    public void objectForPKTemporary() {

        Persistent o1 = env.context().newObject(Artist.class);
        Persistent o2 = env.context().newObject(Artist.class);
        assertSame(o1, Cayenne.objectForPK(env.context(), o1.getObjectId()));
        assertSame(o2, Cayenne.objectForPK(env.context(), o2.getObjectId()));

        assertNull(Cayenne.objectForPK(env.context(), ObjectId.of("Artist", new byte[]{1, 2, 3})));
    }

    @Deprecated
    @Test
    public void objectForPKObjectId() throws Exception {
        createOneArtist();

        Object object = Cayenne.objectForPK(env.context(), ObjectId.of(
                "Artist",
                Artist.ARTIST_ID_PK_COLUMN,
                33002));

        assertNotNull(object);
        assertInstanceOf(Artist.class, object);
        assertEquals("artist2", ((Artist) object).getArtistName());
    }

    @Deprecated
    @Test
    public void objectForPKClassInt() throws Exception {
        createOneArtist();

        Artist object = Cayenne.objectForPK(env.context(), Artist.class, 33002);

        assertNotNull(object);
        assertInstanceOf(Artist.class, object);
        assertEquals("artist2", object.getArtistName());
    }

    @Deprecated
    @Test
    public void objectForPKEntityInt() throws Exception {
        createOneArtist();

        Object object = Cayenne.objectForPK(env.context(), "Artist", 33002);

        assertNotNull(object);
        assertInstanceOf(Artist.class, object);
        assertEquals("artist2", ((Artist) object).getArtistName());
    }

    @Deprecated
    @Test
    public void objectForPKClassMap() throws Exception {
        createOneArtist();

        Map<String, Integer> pk = Collections.singletonMap(
                Artist.ARTIST_ID_PK_COLUMN,
                33002);
        Artist object = Cayenne.objectForPK(env.context(), Artist.class, pk);

        assertNotNull(object);
        assertTrue(true);
        assertEquals("artist2", object.getArtistName());
    }

    @Test
    public void intPKForObject() throws Exception {
        createOneArtist();

        List<Artist> objects = ObjectSelect.query(Artist.class).select(env.context());
        assertEquals(1, objects.size());
        Persistent object = objects.getFirst();

        assertEquals(33002, Cayenne.intPKForObject(object));
    }

    @Test
    public void pkForObject() throws Exception {
        createOneArtist();

        List<Artist> objects = ObjectSelect.query(Artist.class).select(env.context());
        assertEquals(1, objects.size());
        Persistent object = objects.getFirst();

        assertEquals(33002L, Cayenne.pkForObject(object));
    }

}

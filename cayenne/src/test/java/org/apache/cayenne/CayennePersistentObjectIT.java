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

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.ArtistExhibit;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.util.TestObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CayennePersistentObjectIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void setObjectId() throws Exception {
        GenericPersistentObject object = new GenericPersistentObject();
        ObjectId oid = ObjectId.of("T");

        assertNull(object.getObjectId());

        object.setObjectId(oid);
        assertSame(oid, object.getObjectId());
    }

    @Test
    public void setPersistenceState() throws Exception {
        GenericPersistentObject obj = new GenericPersistentObject();
        assertEquals(PersistenceState.TRANSIENT, obj.getPersistenceState());

        obj.setPersistenceState(PersistenceState.COMMITTED);
        assertEquals(PersistenceState.COMMITTED, obj.getPersistenceState());
    }

    @Test
    public void readNestedProperty1() {
        Artist a = new Artist();
        assertNull(a.readNestedProperty("artistName"));
        a.setArtistName("aaa");
        assertEquals("aaa", a.readNestedProperty("artistName"));
    }

    @Test
    public void readNestedPropertyNotPersistentString() {
        Artist a = new Artist();
        assertNull(a.readNestedProperty("someOtherProperty"));
        a.setSomeOtherProperty("aaa");
        assertEquals("aaa", a.readNestedProperty("someOtherProperty"));
    }

    @Test
    public void readNestedPropertyNonPersistentNotString() {
        Artist a = new Artist();
        Object object = new Object();
        assertNull(a.readNestedProperty("someOtherObjectProperty"));
        a.setSomeOtherObjectProperty(object);
        assertSame(object, a.readNestedProperty("someOtherObjectProperty"));
    }

    @Test
    public void readNestedPropertyNonPersistentObjectPath() {
        GenericPersistentObject o1 = new GenericPersistentObject();
        TestObject o2 = new TestObject();
        o2.setInteger(55);
        o1.writePropertyDirectly("o2", o2);

        assertSame(o2, o1.readNestedProperty("o2"));
        assertEquals(55, o1.readNestedProperty("o2.integer"));
        assertEquals(TestObject.class, o1.readNestedProperty("o2.class"));
        assertEquals(TestObject.class.getName(), o1.readNestedProperty("o2.class.name"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void readNestedPropertyToManyInMiddle() {

        Artist a = env.context().newObject(Artist.class);
        ArtistExhibit ex = env.context().newObject(ArtistExhibit.class);
        Painting p1 = env.context().newObject(Painting.class);
        Painting p2 = env.context().newObject(Painting.class);
        p1.setPaintingTitle("p1");
        p2.setPaintingTitle("p2");
        a.addToPaintingArray(p1);
        a.addToPaintingArray(p2);
        ex.setToArtist(a);

        List<String> names = (List<String>) a.readNestedProperty("paintingArray.paintingTitle");
        assertEquals(2, names.size());
        assertEquals("p1", names.get(0));
        assertEquals("p2", names.get(1));

        List<String> names2 = (List<String>) ex.readNestedProperty("toArtist.paintingArray.paintingTitle");
        assertEquals(names, names2);
    }

    @Test
    public void readNestedPropertyToManyInMiddle1() {

        Artist a = env.context().newObject(Artist.class);
        ArtistExhibit ex = env.context().newObject(ArtistExhibit.class);
        Painting p1 = env.context().newObject(Painting.class);
        Painting p2 = env.context().newObject(Painting.class);
        p1.setPaintingTitle("p1");
        p2.setPaintingTitle("p2");
        a.addToPaintingArray(p1);
        a.addToPaintingArray(p2);
        ex.setToArtist(a);

        List<String> names = (List<String>) a.readNestedProperty("paintingArray+.paintingTitle");
        assertEquals(2, names.size());
        assertEquals("p1", names.get(0));
        assertEquals("p2", names.get(1));

        List<String> names2 = (List<String>) ex.readNestedProperty("toArtist.paintingArray+.paintingTitle");
        assertEquals(names, names2);
    }

    @Test
    public void filterObjects() {

        List<Painting> paintingList = new ArrayList<>();
        Painting p1 = env.context().newObject(Painting.class);
        Artist a1 = env.context().newObject(Artist.class);
        a1.setArtistName("dddAd");
        p1.setToArtist(a1);

        paintingList.add(p1);
        Expression exp = ExpressionFactory.likeExp("toArtist+.artistName", "d%");

        List<Painting> rezult = exp.filterObjects(paintingList);
        assertEquals(a1, rezult.getFirst().getToArtist());
    }

    @Test
    public void filterObjectsResultIsMutable() {

        List<Artist> artistList = new ArrayList<>();
        Artist a = env.context().newObject(Artist.class);
        a.setArtistName("Pablo");

        Expression exp = ExpressionFactory.matchExp("artistName", "Mismatch");

        List<Artist> result = exp.filterObjects(artistList);
        assertTrue(result.isEmpty());
        result.add(a); // list should be mutable
    }
}

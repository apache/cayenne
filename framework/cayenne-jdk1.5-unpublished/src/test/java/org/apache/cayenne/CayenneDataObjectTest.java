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

package org.apache.cayenne;

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.ArtistExhibit;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.apache.cayenne.unit.util.TestBean;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class CayenneDataObjectTest extends ServerCase {

    @Inject
    private ObjectContext context;

    public void testSetObjectId() throws Exception {
        CayenneDataObject object = new CayenneDataObject();
        ObjectId oid = new ObjectId("T");

        assertNull(object.getObjectId());

        object.setObjectId(oid);
        assertSame(oid, object.getObjectId());
    }

    public void testSetPersistenceState() throws Exception {
        CayenneDataObject obj = new CayenneDataObject();
        assertEquals(PersistenceState.TRANSIENT, obj.getPersistenceState());

        obj.setPersistenceState(PersistenceState.COMMITTED);
        assertEquals(PersistenceState.COMMITTED, obj.getPersistenceState());
    }

    public void testReadNestedProperty1() throws Exception {
        Artist a = new Artist();
        assertNull(a.readNestedProperty("artistName"));
        a.setArtistName("aaa");
        assertEquals("aaa", a.readNestedProperty("artistName"));
    }

    public void testReadNestedPropertyNotPersistentString() throws Exception {
        Artist a = new Artist();
        assertNull(a.readNestedProperty("someOtherProperty"));
        a.setSomeOtherProperty("aaa");
        assertEquals("aaa", a.readNestedProperty("someOtherProperty"));
    }

    public void testReadNestedPropertyNonPersistentNotString() throws Exception {
        Artist a = new Artist();
        Object object = new Object();
        assertNull(a.readNestedProperty("someOtherObjectProperty"));
        a.setSomeOtherObjectProperty(object);
        assertSame(object, a.readNestedProperty("someOtherObjectProperty"));
    }

    public void testReadNestedPropertyNonDataObjectPath() {
        CayenneDataObject o1 = new CayenneDataObject();
        TestBean o2 = new TestBean();
        o2.setInteger(new Integer(55));
        o1.writePropertyDirectly("o2", o2);

        assertSame(o2, o1.readNestedProperty("o2"));
        assertEquals(new Integer(55), o1.readNestedProperty("o2.integer"));
        assertEquals(TestBean.class, o1.readNestedProperty("o2.class"));
        assertEquals(TestBean.class.getName(), o1.readNestedProperty("o2.class.name"));
    }

    public void testReadNestedPropertyToManyInMiddle() throws Exception {

        Artist a = context.newObject(Artist.class);
        ArtistExhibit ex = context.newObject(ArtistExhibit.class);
        Painting p1 = context.newObject(Painting.class);
        Painting p2 = context.newObject(Painting.class);
        p1.setPaintingTitle("p1");
        p2.setPaintingTitle("p2");
        a.addToPaintingArray(p1);
        a.addToPaintingArray(p2);
        ex.setToArtist(a);

        List<String> names = (List<String>) a
                .readNestedProperty("paintingArray.paintingTitle");
        assertEquals(names.size(), 2);
        assertEquals(names.get(0), "p1");
        assertEquals(names.get(1), "p2");

        List<String> names2 = (List<String>) ex
                .readNestedProperty("toArtist.paintingArray.paintingTitle");
        assertEquals(names, names2);
    }

    public void testReadNestedPropertyToManyInMiddle1() throws Exception {

        Artist a = context.newObject(Artist.class);
        ArtistExhibit ex = context.newObject(ArtistExhibit.class);
        Painting p1 = context.newObject(Painting.class);
        Painting p2 = context.newObject(Painting.class);
        p1.setPaintingTitle("p1");
        p2.setPaintingTitle("p2");
        a.addToPaintingArray(p1);
        a.addToPaintingArray(p2);
        ex.setToArtist(a);

        List<String> names = (List<String>) a
                .readNestedProperty("paintingArray+.paintingTitle");
        assertEquals(names.size(), 2);
        assertEquals(names.get(0), "p1");
        assertEquals(names.get(1), "p2");

        List<String> names2 = (List<String>) ex
                .readNestedProperty("toArtist.paintingArray+.paintingTitle");
        assertEquals(names, names2);
    }

    public void testFilterObjects() {

        List<Painting> paintingList = new ArrayList<Painting>();
        Painting p1 = context.newObject(Painting.class);
        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("dddAd");
        p1.setToArtist(a1);

        paintingList.add(p1);
        Expression exp = ExpressionFactory.likeExp("toArtist+.artistName", "d%");

        List<Painting> rezult = exp.filterObjects(paintingList);
        assertEquals(a1, rezult.get(0).getToArtist());
    }
}

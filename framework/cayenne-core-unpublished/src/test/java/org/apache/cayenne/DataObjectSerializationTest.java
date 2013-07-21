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

import java.util.List;

import org.apache.cayenne.access.ToManyList;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.apache.cayenne.util.Util;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataObjectSerializationTest extends ServerCase {

    @Inject
    private ObjectContext context;

    public void testSerializeTransient() throws Exception {
        Artist artist = new Artist();
        artist.setArtistName("artist1");
        assertEquals(PersistenceState.TRANSIENT, artist.getPersistenceState());
        assertNull(artist.getObjectId());

        Artist deserialized = Util.cloneViaSerialization(artist);
        assertEquals(PersistenceState.TRANSIENT, deserialized.getPersistenceState());
        assertNull(deserialized.getObjectId());
        assertEquals("artist1", deserialized.getArtistName());
    }

    public void testSerializeNew() throws Exception {
        Artist artist = context.newObject(Artist.class);
        artist.setArtistName("artist1");
        // resolve relationship fault
        artist.getPaintingArray();

        Artist deserialized = Util.cloneViaSerialization(artist);

        // everything must be deserialized, but DataContext link should stay null
        assertEquals(PersistenceState.NEW, deserialized.getPersistenceState());
        assertTrue(deserialized.getObjectId().isTemporary());
        assertEquals("artist1", deserialized.getArtistName());

        assertNull("CDO serialized by itself shouldn't have a DataContext: "
                + deserialized.getObjectContext(), deserialized.getObjectContext());

        // test that to-many relationships are initialized
        List<?> paintings = deserialized.getPaintingArray();
        assertNotNull(paintings);
        assertEquals(0, paintings.size());
    }

    public void testSerializeNewWithFaults() throws Exception {
        Artist artist = context.newObject(Artist.class);
        artist.setArtistName("artist1");

        Artist deserialized = Util.cloneViaSerialization(artist);

        // everything must be deserialized, but DataContext link should stay null
        assertEquals(PersistenceState.NEW, deserialized.getPersistenceState());
        assertTrue(deserialized.getObjectId().isTemporary());
        assertEquals("artist1", deserialized.getArtistName());

        assertNull("CDO serialized by itself shouldn't have a DataContext: "
                + deserialized.getObjectContext(), deserialized.getObjectContext());

        // test that to-many relationships are initialized
        assertTrue(deserialized.readPropertyDirectly("paintingArray") instanceof ToManyList);
        ToManyList list = (ToManyList) artist.readPropertyDirectly("paintingArray");
        assertFalse(list.isFault());
    }

    public void testSerializeCommitted() throws Exception {

        Artist artist = context.newObject(Artist.class);
        artist.setArtistName("artist1");
        context.commitChanges();

        assertEquals(PersistenceState.COMMITTED, artist.getPersistenceState());

        ObjectId id = artist.getObjectId();
        assertNotNull(id);

        Artist deserialized = Util.cloneViaSerialization(artist);

        // everything must be deserialized, but DataContext link should stay null,
        // and properties shouldn't be populated
        // deserizalized committed object is HOLLOW
        assertEquals("Unexpected persistence state: "
                + PersistenceState.persistenceStateName(deserialized
                        .getPersistenceState()), PersistenceState.HOLLOW, deserialized
                .getPersistenceState());

        assertEquals(id, deserialized.getObjectId());

        // properties of committed objects are not set...when DataContext is
        // attached to an object, object can populate itself from snapshot
        assertNull(deserialized.getObjectContext());
        assertEquals(null, deserialized.getArtistName());
    }
}

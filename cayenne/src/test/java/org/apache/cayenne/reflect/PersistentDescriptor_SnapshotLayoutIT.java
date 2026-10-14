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

package org.apache.cayenne.reflect;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.DataRowLayout;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PersistentDescriptor_SnapshotLayoutIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private static Set<String> names(DataRowLayout layout) {
        Set<String> names = new HashSet<>();
        for (int i = 0; i < layout.size(); i++) {
            names.add(layout.nameAt(i));
        }
        return names;
    }

    private DataRowLayout layout(String entityName) {
        return env.context().getEntityResolver().getClassDescriptor(entityName).getSnapshotLayout();
    }

    @Test
    public void attributesForeignKeysAndPk() {
        // the FKs of "toArtist" and "toGallery" are columns, the to-dependent-PK "toPaintingInfo" contributes nothing
        DataRowLayout painting = layout("Painting");
        assertEquals(Set.of("PAINTING_ID", "PAINTING_TITLE", "PAINTING_DESCRIPTION", "ESTIMATED_PRICE", "ARTIST_ID",
                "GALLERY_ID"), names(painting));
        assertEquals(6, painting.size(), "each column is listed once");

        DataRowLayout artist = layout("Artist");
        assertEquals(Set.of("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH"), names(artist));
    }

    @Test
    public void flattenedAttributesAndAdditionalPks() {
        Set<String> names = names(layout("CompoundPainting"));
        assertTrue(names.containsAll(List.of("PAINTING_ID", "PAINTING_TITLE", "ESTIMATED_PRICE",
                "toArtist.ARTIST_NAME", "toGallery.GALLERY_NAME", "toPaintingInfo.TEXT_REVIEW",
                "toArtist.ARTIST_ID", "toGallery.GALLERY_ID", "toPaintingInfo.PAINTING_ID")), names.toString());
    }

    @Test
    public void sameInstance() {
        assertSame(layout("Artist"), layout("Artist"));
    }

    @Test
    public void snapshotsAreBuiltOverTheLayout() {
        ObjectContext context = env.context();
        Artist artist = context.newObject(Artist.class);
        artist.setArtistName("a");
        Painting painting = context.newObject(Painting.class);
        painting.setPaintingTitle("p");
        painting.setToArtist(artist);
        context.commitChanges();

        // the keys of a snapshot come in the layout order
        DataRowLayout layout = layout("Painting");
        List<String> expected = new ArrayList<>();
        for (int i = 0; i < layout.size(); i++) {
            expected.add(layout.nameAt(i));
        }

        DataRow snapshot = ((DataContext) context).currentSnapshot(painting);
        assertEquals(expected, new ArrayList<>(snapshot.keySet()));
        assertEquals(artist.getObjectId().getIdSnapshot().get("ARTIST_ID"), snapshot.get("ARTIST_ID"));
        assertEquals("p", snapshot.get("PAINTING_TITLE"));

        // a partial snapshot, of an object pointing at an uncommitted one, has only the columns it could fill: a null
        // to-one is a null FK, an uncommitted one and the object's own temporary id are missing columns
        Painting uncommitted = context.newObject(Painting.class);
        uncommitted.setPaintingTitle("u");
        uncommitted.setToArtist(context.newObject(Artist.class));
        DataRow partial = ((DataContext) context).currentSnapshot(uncommitted);
        assertEquals(Set.of("PAINTING_TITLE", "PAINTING_DESCRIPTION", "ESTIMATED_PRICE", "GALLERY_ID"),
                partial.keySet());
        assertNull(partial.get("GALLERY_ID"));
    }
}

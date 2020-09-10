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
package org.apache.cayenne.cay_2641;

import org.apache.cayenne.CayenneContext;
import org.apache.cayenne.Fault;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.cay_2641.client.ArtistLazy;
import org.apache.cayenne.testdo.cay_2641.client.PaintingLazy;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @since 4.2
 */
@UseServerRuntime(CayenneProjects.CAY_2641)
public class Cay2641IT extends ClientCase {

    @Inject
    private CayenneContext context;

    @Before
    public void setup() {
        ArtistLazy artistLazy = context.newObject(ArtistLazy.class);
        artistLazy.setName("Test");
        artistLazy.setSurname("Test1");

        PaintingLazy paintingLazy = context.newObject(PaintingLazy.class);
        paintingLazy.setName("Test");
        paintingLazy.setArtist(artistLazy);

        context.commitChanges();
    }

    @Test
    public void testSampleSelect() {
        List<ArtistLazy> artists = ObjectSelect.query(ArtistLazy.class).select(context);

        assertEquals(artists.size(), 1);
        assertEquals(artists.get(0).getSurname(), "Test1");

        assertTrue(artists.get(0).readPropertyDirectly("name") instanceof Fault);

        assertEquals(artists.get(0).getName(), "Test");
    }

    @Test
    public void testColumnSelect() {
        List<String> strings = ObjectSelect.columnQuery(ArtistLazy.class, ArtistLazy.NAME).select(context);

        assertEquals(strings.size(), 1);
        assertEquals(strings.get(0), "Test");
    }

    @Test
    public void testPrefetchSelect() {
        List<PaintingLazy> paintingLazyList1 = ObjectSelect.query(PaintingLazy.class).prefetch(PaintingLazy.ARTIST.joint()).select(context);

        assertEquals(paintingLazyList1.size(), 1);
        assertTrue(paintingLazyList1.get(0).getArtist().readPropertyDirectly("name") instanceof Fault);

        List<PaintingLazy> paintingLazyList2 = ObjectSelect.query(PaintingLazy.class).prefetch(PaintingLazy.ARTIST.disjoint()).select(context);

        assertEquals(paintingLazyList2.size(), 1);
        assertTrue(paintingLazyList1.get(0).getArtist().readPropertyDirectly("name") instanceof Fault);

        List<PaintingLazy> paintingLazyList3 = ObjectSelect.query(PaintingLazy.class).prefetch(PaintingLazy.ARTIST.disjointById()).select(context);

        assertEquals(paintingLazyList3.size(), 1);
        assertTrue(paintingLazyList1.get(0).getArtist().readPropertyDirectly("name") instanceof Fault);
    }

}

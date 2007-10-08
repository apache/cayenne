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

package org.apache.cayenne.access.util;

import java.util.Iterator;
import java.util.List;

import org.apache.art.Artist;
import org.apache.art.Painting;
import org.apache.cayenne.Fault;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataContextTestBase;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneTestCase;

/**
 * @deprecated as PrefetchHelper is deperected too.
 * @author Andrei Adamchik
 */
public class PrefetchHelperTst extends CayenneTestCase {

    protected void setUp() throws Exception {
        super.setUp();

        deleteTestData();
    }

    public void testResolveToOneRelations() throws Exception {
        getAccessStack().createTestData(DataContextTestBase.class, "testArtists", null);
        getAccessStack().createTestData(DataContextTestBase.class, "testPaintings", null);

        DataContext context = createDataContext();
        List paintings = context.performQuery(new SelectQuery(Painting.class));

        // assert that artists are still faults...
        Iterator it = paintings.iterator();
        assertTrue(it.hasNext());
        while (it.hasNext()) {
            Painting p = (Painting) it.next();
            assertTrue(p.readPropertyDirectly("toArtist") instanceof Fault);
        }

        PrefetchHelper.resolveToOneRelations(context, paintings, "toArtist");

        // assert that artists are fully resolved
        it = paintings.iterator();
        while (it.hasNext()) {
            Painting p = (Painting) it.next();
            Object o = p.readPropertyDirectly("toArtist");
            assertTrue(o instanceof Artist);
            Artist artist = (Artist) o;
            assertEquals(PersistenceState.COMMITTED, artist.getPersistenceState());
        }
    }

    public void testResolveToOneRelationsResolved() throws Exception {
        // mainly testing CAY-188 - a case when related objects are already resolved
        // ..

        getAccessStack().createTestData(DataContextTestBase.class, "testArtists", null);
        getAccessStack().createTestData(DataContextTestBase.class, "testPaintings", null);

        DataContext context = createDataContext();
        SelectQuery q = new SelectQuery(Painting.class);
        q.addPrefetch(Painting.TO_ARTIST_PROPERTY);

        List paintings = context.performQuery(q);


        // this shouldn't fail, even though artists are prefetched...
        PrefetchHelper.resolveToOneRelations(
                context,
                paintings,
                Painting.TO_ARTIST_PROPERTY);
    }
}

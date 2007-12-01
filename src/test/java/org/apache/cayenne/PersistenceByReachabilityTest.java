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

import org.apache.art.Artist;
import org.apache.art.Painting;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.unit.CayenneCase;

public class PersistenceByReachabilityTest extends CayenneCase {

    public void testToOneTargetTransient() throws Exception {
        DataContext context = createDataContext();
        Painting persistentDO = context.newObject(Painting.class);

        Artist transientDO = new Artist();
        persistentDO.setToOneTarget(Painting.TO_ARTIST_PROPERTY, transientDO, false);

        assertEquals(PersistenceState.NEW, transientDO.getPersistenceState());
    }

    public void testToOneTargetPersistent() throws Exception {
        DataContext context = createDataContext();
        Painting transientDO = context.newObject(Painting.class);

        Artist persistentDO = new Artist();
        transientDO.setToOneTarget(Painting.TO_ARTIST_PROPERTY, persistentDO, false);

        assertEquals(PersistenceState.NEW, transientDO.getPersistenceState());
    }

    public void testToOneTargetDifferentContext() throws Exception {
        DataContext context1 = createDataContext();
        Painting doC1 = context1.newObject(Painting.class);

        DataContext context2 = createDataContext();
        Artist doC2 = context2.newObject(Artist.class);

        // this is the case where exception must be thrown as DataContexts are
        // different
        try {
            doC1.setToOneTarget(Painting.TO_ARTIST_PROPERTY, doC2, false);
            fail("failed to detect relationship between objects in different DataContexts");
        }
        catch (CayenneRuntimeException ex) {

        }
    }

    public void testToManyTargetDifferentContext() throws Exception {
        DataContext context1 = createDataContext();
        Painting doC1 = context1.newObject(Painting.class);

        DataContext context2 = createDataContext();
        Artist doC2 = context2.newObject(Artist.class);

        // this is the case where exception must be thrown as DataContexts are
        // different
        try {
            doC2.addToManyTarget(Artist.PAINTING_ARRAY_PROPERTY, doC1, false);
            fail("failed to detect relationship between objects in different DataContexts");
        }
        catch (CayenneRuntimeException ex) {

        }
    }

    public void testToManyTargetTransient() throws Exception {
        DataContext context = createDataContext();
        Painting transientDO = context.newObject(Painting.class);

        Artist persistentDO = new Artist();
        persistentDO.addToManyTarget(Artist.PAINTING_ARRAY_PROPERTY, transientDO, false);

        assertEquals(PersistenceState.NEW, transientDO.getPersistenceState());
    }

    public void testToManyTargetPersistent() throws Exception {
        DataContext context = createDataContext();
        Painting persistentDO = context.newObject(Painting.class);

        Artist transientDO = new Artist();
        transientDO.addToManyTarget(Artist.PAINTING_ARRAY_PROPERTY, persistentDO, false);

        assertEquals(PersistenceState.NEW, transientDO.getPersistenceState());
    }
}

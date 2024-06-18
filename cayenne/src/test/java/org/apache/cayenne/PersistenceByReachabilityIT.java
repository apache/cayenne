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

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class PersistenceByReachabilityIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Inject
    private ObjectContext context1;

    @Test
    public void testToOneTargetTransient() throws Exception {
        Painting persistentDO = context.newObject(Painting.class);

        Artist transientDO = new Artist();
        persistentDO.setToOneTarget(Painting.TO_ARTIST.getName(), transientDO, false);

        assertEquals(PersistenceState.NEW, transientDO.getPersistenceState());
    }

    @Test
    public void testToOneTargetPersistent() throws Exception {
        Painting transientDO = context.newObject(Painting.class);

        Artist persistentDO = new Artist();
        transientDO.setToOneTarget(Painting.TO_ARTIST.getName(), persistentDO, false);

        assertEquals(PersistenceState.NEW, transientDO.getPersistenceState());
    }

    @Test
    public void testToOneTargetDifferentContext() throws Exception {

        Painting doC1 = context.newObject(Painting.class);
        Artist doC2 = context1.newObject(Artist.class);

        // this is the case where exception must be thrown as DataContexts are
        // different
        try {
            doC1.setToOneTarget(Painting.TO_ARTIST.getName(), doC2, false);
            fail("failed to detect relationship between objects in different DataContexts");
        }
        catch (CayenneRuntimeException ex) {
            // expected
        }
    }

    @Test
    public void testToManyTargetDifferentContext() throws Exception {
        Painting doC1 = context.newObject(Painting.class);
        Artist doC2 = context1.newObject(Artist.class);

        // this is the case where exception must be thrown as DataContexts are
        // different
        try {
            doC2.addToManyTarget(Artist.PAINTING_ARRAY.getName(), doC1, false);
            fail("failed to detect relationship between objects in different DataContexts");
        }
        catch (CayenneRuntimeException ex) {

        }
    }

    @Test
    public void testToManyTargetTransient() throws Exception {
        Painting transientDO = context.newObject(Painting.class);

        Artist persistentDO = new Artist();
        persistentDO.addToManyTarget(Artist.PAINTING_ARRAY.getName(), transientDO, false);

        assertEquals(PersistenceState.NEW, transientDO.getPersistenceState());
    }

    @Test
    public void testToManyTargetPersistent() throws Exception {
        Painting persistentDO = context.newObject(Painting.class);

        Artist transientDO = new Artist();
        transientDO.addToManyTarget(Artist.PAINTING_ARRAY.getName(), persistentDO, false);

        assertEquals(PersistenceState.NEW, transientDO.getPersistenceState());
    }
}

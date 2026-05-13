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

import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsExt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PersistenceByReachabilityIT {

    @RegisterExtension
    static final CayenneTestsExt env = CayenneTestsExt.forProject(CayenneProjects.TESTMAP_PROJECT);

    private ObjectContext context;
    private ObjectContext context1;

    @BeforeEach
    public void setUp() {
        context = env.context();
        context1 = env.runtime().newContext();
    }

    @Test
    public void toOneTargetTransient() throws Exception {
        Painting persistentDO = context.newObject(Painting.class);

        Artist transientDO = new Artist();
        persistentDO.setToOneTarget(Painting.TO_ARTIST.getName(), transientDO, false);

        assertEquals(PersistenceState.NEW, transientDO.getPersistenceState());
    }

    @Test
    public void toOneTargetPersistent() throws Exception {
        Painting transientDO = context.newObject(Painting.class);

        Artist persistentDO = new Artist();
        transientDO.setToOneTarget(Painting.TO_ARTIST.getName(), persistentDO, false);

        assertEquals(PersistenceState.NEW, transientDO.getPersistenceState());
    }

    @Test
    public void toOneTargetDifferentContext() throws Exception {

        Painting doC1 = context.newObject(Painting.class);
        Artist doC2 = context1.newObject(Artist.class);

        // this is the case where exception must be thrown as DataContexts are different
        assertThrows(CayenneRuntimeException.class,
                () -> doC1.setToOneTarget(Painting.TO_ARTIST.getName(), doC2, false));
    }

    @Test
    public void toManyTargetDifferentContext() throws Exception {
        Painting doC1 = context.newObject(Painting.class);
        Artist doC2 = context1.newObject(Artist.class);

        // this is the case where exception must be thrown as DataContexts are different
        assertThrows(CayenneRuntimeException.class,
                () -> doC2.addToManyTarget(Artist.PAINTING_ARRAY.getName(), doC1, false));
    }

    @Test
    public void toManyTargetTransient() throws Exception {
        Painting transientDO = context.newObject(Painting.class);

        Artist persistentDO = new Artist();
        persistentDO.addToManyTarget(Artist.PAINTING_ARRAY.getName(), transientDO, false);

        assertEquals(PersistenceState.NEW, transientDO.getPersistenceState());
    }

    @Test
    public void toManyTargetPersistent() throws Exception {
        Painting persistentDO = context.newObject(Painting.class);

        Artist transientDO = new Artist();
        transientDO.addToManyTarget(Artist.PAINTING_ARRAY.getName(), persistentDO, false);

        assertEquals(PersistenceState.NEW, transientDO.getPersistenceState());
    }
}

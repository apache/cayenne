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

package org.apache.cayenne.util;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DeepMergeOperationIT extends RuntimeCase {

    @Inject
    private DataChannelInterceptor queryInterceptor;

    @Inject
    private DataContext context;

    @Inject
    private DataContext context1;

    @Test
    public void testDeepMergeNonExistent() {

        final Artist a = context.newObject(Artist.class);
        a.setArtistName("AAA");
        context.commitChanges();

        final DeepMergeOperation op = new DeepMergeOperation(context1);

        queryInterceptor.runWithQueriesBlocked(() -> {
            Artist a2 = op.merge(a);
            assertNotNull(a2);
            assertEquals(PersistenceState.COMMITTED, a2.getPersistenceState());
            assertEquals(a.getArtistName(), a2.getArtistName());
        });
    }

    @Test
    public void testDeepMergeModified() {

        final Artist a = context.newObject(Artist.class);
        a.setArtistName("AAA");
        context.commitChanges();

        final Artist a1 = (Artist) Cayenne.objectForPK(context1, a.getObjectId());
        a1.setArtistName("BBB");
        final DeepMergeOperation op = new DeepMergeOperation(context1);

        queryInterceptor.runWithQueriesBlocked(() -> {
            Artist a2 = op.merge(a);
            assertNotNull(a2);
            assertEquals(PersistenceState.MODIFIED, a2.getPersistenceState());
            assertSame(a1, a2);
            assertEquals("BBB", a2.getArtistName());
        });
    }

}

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
package org.apache.cayenne.access;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.FaultFailureException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextLocalObjectIT extends RuntimeCase {

    @Inject
    private DataContext context1;

    @Inject
    private DataContext context2;

    @Inject
    private DBHelper dbHelper;

    @Inject
    private DataChannelInterceptor interceptor;

    @Inject
    private CayenneRuntime runtime;

    private TableHelper tArtist;

    @Before
    public void setUp() throws Exception {
        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");
    }

    @Test
    public void testLocalObject_InCache() throws Exception {
        tArtist.insert(456, "Bla");

        final Artist a1 = Cayenne.objectForPK(context1, Artist.class, 456);
        final Artist a2 = Cayenne.objectForPK(context2, Artist.class, 456);

        interceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                Artist a3 = context2.localObject(a1);
                assertSame(a3, a2);
                assertSame(context2, a3.getObjectContext());
            }
        });
    }

    @Test
    public void testLocalObject_SameContext() throws Exception {
        tArtist.insert(456, "Bla");

        final Artist a1 = Cayenne.objectForPK(context1, Artist.class, 456);

        interceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                Artist a2 = context1.localObject(a1);
                assertSame(a2, a1);
            }
        });
    }

    @Test
    public void testLocalObject_NotInCache() throws Exception {
        tArtist.insert(456, "Bla");

        final Artist a1 = Cayenne.objectForPK(context1, Artist.class, 456);

        interceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                Artist a3 = context2.localObject(a1);
                assertNotSame(a3, a1);
                assertEquals(a3.getObjectId(), a1.getObjectId());
                assertSame(context2, a3.getObjectContext());
            }
        });
    }

    @Test
    public void testLocalObject_FFE_InvalidID() throws Exception {
        tArtist.insert(777, "AA");

        final Artist a1 = Cayenne.objectForPK(context1, Artist.class, 777);

        Artist a3 = context2.localObject(a1);
        assertEquals(PersistenceState.HOLLOW, a3.getPersistenceState());

        context1.invalidateObjects(a1);
        tArtist.deleteAll();

        assertEquals(PersistenceState.HOLLOW, a3.getPersistenceState());

        try {
            a3.getArtistName();

            fail("FaultFailureException wasn't thrown on attempt to "
                    + "resolve HOLLOW object with no backing DB row");
        }
        catch (FaultFailureException e) {
            // expected
        }

    }

    @Test
    public void testLocalObject_TempId() throws Exception {

        final Artist a1 = context1.newObject(Artist.class);

        interceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {

                Artist a = context2.localObject(a1);
                assertNotNull(a);
                assertEquals(a1.getObjectId(), a.getObjectId());

                // FFE mist be thrown on attempt to read non-existing temp ID
                try {

                    a.getArtistName();
                    fail("FaultFailureException wasn't thrown on attempt to "
                            + "resolve HOLLOW object with temp id");
                }
                catch (FaultFailureException e) {
                    // expected
                }
            }
        });
    }

    @Test
    public void testLocalObject_TempId_NestedContext() throws Exception {

        final Artist a1 = context1.newObject(Artist.class);

        final ObjectContext nestedContext = runtime.newContext(context1);

        interceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {

                Artist a3 = nestedContext.localObject(a1);
                assertNotSame(a3, a1);
                assertEquals(a3.getObjectId(), a1.getObjectId());
                assertSame(nestedContext, a3.getObjectContext());
            }
        });
    }
}

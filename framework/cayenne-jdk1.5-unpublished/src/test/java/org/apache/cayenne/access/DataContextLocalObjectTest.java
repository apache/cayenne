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
package org.apache.cayenne.access;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextLocalObjectTest extends ServerCase {

    @Inject
    private DataContext context1;

    @Inject
    private DataContext context2;

    @Inject
    private DBHelper dbHelper;

    @Inject
    private DataChannelInterceptor interceptor;

    @Inject
    private ServerRuntime runtime;

    private TableHelper tArtist;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");
        dbHelper.deleteAll("EXHIBIT");
        dbHelper.deleteAll("GALLERY");

        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");
    }

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

    public void testLocalObject_TempId() throws Exception {

        final Artist a1 = context1.newObject(Artist.class);

        interceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {

                try {
                    context2.localObject(a1);
                    fail("returned a local object for temp ID");
                }
                catch (CayenneRuntimeException e) {
                    // expected
                }
            }
        });
    }

    public void testLocalObject_TempId_NestedContext() throws Exception {

        final Artist a1 = context1.newObject(Artist.class);

        final ObjectContext nestedContext = runtime.getContext(context1);

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

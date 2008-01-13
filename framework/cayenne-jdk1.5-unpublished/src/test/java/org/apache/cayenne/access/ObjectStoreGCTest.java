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

import org.apache.art.Artist;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.util.ThreadedTestHelper;

public class ObjectStoreGCTest extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testReleaseUnreferenced() throws Exception {
        final DataContext context = createDataContext();
        context.performGenericQuery(new SQLTemplate(
                Artist.class,
                "insert into ARTIST (ARTIST_ID, ARTIST_NAME) values (1, 'aa')"));

        assertEquals(0, context.getObjectStore().registeredObjectsCount());
        context.performQuery(new SelectQuery(Artist.class));
        assertEquals(1, context.getObjectStore().registeredObjectsCount());

        // allow for slow GC
        new ThreadedTestHelper() {

            @Override
            protected void assertResult() throws Exception {
                System.gc();
                assertEquals(0, context.getObjectStore().registeredObjectsCount());
            }
        }.assertWithTimeout(2000);
    }

    public void testRetainUnreferencedNew() throws Exception {
        final DataContext context = createDataContext();

        assertEquals(0, context.getObjectStore().registeredObjectsCount());
        Artist a = context.newObject(Artist.class);
        a.setArtistName("X");
        a = null;
        assertEquals(1, context.getObjectStore().registeredObjectsCount());

        // allow for slow GC
        new ThreadedTestHelper() {

            @Override
            protected void assertResult() throws Exception {
                System.gc();
                assertEquals(1, context.getObjectStore().registeredObjectsCount());
            }
        }.assertWithTimeout(2000);

        assertEquals(1, context.getObjectStore().registeredObjectsCount());
        context.commitChanges();
        new ThreadedTestHelper() {

            @Override
            protected void assertResult() throws Exception {
                System.gc();
                assertEquals(0, context.getObjectStore().registeredObjectsCount());
            }
        }.assertWithTimeout(2000);

    }

    public void testRetainUnreferencedModified() throws Exception {
        final DataContext context = createDataContext();
        context.performGenericQuery(new SQLTemplate(
                Artist.class,
                "insert into ARTIST (ARTIST_ID, ARTIST_NAME) values (1, 'aa')"));

        assertEquals(0, context.getObjectStore().registeredObjectsCount());
        Artist a = DataObjectUtils.objectForPK(context, Artist.class, 1);
        a.setArtistName("Y");
        a = null;
        assertEquals(1, context.getObjectStore().registeredObjectsCount());

        new ThreadedTestHelper() {

            @Override
            protected void assertResult() throws Exception {
                System.gc();
                assertEquals(1, context.getObjectStore().registeredObjectsCount());
            }
        }.assertWithTimeout(2000);

        context.commitChanges();
        new ThreadedTestHelper() {

            @Override
            protected void assertResult() throws Exception {
                System.gc();
                assertEquals(0, context.getObjectStore().registeredObjectsCount());
            }
        }.assertWithTimeout(2000);

    }
}

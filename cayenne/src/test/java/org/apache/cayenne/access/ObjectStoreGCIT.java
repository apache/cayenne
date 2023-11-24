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
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.test.parallel.ParallelTestContainer;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.apache.cayenne.unit.di.runtime.WeakReferenceStrategyRuntimeCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class ObjectStoreGCIT extends WeakReferenceStrategyRuntimeCase {

    @Inject
    private DataContext context;

    @Test
    public void testReleaseUnreferenced() throws Exception {
        context.performGenericQuery(new SQLTemplate(
                Artist.class,
                "insert into ARTIST (ARTIST_ID, ARTIST_NAME) values (1, 'aa')"));

        assertEquals(0, context.getObjectStore().registeredObjectsCount());
        ObjectSelect.query(Artist.class).select(context);
        assertEquals(1, context.getObjectStore().registeredObjectsCount());

        // allow for slow GC
        new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                System.gc();
                assertEquals(0, context.getObjectStore().registeredObjectsCount());
            }
        }.runTest(2000);
    }

    @Test
    public void testRetainUnreferencedNew() throws Exception {
        assertEquals(0, context.getObjectStore().registeredObjectsCount());
        Artist a = context.newObject(Artist.class);
        a.setArtistName("X");
        a = null;
        assertEquals(1, context.getObjectStore().registeredObjectsCount());

        // allow for slow GC
        new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                System.gc();
                assertEquals(1, context.getObjectStore().registeredObjectsCount());
            }
        }.runTest(2000);

        assertEquals(1, context.getObjectStore().registeredObjectsCount());
        context.commitChanges();
        new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                System.gc();
                assertEquals(0, context.getObjectStore().registeredObjectsCount());
            }
        }.runTest(2000);

    }

    @Test
    public void testRetainUnreferencedModified() throws Exception {
        context.performGenericQuery(new SQLTemplate(
                Artist.class,
                "insert into ARTIST (ARTIST_ID, ARTIST_NAME) values (1, 'aa')"));

        assertEquals(0, context.getObjectStore().registeredObjectsCount());
        Artist a = Cayenne.objectForPK(context, Artist.class, 1);
        a.setArtistName("Y");
        a = null;
        assertEquals(1, context.getObjectStore().registeredObjectsCount());

        new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                System.gc();
                assertEquals(1, context.getObjectStore().registeredObjectsCount());
            }
        }.runTest(2000);

        context.commitChanges();
        new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                System.gc();
                assertEquals(0, context.getObjectStore().registeredObjectsCount());
            }
        }.runTest(2000);

    }
}

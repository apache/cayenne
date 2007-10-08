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
import org.apache.art.Painting;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class ObjectStoreDiffRetainingTst extends CayenneTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testSnapshotRetainedOnPropertyModification() throws Exception {
        createTestData("test");

        Artist a = (Artist) DataObjectUtils.objectForPK(
                createDataContext(),
                Artist.class,
                2000);
        ObjectStore objectStore = a.getDataContext().getObjectStore();

        assertNull(objectStore.getChangesByObjectId().get(a.getObjectId()));

        a.setArtistName("some other name");
        assertNotNull(objectStore.getChangesByObjectId().get(a.getObjectId()));
    }

    public void testSnapshotRetainedOnRelAndPropertyModification() throws Exception {
        createTestData("test");

        Artist a = (Artist) DataObjectUtils.objectForPK(
                createDataContext(),
                Artist.class,
                2000);
        ObjectStore objectStore = a.getDataContext().getObjectStore();

        assertNull(objectStore.getChangesByObjectId().get(a.getObjectId()));

        // we are trying to reproduce the bug CAY-213 - relationship modification puts
        // object in a modified state, so later when object is really modified, its
        // snapshot is not retained... in testing this I am leaving some flexibility for
        // the framework to retain a snapshot when it deems appropriate...

        a.addToPaintingArray((Painting) a.getDataContext().createAndRegisterNewObject(
                Painting.class));
        a.setArtistName("some other name");
        assertNotNull("Snapshot wasn't retained - CAY-213", objectStore
                .getChangesByObjectId()
                .get(a.getObjectId()));
    }
}

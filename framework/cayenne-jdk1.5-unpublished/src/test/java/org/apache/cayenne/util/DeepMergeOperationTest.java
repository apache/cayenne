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

package org.apache.cayenne.util;

import org.apache.art.Artist;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.util.DeepMergeOperation;

public class DeepMergeOperationTest extends CayenneCase {

    public void testDeepMergeNonExistent() {

        ClassDescriptor d = getDomain().getEntityResolver().getClassDescriptor("Artist");

        DataContext context = createDataContext();
        DataContext context1 = createDataContext();

        Artist a = context.newObject(Artist.class);
        a.setArtistName("AAA");
        context.commitChanges();

        DeepMergeOperation op = new DeepMergeOperation(context1);
        blockQueries();
        try {
            Artist a2 = (Artist) op.merge(a, d);
            assertNotNull(a2);
            assertEquals(PersistenceState.COMMITTED, a2.getPersistenceState());
            assertEquals(a.getArtistName(), a2.getArtistName());
        }
        finally {
            unblockQueries();
        }
    }

    public void testDeepMergeModified() {

        ClassDescriptor d = getDomain().getEntityResolver().getClassDescriptor("Artist");

        DataContext context = createDataContext();
        DataContext context1 = createDataContext();

        Artist a = context.newObject(Artist.class);
        a.setArtistName("AAA");
        context.commitChanges();

        Artist a1 = (Artist) DataObjectUtils.objectForPK(context1, a.getObjectId());
        a1.setArtistName("BBB");
        DeepMergeOperation op = new DeepMergeOperation(context1);

        blockQueries();
        try {
            Artist a2 = (Artist) op.merge(a, d);
            assertNotNull(a2);
            assertEquals(PersistenceState.MODIFIED, a2.getPersistenceState());
            assertSame(a1, a2);
            assertEquals("BBB", a2.getArtistName());
        }
        finally {
            unblockQueries();
        }
    }

}

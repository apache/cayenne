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
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.MockGraphChangeHandler;
import org.apache.cayenne.unit.CayenneCase;

public class DataContextCommitTest extends CayenneCase {

    public void testFlushToParent_Commit() {

        DataContext context = createDataContext();
        
        // commit new object
        Artist a = context.newObject(Artist.class);
        a.setArtistName("Test");

        assertTrue(context.hasChanges());

        GraphDiff diff = context.flushToParent(true);
        assertNotNull(diff);
        assertFalse(context.hasChanges());

        final Object[] newIds = new Object[1];

        MockGraphChangeHandler diffChecker = new MockGraphChangeHandler() {

            @Override
            public void nodeIdChanged(Object nodeId, Object newId) {
                super.nodeIdChanged(nodeId, newId);

                newIds[0] = newId;
            }
        };

        diff.apply(diffChecker);
        assertEquals(1, diffChecker.getCallbackCount());
        assertSame(a.getObjectId(), newIds[0]);
        
        // commit a mix of new and modified
        Painting p = context.newObject(Painting.class);
        p.setPaintingTitle("PT");
        p.setToArtist(a);
        a.setArtistName(a.getArtistName() + "_");
        
        GraphDiff diff2 = context.flushToParent(true);
        assertNotNull(diff2);
        assertFalse(context.hasChanges());

        final Object[] newIds2 = new Object[1];

        MockGraphChangeHandler diffChecker2 = new MockGraphChangeHandler() {

            @Override
            public void nodeIdChanged(Object nodeId, Object newId) {
                super.nodeIdChanged(nodeId, newId);

                newIds2[0] = newId;
            }
        };

        diff2.apply(diffChecker2);
        assertEquals(1, diffChecker2.getCallbackCount());
        assertSame(p.getObjectId(), newIds2[0]);
    }
}

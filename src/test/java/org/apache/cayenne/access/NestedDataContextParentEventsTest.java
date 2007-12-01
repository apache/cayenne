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
import org.apache.cayenne.unit.CayenneCase;

public class NestedDataContextParentEventsTest extends CayenneCase {

    public void testParentUpdatedId() {
        DataContext context = createDataContext();
        DataContext child1 = context.createChildDataContext();

        Artist ac = child1.newObject(Artist.class);
        ac.setArtistName("X");
        child1.commitChangesToParent();

        Artist ap = (Artist) context.getGraphManager().getNode(ac.getObjectId());
        assertNotNull(ap);

        assertTrue(ap.getObjectId().isTemporary());
        context.commitChanges();

        assertFalse(ap.getObjectId().isTemporary());
        assertEquals(ap.getObjectId(), ac.getObjectId());
    }
}

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

package org.apache.cayenne;

import java.util.Arrays;

import org.apache.art.Artist;
import org.apache.art.Painting1;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.unit.CayenneCase;

/**
 * Tests DataObjects with no reverse relationships.
 * 
 * @author Andrus Adamchik
 */
public class CDOMany2OneNoRevTest extends CayenneCase {

    public void testNewAdd() throws Exception {
        deleteTestData();

        DataContext context = createDataContext();

        Artist a1 = (Artist) context.newObject("Artist");
        a1.setArtistName("a");
        Painting1 p1 = (Painting1) context.newObject("Painting1");
        p1.setPaintingTitle("p");

        // *** TESTING THIS ***
        p1.setToArtist(a1);

        assertSame(a1, p1.getToArtist());

        context.commitChanges();
        ObjectId aid = a1.getObjectId();
        ObjectId pid = p1.getObjectId();
        context.invalidateObjects(Arrays.asList(new Object[] {
                a1, p1
        }));

        Painting1 p2 = (Painting1) DataObjectUtils.objectForPK(context, pid);
        Artist a2 = p2.getToArtist();
        assertNotNull(a2);
        assertEquals(aid, a2.getObjectId());
    }
}

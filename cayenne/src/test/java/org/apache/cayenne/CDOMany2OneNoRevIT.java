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

package org.apache.cayenne;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting1;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class CDOMany2OneNoRevIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Test
    public void testNewAdd() throws Exception {

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("a");
        Painting1 p1 = context.newObject(Painting1.class);
        p1.setPaintingTitle("p");

        // TESTING THIS
        p1.setToArtist(a1);

        assertSame(a1, p1.getToArtist());

        context.commitChanges();
        ObjectId aid = a1.getObjectId();
        ObjectId pid = p1.getObjectId();
        context.invalidateObjects(a1, p1);

        Painting1 p2 = (Painting1) Cayenne.objectForPK(context, pid);
        Artist a2 = p2.getToArtist();
        assertNotNull(a2);
        assertEquals(aid, a2.getObjectId());
    }
}

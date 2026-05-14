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

import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting1;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class CDOMany2OneNoRevIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void newAdd() throws Exception {

        Artist a1 = env.context().newObject(Artist.class);
        a1.setArtistName("a");
        Painting1 p1 = env.context().newObject(Painting1.class);
        p1.setPaintingTitle("p");

        // TESTING THIS
        p1.setToArtist(a1);

        assertSame(a1, p1.getToArtist());

        env.context().commitChanges();
        ObjectId aid = a1.getObjectId();
        ObjectId pid = p1.getObjectId();
        env.context().invalidateObjects(a1, p1);

        Painting1 p2 = (Painting1) Cayenne.objectForPK(env.context(), pid);
        Artist a2 = p2.getToArtist();
        assertNotNull(a2);
        assertEquals(aid, a2.getObjectId());
    }
}

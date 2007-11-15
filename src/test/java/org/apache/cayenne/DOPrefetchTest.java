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

import java.util.List;

import org.apache.art.Artist;
import org.apache.art.Painting;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

public class DOPrefetchTest extends CayenneDOTestBase {

    public void testPrefetchToMany() throws Exception {
        Artist a1 = super.newArtist();
        Painting p1 = super.newPainting();
        p1.setToArtist(a1);
        ctxt.commitChanges();

        ctxt = createDataContext();
        Expression e = ExpressionFactory.likeExp("artistName", "artist%");
        SelectQuery q = new SelectQuery("Artist", e);

        // ** TESTING THIS **
        q.addPrefetch("paintingArray");

        List artists = ctxt.performQuery(q);
        assertEquals(1, artists.size());
        Artist a2 = (Artist) artists.get(0);
        assertNotNull(a2);
    }
}

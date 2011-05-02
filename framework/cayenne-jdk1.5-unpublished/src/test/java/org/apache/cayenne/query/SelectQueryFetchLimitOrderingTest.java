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

package org.apache.cayenne.query;

import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class SelectQueryFetchLimitOrderingTest extends ServerCase {

    @Inject
    protected ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tArtist;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");

        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");
    }

    protected void creatArtistsDataSet() throws Exception {
        tArtist.insert(33001, "c");
        tArtist.insert(33002, "b");
        tArtist.insert(33003, "f");
        tArtist.insert(33004, "d");
        tArtist.insert(33005, "a");
        tArtist.insert(33006, "e");
    }

    public void testOrdering() throws Exception {

        creatArtistsDataSet();

        SelectQuery query = new SelectQuery("Artist");
        query.addOrdering(Artist.ARTIST_NAME_PROPERTY, SortOrder.ASCENDING);

        query.setFetchLimit(4);

        List<?> results = context.performQuery(query);
        assertEquals(4, results.size());

        assertEquals("a", ((Artist) results.get(0)).getArtistName());
        assertEquals("b", ((Artist) results.get(1)).getArtistName());
        assertEquals("c", ((Artist) results.get(2)).getArtistName());
        assertEquals("d", ((Artist) results.get(3)).getArtistName());
    }
}

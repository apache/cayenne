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
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class ObjectSelect_FetchLimitOrderingIT extends RuntimeCase {

    @Inject
    protected ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tArtist;

    @Before
    public void setUp() throws Exception {
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

    @Test
    public void testOrdering() throws Exception {

        creatArtistsDataSet();

        List<Artist> results = ObjectSelect.query(Artist.class).orderBy(Artist.ARTIST_NAME.asc()).limit(4).select(context);
        assertEquals(4, results.size());

        assertEquals("a", results.get(0).getArtistName());
        assertEquals("b", results.get(1).getArtistName());
        assertEquals("c", results.get(2).getArtistName());
        assertEquals("d", results.get(3).getArtistName());
    }
}

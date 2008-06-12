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

import java.util.Calendar;
import java.util.List;

import org.apache.art.Artist;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

public class DataContextOrderingTest extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testMultipleOrdering() throws Exception {

        Calendar c = Calendar.getInstance();
        
        DataContext context = createDataContext();
        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("2");
        a1.setDateOfBirth(c.getTime());
        
        c.add(Calendar.DAY_OF_MONTH, -1);
        Artist a2 = context.newObject(Artist.class);
        a2.setArtistName("3");
        a2.setDateOfBirth(c.getTime());
        
        c.add(Calendar.DAY_OF_MONTH, -1);
        Artist a3 = context.newObject(Artist.class);
        a3.setArtistName("3");
        a3.setDateOfBirth(c.getTime());
        
        context.commitChanges();
        
        SelectQuery query = new SelectQuery(Artist.class);
        query.addOrdering(Artist.ARTIST_NAME_PROPERTY, Ordering.DESC);
        query.addOrdering(Artist.DATE_OF_BIRTH_PROPERTY, Ordering.DESC);
        
        List<Artist> list = context.performQuery(query);
        assertEquals(3, list.size());
        assertSame(a2, list.get(0));
        assertSame(a3, list.get(1));
        assertSame(a1, list.get(2));
    }
}

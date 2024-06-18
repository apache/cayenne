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
package org.apache.cayenne.access;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextOrderingIT extends RuntimeCase {

    @Inject
    private DataContext context;

    @Test
    public void testMultipleOrdering() {

        Calendar c = Calendar.getInstance();

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

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .orderBy(Artist.ARTIST_NAME.desc(), Artist.DATE_OF_BIRTH.desc());

        List<Artist> list = query.select(context);
        assertEquals(3, list.size());
        assertSame(a2, list.get(0));
        assertSame(a3, list.get(1));
        assertSame(a1, list.get(2));
    }

    @Test
    public void testMultipleOrderingInSelectClauseCAY_1074() throws Exception {

        Calendar c = Calendar.getInstance();

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

        Painting p1 = context.newObject(Painting.class);
        p1.setEstimatedPrice(new BigDecimal(1));
        p1.setPaintingTitle("Y");
        a1.addToPaintingArray(p1);

        Painting p2 = context.newObject(Painting.class);
        p2.setEstimatedPrice(new BigDecimal(2));
        p2.setPaintingTitle("X");
        a2.addToPaintingArray(p2);

        context.commitChanges();

        ObjectSelect<Artist> query1 = ObjectSelect.query(Artist.class)
                // per CAY-1074, adding a to-many join to expression messes up the ordering
                .and(Artist.PAINTING_ARRAY.ne((List<Painting>) null))
                .orderBy(Artist.ARTIST_NAME.desc(), (Artist.DATE_OF_BIRTH.desc()));

        List<Artist> list1 = query1.select(context);
        assertEquals(2, list1.size());
    }

    @Test
    public void testCustomPropertySort() throws Exception {
        Calendar c = Calendar.getInstance();

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("31");
        a1.setDateOfBirth(c.getTime());

        c.add(Calendar.DAY_OF_MONTH, -1);
        Artist a2 = context.newObject(Artist.class);
        a2.setArtistName("22");
        a2.setDateOfBirth(c.getTime());

        c.add(Calendar.DAY_OF_MONTH, -1);
        Artist a3 = context.newObject(Artist.class);
        a3.setArtistName("13");
        a3.setDateOfBirth(c.getTime());

        context.commitChanges();

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .orderBy(Artist.ARTIST_NAME.substring(2, 1).desc());

        List<Artist> list = query.select(context);
        assertEquals(3, list.size());
        assertSame(a3, list.get(0));
        assertSame(a2, list.get(1));
        assertSame(a1, list.get(2));
    }
}

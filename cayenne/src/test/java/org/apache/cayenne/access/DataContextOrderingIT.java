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

import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class DataContextOrderingIT  {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void multipleOrdering() {

        Calendar c = Calendar.getInstance();

        Artist a1 = env.context().newObject(Artist.class);
        a1.setArtistName("2");
        a1.setDateOfBirth(c.getTime());

        c.add(Calendar.DAY_OF_MONTH, -1);
        Artist a2 = env.context().newObject(Artist.class);
        a2.setArtistName("3");
        a2.setDateOfBirth(c.getTime());

        c.add(Calendar.DAY_OF_MONTH, -1);
        Artist a3 = env.context().newObject(Artist.class);
        a3.setArtistName("3");
        a3.setDateOfBirth(c.getTime());

        env.context().commitChanges();

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .orderBy(Artist.ARTIST_NAME.desc(), Artist.DATE_OF_BIRTH.desc());

        List<Artist> list = query.select(env.context());
        assertEquals(3, list.size());
        assertSame(a2, list.get(0));
        assertSame(a3, list.get(1));
        assertSame(a1, list.get(2));
    }

    @Test
    public void multipleOrderingInSelectClauseCAY_1074() throws Exception {

        Calendar c = Calendar.getInstance();

        Artist a1 = env.context().newObject(Artist.class);
        a1.setArtistName("2");
        a1.setDateOfBirth(c.getTime());

        c.add(Calendar.DAY_OF_MONTH, -1);
        Artist a2 = env.context().newObject(Artist.class);
        a2.setArtistName("3");
        a2.setDateOfBirth(c.getTime());

        c.add(Calendar.DAY_OF_MONTH, -1);
        Artist a3 = env.context().newObject(Artist.class);
        a3.setArtistName("3");
        a3.setDateOfBirth(c.getTime());

        Painting p1 = env.context().newObject(Painting.class);
        p1.setEstimatedPrice(new BigDecimal(1));
        p1.setPaintingTitle("Y");
        a1.addToPaintingArray(p1);

        Painting p2 = env.context().newObject(Painting.class);
        p2.setEstimatedPrice(new BigDecimal(2));
        p2.setPaintingTitle("X");
        a2.addToPaintingArray(p2);

        env.context().commitChanges();

        ObjectSelect<Artist> query1 = ObjectSelect.query(Artist.class)
                // per CAY-1074, adding a to-many join to expression messes up the ordering
                .and(Artist.PAINTING_ARRAY.ne((List<Painting>) null))
                .orderBy(Artist.ARTIST_NAME.desc(), (Artist.DATE_OF_BIRTH.desc()));

        List<Artist> list1 = query1.select(env.context());
        assertEquals(2, list1.size());
    }

    @Test
    public void customPropertySort() throws Exception {
        Calendar c = Calendar.getInstance();

        Artist a1 = env.context().newObject(Artist.class);
        a1.setArtistName("31");
        a1.setDateOfBirth(c.getTime());

        c.add(Calendar.DAY_OF_MONTH, -1);
        Artist a2 = env.context().newObject(Artist.class);
        a2.setArtistName("22");
        a2.setDateOfBirth(c.getTime());

        c.add(Calendar.DAY_OF_MONTH, -1);
        Artist a3 = env.context().newObject(Artist.class);
        a3.setArtistName("13");
        a3.setDateOfBirth(c.getTime());

        env.context().commitChanges();

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .orderBy(Artist.ARTIST_NAME.substring(2, 1).desc());

        List<Artist> list = query.select(env.context());
        assertEquals(3, list.size());
        assertSame(a3, list.get(0));
        assertSame(a2, list.get(1));
        assertSame(a1, list.get(2));
    }
}

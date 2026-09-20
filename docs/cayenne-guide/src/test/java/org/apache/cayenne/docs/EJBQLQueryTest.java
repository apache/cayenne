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
package org.apache.cayenne.docs;

import org.apache.cayenne.docs.persistent.Artist;
import org.apache.cayenne.docs.persistent.Painting;
import org.apache.cayenne.query.EJBQLQuery;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EJBQLQueryTest extends BaseTest {

    @Test
    public void create() {
        // tag::create[]
        EJBQLQuery<Artist> query = new EJBQLQuery<>("select a FROM Artist a");
        // end::create[]

        assertNotNull(query);
    }

    @Test
    public void select() {
        createArtistsDataSet();

        // tag::select[]
        EJBQLQuery<Artist> select =
                new EJBQLQuery<>("select a FROM Artist a WHERE a.artistName = 'Dali'");
        List<Artist> artists = context.select(select);
        // end::select[]

        assertEquals(1, artists.size());
    }

    @Test
    public void delete() {
        createArtistsDataSet();

        // tag::delete[]
        EJBQLQuery<?> delete = new EJBQLQuery<>("delete from Painting");
        context.execute(delete);
        // end::delete[]

        assertEquals(0, context.select(new EJBQLQuery<Painting>("select p FROM Painting p")).size());
    }

    @Test
    public void update() {
        createArtistsDataSet();

        // tag::update[]
        EJBQLQuery<?> update =
                new EJBQLQuery<>("UPDATE Painting AS p SET p.paintingTitle = 'P3' WHERE p.paintingTitle = 'P1'");
        context.execute(update);
        // end::update[]

        assertEquals(1, context
                .select(new EJBQLQuery<Painting>("select p FROM Painting p WHERE p.paintingTitle = 'P3'"))
                .size());
    }

    @Test
    public void objectsAndScalars() {
        createArtistsDataSet();

        // tag::objectsAndScalars[]
        EJBQLQuery<Object[]> query =
                new EJBQLQuery<>("select a, COUNT(p) FROM Artist a JOIN a.paintingArray p GROUP BY a");
        List<Object[]> result = context.select(query);
        for (Object[] artistWithCount : result) {
            Artist a = (Artist) artistWithCount[0];
            long hasPaintings = (Long) artistWithCount[1];
        }
        // end::objectsAndScalars[]

        assertEquals(2, result.size());
    }

    @Test
    public void scalars() {
        createArtistsDataSet();

        // tag::scalars[]
        EJBQLQuery<String> query = new EJBQLQuery<>("select a.artistName FROM Artist a");
        List<String> names = context.select(query);
        // end::scalars[]

        assertEquals(3, names.size());
    }

    @Test
    public void inPositional() {
        createArtistsDataSet();

        // tag::inPositional[]
        EJBQLQuery<Painting> query =
                new EJBQLQuery<>("select p from Painting p where p.paintingTitle in (?1,?2,?3)");
        query.setParameter(1, "P1");
        query.setParameter(2, "P2");
        query.setParameter(3, "P3");
        // end::inPositional[]

        assertEquals(2, context.select(query).size());
    }

    @Test
    public void inCollection() {
        createArtistsDataSet();

        // tag::inCollection[]
        EJBQLQuery<Painting> query = new EJBQLQuery<>("select p from Painting p where p.paintingTitle in ?1");
        query.setParameter(1, List.of("P1", "P2", "P3"));
        // end::inCollection[]

        assertEquals(2, context.select(query).size());
    }

    @Test
    public void inCollectionInParentheses() {
        createArtistsDataSet();

        // tag::inCollectionInParentheses[]
        EJBQLQuery<Painting> query = new EJBQLQuery<>("select p from Painting p where p.paintingTitle in (?1)");
        query.setParameter(1, List.of("P1", "P2", "P3"));
        // end::inCollectionInParentheses[]

        assertEquals(2, context.select(query).size());
    }
}

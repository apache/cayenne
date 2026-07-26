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

import org.apache.cayenne.configuration.runtime.CoreModule;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * "maxIdQualifierSize" is immutable and comes from the runtime properties, so each size under test needs its own
 * stack.
 */
public class DataContextMaxIdQualifierIT {

    private static CayenneTestsEnv envWithMaxIdQualifierSize(int size) {
        return CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT)
                .withExtraModules(b -> CoreModule.extend(b).maxIdQualifierSize(size));
    }

    private static void insert100ArtistsWithAPaintingEach(CayenneTestsEnv env) throws SQLException {
        TableHelper tArtist = artistTable(env);
        TableHelper tPainting = paintingTable(env);

        for (int i = 1; i <= 100; i++) {
            tArtist.insert(i, "AA" + i);
            tPainting.insert(i, i, "P" + i);
        }
    }

    private static void insertOneArtistWith100Paintings(CayenneTestsEnv env) throws SQLException {
        artistTable(env).insert(1, "AA1");

        TableHelper tPainting = paintingTable(env);
        for (int i = 1; i <= 100; i++) {
            tPainting.insert(i, 1, "P" + i);
        }
    }

    private static TableHelper artistTable(CayenneTestsEnv env) {
        return env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME");
    }

    private static TableHelper paintingTable(CayenneTestsEnv env) {
        return env.table("PAINTING")
                .setColumns("PAINTING_ID", "ARTIST_ID", "PAINTING_TITLE")
                .setColumnTypes(Types.INTEGER, Types.BIGINT, Types.VARCHAR);
    }

    @Nested
    public class Size10 {

        @RegisterExtension
        final CayenneTestsEnv env = envWithMaxIdQualifierSize(10);

        @Test
        public void disjointByIdPrefetch() throws Exception {
            insert100ArtistsWithAPaintingEach(env);

            int queriesCount = env.runWithQueryCounter(() ->
                    ObjectSelect.query(Artist.class)
                            .prefetch(Artist.PAINTING_ARRAY.disjointById())
                            .select(env.context()));

            assertEquals(11, queriesCount);
        }
    }

    @Nested
    public class Size5 {

        @RegisterExtension
        final CayenneTestsEnv env = envWithMaxIdQualifierSize(5);

        @Test
        public void incrementalFaultList() throws Exception {
            insertOneArtistWith100Paintings(env);

            ObjectSelect<Painting> query = ObjectSelect.query(Painting.class).pageSize(10);

            int queriesCount = env.runWithQueryCounter(() -> {
                List<Painting> paintings = query.select(env.context());
                for (Painting painting : paintings) {
                    painting.getToArtist();
                }
            });

            assertEquals(21, queriesCount);

            queriesCount = env.runWithQueryCounter(() -> {
                List<Painting> paintings = query.select(env.context());
                List<Painting> tempList = new ArrayList<>(paintings);
            });

            assertEquals(21, queriesCount);
        }
    }

    @Nested
    public class Size101 {

        @RegisterExtension
        final CayenneTestsEnv env = envWithMaxIdQualifierSize(101);

        @Test
        public void incrementalFaultList() throws Exception {
            insertOneArtistWith100Paintings(env);

            ObjectSelect<Painting> query = ObjectSelect.query(Painting.class).pageSize(10);

            int queriesCount = env.runWithQueryCounter(() -> {
                List<Painting> paintings = query.select(env.context());
                for (Painting painting : paintings) {
                    painting.getToArtist();
                }
            });

            assertEquals(11, queriesCount);

            queriesCount = env.runWithQueryCounter(() -> {
                List<Painting> paintings = query.select(env.context());
                List<Painting> tempList = new ArrayList<>(paintings);
            });

            assertEquals(2, queriesCount);
        }
    }

    @Nested
    public class SizeZero {

        @RegisterExtension
        final CayenneTestsEnv env = envWithMaxIdQualifierSize(0);

        @Test
        public void disjointByIdPrefetch() throws Exception {
            insert100ArtistsWithAPaintingEach(env);

            int queriesCount = env.runWithQueryCounter(() ->
                    ObjectSelect.query(Artist.class)
                            .prefetch(Artist.PAINTING_ARRAY.disjointById())
                            .select(env.context()));

            assertEquals(2, queriesCount);
        }

        @Test
        public void incrementalFaultList() throws Exception {
            insertOneArtistWith100Paintings(env);

            ObjectSelect<Painting> query = ObjectSelect.query(Painting.class).pageSize(10);

            int queriesCount = env.runWithQueryCounter(() -> {
                List<Painting> paintings = query.select(env.context());
                List<Painting> tempList = new ArrayList<>(paintings);
            });

            assertEquals(2, queriesCount);
        }
    }

    @Nested
    public class SizeNegative {

        @RegisterExtension
        final CayenneTestsEnv env = envWithMaxIdQualifierSize(-1);

        @Test
        public void disjointByIdPrefetch() throws Exception {
            insert100ArtistsWithAPaintingEach(env);

            int queriesCount = env.runWithQueryCounter(() ->
                    ObjectSelect.query(Artist.class)
                            .prefetch(Artist.PAINTING_ARRAY.disjointById())
                            .select(env.context()));

            assertEquals(2, queriesCount);
        }

        @Test
        public void incrementalFaultList() throws Exception {
            insertOneArtistWith100Paintings(env);

            ObjectSelect<Painting> query = ObjectSelect.query(Painting.class).pageSize(10);

            int queriesCount = env.runWithQueryCounter(() -> {
                List<Painting> paintings = query.select(env.context());
                List<Painting> tempList = new ArrayList<>(paintings);
            });

            assertEquals(2, queriesCount);
        }
    }
}

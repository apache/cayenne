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

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataContextMaxIdQualifierIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    protected DataContext context;
    protected DataChannelInterceptor queryInterceptor;
    protected CayenneRuntime runtime;
    
    private TableHelper tArtist;
    private TableHelper tPainting;

    @BeforeEach
    public void setUp() throws Exception {
        context = env.context();
        queryInterceptor = env.getInstance(DataChannelInterceptor.class);
        runtime = env.runtime();
        tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME");

        tPainting = env.table("PAINTING").setColumns("PAINTING_ID", "ARTIST_ID", "PAINTING_TITLE").setColumnTypes(Types.INTEGER, Types.BIGINT,
                Types.VARCHAR);
    }

    private void insertData() throws SQLException {
        
        for (int i = 1; i <= 100; i++) {
            tArtist.insert(i, "AA" + i);
            tPainting.insert(i, i, "P" + i);
        }
    }

    private void insertData_OneBag_100Boxes() throws SQLException {
        tArtist.insert(1, "AA1");

        for (int i = 1; i <= 100; i++) {
            tPainting.insert(i, 1, "P" + i);
        }
    }

    @Test
    public void disjointByIdPrefetch() throws Exception {
        insertData();
        runtime.getDataDomain().setMaxIdQualifierSize(10);

        int queriesCount = queryInterceptor.runWithQueryCounter(() ->
                ObjectSelect.query(Artist.class)
                        .prefetch(Artist.PAINTING_ARRAY.disjointById())
                        .select(context));

        assertEquals(11, queriesCount);
    }

    @Test
    public void disjointByIdPrefetch_Zero() throws Exception {
        insertData();
        runtime.getDataDomain().setMaxIdQualifierSize(0);

        int queriesCount = queryInterceptor.runWithQueryCounter(() ->
                ObjectSelect.query(Artist.class)
                        .prefetch(Artist.PAINTING_ARRAY.disjointById())
                        .select(context));

        assertEquals(2, queriesCount);
    }

    @Test
    public void disjointByIdPrefetch_Negative() throws Exception {
        insertData();
        runtime.getDataDomain().setMaxIdQualifierSize(-1);

        int queriesCount = queryInterceptor.runWithQueryCounter(() ->
                ObjectSelect.query(Artist.class)
                        .prefetch(Artist.PAINTING_ARRAY.disjointById())
                        .select(context));

        assertEquals(2, queriesCount);
    }

    @Test
    public void incrementalFaultList_Lower() throws Exception {
        insertData_OneBag_100Boxes();

        runtime.getDataDomain().setMaxIdQualifierSize(5);

        ObjectSelect<Painting> query = ObjectSelect.query(Painting.class).pageSize(10);

        int queriesCount = queryInterceptor.runWithQueryCounter(() -> {
            List<Painting> boxes = query.select(context);
            for (Painting box : boxes) {
                box.getToArtist();
            }
        });

        assertEquals(21, queriesCount);

        queriesCount = queryInterceptor.runWithQueryCounter(() -> {
            List<Painting> boxes = query.select(context);
            List<Painting> tempList = new ArrayList<>(boxes);
        });

        assertEquals(21, queriesCount);
    }

    @Test
    public void incrementalFaultList_Higher() throws Exception {
        insertData_OneBag_100Boxes();

        runtime.getDataDomain().setMaxIdQualifierSize(101);

        ObjectSelect<Painting> query = ObjectSelect.query(Painting.class).pageSize(10);

        int queriesCount = queryInterceptor.runWithQueryCounter(() -> {
            final List<Painting> boxes = query.select(context);
            for (Painting box : boxes) {
                box.getToArtist();
            }
        });

        assertEquals(11, queriesCount);

        queriesCount = queryInterceptor.runWithQueryCounter(() -> {
            final List<Painting> boxes = query.select(context);
            List<Painting> tempList = new ArrayList<>(boxes);
        });

        assertEquals(2, queriesCount);
    }

    @Test
    public void incrementalFaultList_Zero() throws Exception {
        insertData_OneBag_100Boxes();

        runtime.getDataDomain().setMaxIdQualifierSize(0);

        ObjectSelect<Painting> query = ObjectSelect.query(Painting.class).pageSize(10);

        int queriesCount = queryInterceptor.runWithQueryCounter(() -> {
            final List<Painting> boxes = query.select(context);
            List<Painting> tempList = new ArrayList<>(boxes);
        });

        assertEquals(2, queriesCount);
    }

    @Test
    public void incrementalFaultList_Negative() throws Exception {
        insertData_OneBag_100Boxes();

        runtime.getDataDomain().setMaxIdQualifierSize(-1);

        ObjectSelect<Painting> query = ObjectSelect.query(Painting.class).pageSize(10);

        int queriesCount = queryInterceptor.runWithQueryCounter(() -> {
            final List<Painting> boxes = query.select(context);
            List<Painting> tempList = new ArrayList<>(boxes);
        });

        assertEquals(2, queriesCount);
    }
}

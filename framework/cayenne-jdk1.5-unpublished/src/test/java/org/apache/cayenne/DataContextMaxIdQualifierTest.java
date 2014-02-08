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

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextMaxIdQualifierTest extends ServerCase {

    @Inject
    protected DataContext context;

    @Inject
    protected DBHelper dbHelper;

    @Inject
    protected DataChannelInterceptor queryInterceptor;

    @Inject
    protected ServerRuntime runtime;
    
    private TableHelper tArtist;
    private TableHelper tPainting;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");

        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns("PAINTING_ID", "ARTIST_ID", "PAINTING_TITLE").setColumnTypes(Types.INTEGER, Types.BIGINT,
                Types.VARCHAR);
    }

    private void insertData() throws SQLException {
        
        for (int i = 1; i <= 1000; i++) {
            tArtist.insert(i, "AA" + i);
            tPainting.insert(i, i, "P" + i);
        }
    }

    private void insertData_OneBag_1000Boxes() throws SQLException {
        tArtist.insert(1, "AA1");

        for (int i = 1; i <= 1000; i++) {
            tPainting.insert(i, 1, "P" + i);
        }
    }

    public void testDisjointByIdPrefetch() throws Exception {
        insertData();
        runtime.getDataDomain().setMaxIdQualifierSize(100);
        
        final SelectQuery query = new SelectQuery(Artist.class);
        query.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY).setSemantics(PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS);

        int queriesCount = queryInterceptor.runWithQueryCounter(new UnitTestClosure() {

            public void execute() {
                context.performQuery(query);
            }
        });

        assertEquals(11, queriesCount);
    }

    public void testDisjointByIdPrefetch_Zero() throws Exception {
        insertData();
        runtime.getDataDomain().setMaxIdQualifierSize(0);

        final SelectQuery query = new SelectQuery(Artist.class);
        query.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY).setSemantics(PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS);

        int queriesCount = queryInterceptor.runWithQueryCounter(new UnitTestClosure() {

            public void execute() {
                context.performQuery(query);
            }
        });

        assertEquals(2, queriesCount);
    }

    public void testDisjointByIdPrefetch_Negative() throws Exception {
        insertData();
        runtime.getDataDomain().setMaxIdQualifierSize(-1);

        final SelectQuery query = new SelectQuery(Artist.class);
        query.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY).setSemantics(PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS);

        int queriesCount = queryInterceptor.runWithQueryCounter(new UnitTestClosure() {

            public void execute() {
                context.performQuery(query);
            }
        });

        assertEquals(2, queriesCount);
    }

    public void testIncrementalFaultList_Lower() throws Exception {
        insertData_OneBag_1000Boxes();

        runtime.getDataDomain().setMaxIdQualifierSize(50);

        final SelectQuery query = new SelectQuery(Painting.class);
        query.setPageSize(100);
        int queriesCount = queryInterceptor.runWithQueryCounter(new UnitTestClosure() {

            public void execute() {
                final List<Painting> boxes = context.performQuery(query);
                for (Painting box : boxes) {
                    box.getToArtist();
                }
            }
        });

        assertEquals(21, queriesCount);

        queriesCount = queryInterceptor.runWithQueryCounter(new UnitTestClosure() {

            public void execute() {
                final List<Painting> boxes = context.performQuery(query);
                List<Painting> tempList = new ArrayList<Painting>();
                tempList.addAll(boxes);
            }
        });

        assertEquals(21, queriesCount);
    }
    
    public void testIncrementalFaultList_Higher() throws Exception {
        insertData_OneBag_1000Boxes();

        runtime.getDataDomain().setMaxIdQualifierSize(1001);

        final SelectQuery query = new SelectQuery(Painting.class);
        query.setPageSize(100);
        int queriesCount = queryInterceptor.runWithQueryCounter(new UnitTestClosure() {

            public void execute() {
                final List<Painting> boxes = context.performQuery(query);
                for (Painting box : boxes) {
                    box.getToArtist();
                }
            }
        });

        assertEquals(11, queriesCount);

        queriesCount = queryInterceptor.runWithQueryCounter(new UnitTestClosure() {

            public void execute() {
                final List<Painting> boxes = context.performQuery(query);
                List<Painting> tempList = new ArrayList<Painting>();
                tempList.addAll(boxes);
            }
        });

        assertEquals(2, queriesCount);
    }

    public void testIncrementalFaultList_Zero() throws Exception {
        insertData_OneBag_1000Boxes();

        runtime.getDataDomain().setMaxIdQualifierSize(0);

        final SelectQuery query = new SelectQuery(Painting.class);
        query.setPageSize(100);
        int queriesCount = queryInterceptor.runWithQueryCounter(new UnitTestClosure() {

            public void execute() {
                final List<Painting> boxes = context.performQuery(query);
                List<Painting> tempList = new ArrayList<Painting>();
                tempList.addAll(boxes);
            }
        });

        assertEquals(2, queriesCount);
    }

    public void testIncrementalFaultList_Negative() throws Exception {
        insertData_OneBag_1000Boxes();

        runtime.getDataDomain().setMaxIdQualifierSize(-1);

        final SelectQuery query = new SelectQuery(Painting.class);
        query.setPageSize(100);
        int queriesCount = queryInterceptor.runWithQueryCounter(new UnitTestClosure() {

            public void execute() {
                final List<Painting> boxes = context.performQuery(query);
                List<Painting> tempList = new ArrayList<Painting>();
                tempList.addAll(boxes);
            }
        });

        assertEquals(2, queriesCount);
    }
}

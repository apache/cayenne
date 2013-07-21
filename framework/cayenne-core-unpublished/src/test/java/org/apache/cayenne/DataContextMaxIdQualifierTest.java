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

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Bag;
import org.apache.cayenne.testdo.testmap.Box;
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

    protected TableHelper tBag;
    protected TableHelper tBox;

    @Override
    protected void setUpAfterInjection() throws Exception {

        dbHelper.deleteAll("BALL");
        dbHelper.deleteAll("BOX_THING");
        dbHelper.deleteAll("THING");
        dbHelper.deleteAll("BOX_INFO");
        dbHelper.deleteAll("BOX");
        dbHelper.deleteAll("BAG");

        tBag = new TableHelper(dbHelper, "BAG");
        tBag.setColumns("ID", "NAME");

        tBox = new TableHelper(dbHelper, "BOX");
        tBox.setColumns("ID", "BAG_ID", "NAME");
    }

    public void testDisjointByIdPrefetch() throws Exception {

        for (int i = 0; i < 1000; i++) {
            tBag.insert(i + 1, "bag" + (i + 1));
            tBox.insert(i + 1, i + 1, "box" + (i + 1));
        }

        runtime.getDataDomain().setMaxIdQualifierSize(100);

        final SelectQuery query = new SelectQuery(Bag.class);
        query.addPrefetch(Bag.BOXES_PROPERTY).setSemantics(
                PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS);

        int queriesCount = queryInterceptor.runWithQueryCounter(new UnitTestClosure() {

            public void execute() {
                context.performQuery(query);
            }
        });

        assertEquals(11, queriesCount);
    }

    public void testDisjointByIdPrefetch_Zero() throws Exception {
        for (int i = 0; i < 1000; i++) {
            tBag.insert(i + 1, "bag" + (i + 1));
            tBox.insert(i + 1, i + 1, "box" + (i + 1));
        }

        runtime.getDataDomain().setMaxIdQualifierSize(0);
        
        final SelectQuery query = new SelectQuery(Bag.class);
        query.addPrefetch(Bag.BOXES_PROPERTY).setSemantics(
                PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS);

        int queriesCount = queryInterceptor.runWithQueryCounter(new UnitTestClosure() {

            public void execute() {
                context.performQuery(query);
            }
        });

        assertEquals(2, queriesCount);
    }
    
    public void testDisjointByIdPrefetch_Negative() throws Exception {
        for (int i = 0; i < 1000; i++) {
            tBag.insert(i + 1, "bag" + (i + 1));
            tBox.insert(i + 1, i + 1, "box" + (i + 1));
        }

        runtime.getDataDomain().setMaxIdQualifierSize(-1);
        
        final SelectQuery query = new SelectQuery(Bag.class);
        query.addPrefetch(Bag.BOXES_PROPERTY).setSemantics(
                PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS);

        int queriesCount = queryInterceptor.runWithQueryCounter(new UnitTestClosure() {

            public void execute() {
                context.performQuery(query);
            }
        });

        assertEquals(2, queriesCount);
    }


    public void testIncrementalFaultList_Lower() throws Exception {
        tBag.insert(1, "bag1");
        for (int i = 0; i < 1000; i++) {
            tBox.insert(i + 1, 1, "box" + (i + 1));
        }

        runtime.getDataDomain().setMaxIdQualifierSize(50);

        final SelectQuery query = new SelectQuery(Box.class);
        query.setPageSize(100);
        int queriesCount = queryInterceptor.runWithQueryCounter(new UnitTestClosure() {

            public void execute() {
                final List<Box> boxes = context.performQuery(query);
                for(Box box : boxes) {
                    box.getBag();
                }
            }
        });

        assertEquals(21, queriesCount);
        
        queriesCount = queryInterceptor.runWithQueryCounter(new UnitTestClosure() {

            public void execute() {
                final List<Box> boxes = context.performQuery(query);
                List<Box> tempList = new ArrayList<Box>();
                tempList.addAll(boxes);
            }
        });

        assertEquals(21, queriesCount);
    }
    
    public void testIncrementalFaultList_Higher() throws Exception {
        tBag.insert(1, "bag1");
        for (int i = 0; i < 1000; i++) {
            tBox.insert(i + 1, 1, "box" + (i + 1));
        }

        runtime.getDataDomain().setMaxIdQualifierSize(1001);

        final SelectQuery query = new SelectQuery(Box.class);
        query.setPageSize(100);
        int queriesCount = queryInterceptor.runWithQueryCounter(new UnitTestClosure() {

            public void execute() {
                final List<Box> boxes = context.performQuery(query);
                for(Box box : boxes) {
                    box.getBag();
                }
            }
        });

        assertEquals(11, queriesCount);
        
        queriesCount = queryInterceptor.runWithQueryCounter(new UnitTestClosure() {

            public void execute() {
                final List<Box> boxes = context.performQuery(query);
                List<Box> tempList = new ArrayList<Box>();
                tempList.addAll(boxes);
            }
        });

        assertEquals(2, queriesCount);
    }
    
    public void testIncrementalFaultList_Zero() throws Exception {
        tBag.insert(1, "bag1");
        for (int i = 0; i < 1000; i++) {
            tBox.insert(i + 1, 1, "box" + (i + 1));
        }

        runtime.getDataDomain().setMaxIdQualifierSize(0);

        final SelectQuery query = new SelectQuery(Box.class);
        query.setPageSize(100);
        int queriesCount = queryInterceptor.runWithQueryCounter(new UnitTestClosure() {

            public void execute() {
                final List<Box> boxes = context.performQuery(query);
                List<Box> tempList = new ArrayList<Box>();
                tempList.addAll(boxes);
            }
        });

        assertEquals(2, queriesCount);
    }
    
    public void testIncrementalFaultList_Negative() throws Exception {
        tBag.insert(1, "bag1");
        for (int i = 0; i < 1000; i++) {
            tBox.insert(i + 1, 1, "box" + (i + 1));
        }

        runtime.getDataDomain().setMaxIdQualifierSize(-1);

        final SelectQuery query = new SelectQuery(Box.class);
        query.setPageSize(100);
        int queriesCount = queryInterceptor.runWithQueryCounter(new UnitTestClosure() {

            public void execute() {
                final List<Box> boxes = context.performQuery(query);
                List<Box> tempList = new ArrayList<Box>();
                tempList.addAll(boxes);
            }
        });

        assertEquals(2, queriesCount);
    }
}

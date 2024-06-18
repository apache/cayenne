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

import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.testdo.testmap.PaintingInfo;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * A test case for CAY-788.
 */
@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextPrefetchExtras1IT extends RuntimeCase {

    @Inject
    protected ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    protected void createDataSet() throws Exception {

        TableHelper tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns("PAINTING_ID", "PAINTING_TITLE");

        TableHelper tPaintingInfo = new TableHelper(dbHelper, "PAINTING_INFO");
        tPaintingInfo.setColumns("PAINTING_ID", "TEXT_REVIEW");

        for (int i = 1; i <= 10; i++) {
            tPainting.insert(i, "P" + i);
            tPaintingInfo.insert(i, "Review #" + i);
        }
    }

    @Test
    public void testPrefetchToOne() throws Exception {
        createDataSet();

        ObjectSelect<Painting> query = ObjectSelect.query(Painting.class)
                .prefetch(Painting.TO_PAINTING_INFO.disjoint());

        List<Painting> objects = query.select(context);
        assertFalse(objects.isEmpty());
        for (Painting p : objects) {
            PaintingInfo pi = p.getToPaintingInfo();
            assertEquals(PersistenceState.COMMITTED, p.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, pi.getPersistenceState());
        }
    }

}

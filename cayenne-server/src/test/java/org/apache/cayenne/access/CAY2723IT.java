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

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.dba.JdbcPkGenerator;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.testdo.testmap.PaintingInfo;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class CAY2723IT extends ServerCase {
    @Inject
    private DataContext context;

    @Inject
    private DataChannelInterceptor queryInterceptor;

    /**
     * need to run this to ensure that PK generation doesn't affect main test
     */
    @Before
    public void warmup() {
        Painting painting = context.newObject(Painting.class);
        painting.setPaintingTitle("test_warmup");
        context.commitChanges();
        int pk = Cayenne.intPKForObject(painting);

        // push PK sequence one more time, to make sure the test wouldn't fail because of PK generation queries.
        if(pk % JdbcPkGenerator.DEFAULT_PK_CACHE_SIZE == 0) {
            painting = context.newObject(Painting.class);
            painting.setPaintingTitle("test_warmup_2");
            context.commitChanges();
        }
    }

    @Test
    public void phantomToDepPKUpdate() {
        Painting painting = context.newObject(Painting.class);
        painting.setPaintingTitle("test_p_123");

        PaintingInfo paintingInfo = context.newObject(PaintingInfo.class);
        paintingInfo.setTextReview("test_a_123");

        painting.setToPaintingInfo(paintingInfo);
        painting.setToPaintingInfo(null);

        context.deleteObject(paintingInfo);

        // here should be only single insert of the painting object
        int queryCounter = queryInterceptor.runWithQueryCounter(() -> context.commitChanges());
        assertEquals(1, queryCounter);
    }
}

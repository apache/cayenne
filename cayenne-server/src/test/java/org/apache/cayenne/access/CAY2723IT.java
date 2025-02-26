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

import org.apache.cayenne.dba.JdbcPkGenerator;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.testdo.toone.TooneDep;
import org.apache.cayenne.testdo.toone.TooneMaster;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@UseServerRuntime(CayenneProjects.TOONE_PROJECT)
public class CAY2723IT extends ServerCase {
    @Inject
    private DataContext context;

    @Inject
    private DataChannelInterceptor queryInterceptor;

    @Test
    public void phantomToDepPKUpdate() {
        // try to trigger PK generator. so it wouldn't random fail the actual test
        for (int i = 0; i < JdbcPkGenerator.DEFAULT_PK_CACHE_SIZE; i++) {
            int queryCounter = queryInterceptor.runWithQueryCounter(() -> {
                context.newObject(TooneMaster.class);
                context.commitChanges();
            });
            // PK generator triggered, we are ready
            if (queryCounter > 1) {
                break;
            }
        }

        TooneMaster master = context.newObject(TooneMaster.class);
        TooneDep dep = context.newObject(TooneDep.class);
        master.setToDependent(dep);
        master.setToDependent(null);

        context.deleteObject(dep);

        // here should be only single insert of the painting object
        int queryCounter = queryInterceptor.runWithQueryCounter(() -> context.commitChanges());
        assertEquals(1, queryCounter);
    }
}

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

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.binary_pk.BinaryPKTest1;
import org.apache.cayenne.testdo.binary_pk.BinaryPKTest2;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@UseCayenneRuntime(CayenneProjects.BINARY_PK_PROJECT)
public class DataContextBinaryPKIT extends RuntimeCase {

    @Inject
    private DataContext context;

    @Inject
    private DataContext context1;

    @Inject
    private UnitDbAdapter accessStackAdapter;

    @Test
    public void testInsertBinaryPK() throws Exception {
        if (accessStackAdapter.supportsBinaryPK()) {

            BinaryPKTest1 master = (BinaryPKTest1) context.newObject("BinaryPKTest1");
            master.setName("master1");

            BinaryPKTest2 detail = (BinaryPKTest2) context.newObject("BinaryPKTest2");
            detail.setDetailName("detail2");

            master.addToBinaryPKDetails(detail);

            context.commitChanges();
        }
    }

    @Test
    public void testFetchRelationshipBinaryPK() throws Exception {
        if (accessStackAdapter.supportsBinaryPK()) {

            BinaryPKTest1 master = (BinaryPKTest1) context.newObject("BinaryPKTest1");
            master.setName("master1");

            BinaryPKTest2 detail = (BinaryPKTest2) context.newObject("BinaryPKTest2");
            detail.setDetailName("detail2");

            master.addToBinaryPKDetails(detail);

            context.commitChanges();
            context.invalidateObjects(master, detail);

            BinaryPKTest2 fetchedDetail = ObjectSelect.query(BinaryPKTest2.class).selectFirst(context);
            assertNotNull(fetchedDetail.readPropertyDirectly("toBinaryPKMaster"));

            BinaryPKTest1 fetchedMaster = fetchedDetail.getToBinaryPKMaster();
            assertNotNull(fetchedMaster);
            assertEquals(PersistenceState.HOLLOW, fetchedMaster.getPersistenceState());
            assertEquals("master1", fetchedMaster.getName());
        }
    }
}

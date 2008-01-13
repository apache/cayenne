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

package org.apache.cayenne.access;

import org.apache.cayenne.testdo.db1.CrossdbM1E1;
import org.apache.cayenne.testdo.db2.CrossdbM2E1;
import org.apache.cayenne.testdo.db2.CrossdbM2E2;
import org.apache.cayenne.unit.MultiNodeCase;

public class DataContextCrossDBTest extends MultiNodeCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testMultiDBUpdate() {

        // for now testing that no exceptions are thrown... wouldn't hurt to check the
        // data as well???

        DataContext context = createDataContext();

        // insert
        CrossdbM1E1 o1 = context.newObject(CrossdbM1E1.class);
        o1.setName("o1");

        CrossdbM2E1 o2 = context.newObject(CrossdbM2E1.class);
        o2.setName("o2");

        CrossdbM2E2 o3 = context.newObject(CrossdbM2E2.class);
        o3.setName("o3");

        o3.setToM1E1(o1);
        o3.setToM2E1(o2);
        context.commitChanges();

        // update
        CrossdbM1E1 o11 = context.newObject(CrossdbM1E1.class);
        o11.setName("o11");
        o3.setToM1E1(o11);
        context.commitChanges();

        // update with existing

        o3.setToM1E1(o1);
        context.commitChanges();
    }
}

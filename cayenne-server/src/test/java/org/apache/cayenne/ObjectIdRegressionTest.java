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

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class ObjectIdRegressionTest {

    @Test
    public void testIdPool() throws Exception {
        // testing uniqueness of a sequence of ObjectIds generated quickly one after the other...

        int size = 100000;

        ObjectId.of("Artist");
        ObjectId[] pool = new ObjectId[size];

        long t0 = System.currentTimeMillis();
        // fill in
        for (int i = 0; i < size; i++) {
            pool[i] = ObjectId.of("Artist");
        }

        long t1 = System.currentTimeMillis();

        assertTrue("This machine is too fast to run such test!", t1 - t0 > 1);

        Set<ObjectId> idSet = new HashSet<>();
        for (int i = 0; i < size; i++) {
            assertTrue("Failed to generate unique id #" + i, idSet.add(pool[i]));
        }
    }
}

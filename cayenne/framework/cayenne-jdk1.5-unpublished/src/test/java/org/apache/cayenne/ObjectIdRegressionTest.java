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

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

public class ObjectIdRegressionTest extends TestCase {

    // public void testX() {
    // for (int i = 0; i < 10000; i++) {
    // byte[] bytes = IDUtil.pseudoUniqueByteSequence8();
    // StringBuffer buffer = new StringBuffer(16);
    // for(int j = 0; j < 8; j++) {
    // IDUtil.appendFormattedByte(buffer, bytes[j]);
    // }
    //            
    // System.out.println(buffer);
    // }
    // }

    public void testIdPool() throws Exception {
        // testing uniqueness of a sequence of ObjectIds generated quickly one after the
        // other...

        int size = 100000;

        new ObjectId("Artist");
        Object[] pool = new Object[size];

        long t0 = System.currentTimeMillis();
        // fill in
        for (int i = 0; i < size; i++) {
            pool[i] = new ObjectId("Artist");
        }

        long t1 = System.currentTimeMillis();

        assertTrue("This machine is too fast to run such test!", t1 - t0 > 1);

        Set idSet = new HashSet();
        for (int i = 0; i < size; i++) {
            assertTrue("Failed to generate unique id #" + i, idSet.add(pool[i]));
        }
    }
}

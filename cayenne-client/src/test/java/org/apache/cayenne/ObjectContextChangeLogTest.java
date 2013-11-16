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

import junit.framework.TestCase;

import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.NodeCreateOperation;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.remote.hessian.service.HessianUtil;

public class ObjectContextChangeLogTest extends TestCase {

    public void testGetDiffsSerializableWithHessian() throws Exception {
        ObjectContextChangeLog recorder = new ObjectContextChangeLog();

        // id must be a serializable object
        recorder.addOperation(new NodeCreateOperation("id-string"));
        CompoundDiff diff = (CompoundDiff) recorder.getDiffs();

        Object clone = HessianUtil.cloneViaClientServerSerialization(diff, new EntityResolver());
        assertNotNull(clone);
        assertTrue(clone instanceof CompoundDiff);

        CompoundDiff d1 = (CompoundDiff) clone;
        assertEquals(1, d1.getDiffs().size());
    }
}

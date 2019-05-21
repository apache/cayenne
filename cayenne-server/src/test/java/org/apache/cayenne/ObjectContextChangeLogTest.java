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

import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.NodeCreateOperation;
import org.apache.cayenne.util.Util;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ObjectContextChangeLogTest {

    @Test
    public void testReset() {
        ObjectContextChangeLog recorder = new ObjectContextChangeLog();
        assertNotNull(recorder.getDiffs());
        assertTrue(recorder.getDiffs().isNoop());

        recorder.addOperation(new NodeCreateOperation(new Object()));
        assertNotNull(recorder.getDiffs());
        assertFalse(recorder.getDiffs().isNoop());

        recorder.reset();
        assertNotNull(recorder.getDiffs());
        assertTrue(recorder.getDiffs().isNoop());

        // now test that a diff stored before "clear" is not affected by 'clear'
        recorder.addOperation(new NodeCreateOperation(new Object()));
        GraphDiff diff = recorder.getDiffs();
        assertFalse(diff.isNoop());

        recorder.reset();
        assertFalse(diff.isNoop());
    }

    @Test
    public void testGetDiffs() {
        // assert that after returning, the diffs array won't get modified by
        // operation
        // recorder
        ObjectContextChangeLog recorder = new ObjectContextChangeLog();
        recorder.addOperation(new NodeCreateOperation(new Object()));
        CompoundDiff diff = (CompoundDiff) recorder.getDiffs();
        assertEquals(1, diff.getDiffs().size());

        recorder.addOperation(new NodeCreateOperation(new Object()));
        assertEquals(1, diff.getDiffs().size());

        CompoundDiff diff2 = (CompoundDiff) recorder.getDiffs();
        assertEquals(2, diff2.getDiffs().size());
    }

    @Test
    public void testGetDiffsSerializable() throws Exception {
        ObjectContextChangeLog recorder = new ObjectContextChangeLog();
        recorder.addOperation(new NodeCreateOperation(ObjectId.of("test")));
        CompoundDiff diff = (CompoundDiff) recorder.getDiffs();

        Object clone = Util.cloneViaSerialization(diff);
        assertNotNull(clone);
        assertTrue(clone instanceof CompoundDiff);

        CompoundDiff d1 = (CompoundDiff) clone;
        assertEquals(1, d1.getDiffs().size());
    }

}

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

package org.apache.cayenne.unit;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.apache.cayenne.MockDataChannel;
import org.apache.cayenne.query.NamedQuery;
import org.apache.cayenne.remote.QueryMessage;

public class TestLocalConnectionTest extends TestCase {

    public void testBlockUnblock() {

        UnitLocalConnection c = new UnitLocalConnection(new MockDataChannel());
        assertFalse(c.isBlockingMessages());

        c.setBlockingMessages(true);
        assertTrue(c.isBlockingMessages());

        try {
            c.sendMessage(new QueryMessage(new NamedQuery("dummy")));
        }
        catch (AssertionFailedError e) {
            // expected...
            return;
        }
        fail("Didn't block message...");
    }

}

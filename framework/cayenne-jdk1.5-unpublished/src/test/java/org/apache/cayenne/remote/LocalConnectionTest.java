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

package org.apache.cayenne.remote;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.MockDataChannel;
import org.apache.cayenne.remote.service.LocalConnection;

import junit.framework.TestCase;

/**
 */
public class LocalConnectionTest extends TestCase {

    public void testConstructors() {
        DataChannel handler1 = new MockDataChannel();
        LocalConnection connector1 = new LocalConnection(handler1);
        assertFalse(connector1.isSerializingMessages());
        assertSame(handler1, connector1.getChannel());

        DataChannel handler2 = new MockDataChannel();
        LocalConnection connector2 = new LocalConnection(
                handler2,
                LocalConnection.JAVA_SERIALIZATION);
        assertTrue(connector2.isSerializingMessages());
        assertSame(handler2, connector2.getChannel());
    }
}

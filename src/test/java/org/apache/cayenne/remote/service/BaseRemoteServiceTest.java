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
package org.apache.cayenne.remote.service;

import junit.framework.TestCase;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.remote.QueryMessage;
import org.apache.cayenne.remote.RemoteSession;
import org.apache.cayenne.util.Util;

public class BaseRemoteServiceTest extends TestCase {

    public void testProcessMessageExceptionSerializability() throws Throwable {

        BaseRemoteService handler = new BaseRemoteService() {

            @Override
            protected ServerSession createServerSession() {
                return new ServerSession(new RemoteSession("a"), null);
            }

            @Override
            protected ServerSession createServerSession(String name) {
                return createServerSession();
            }

            @Override
            protected ServerSession getServerSession() {
                return createServerSession();
            }
        };

        try {
            handler.processMessage(new QueryMessage(null) {

                @Override
                public Query getQuery() {
                    // serializable exception thrown
                    throw new CayenneRuntimeException();
                }
            });

            fail("Expected to throw");
        }
        catch (Exception ex) {
            Util.cloneViaSerialization(ex);
        }

        try {
            handler.processMessage(new QueryMessage(null) {

                @Override
                public Query getQuery() {
                    // non-serializable exception thrown
                    throw new MockUnserializableException();
                }
            });

            fail("Expected to throw");
        }
        catch (Exception ex) {
            Util.cloneViaSerialization(ex);
        }
    }
}

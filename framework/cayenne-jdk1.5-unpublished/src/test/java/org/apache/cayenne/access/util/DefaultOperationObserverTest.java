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

package org.apache.cayenne.access.util;

import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DefaultOperationObserverTest extends ServerCase {

    public void testHasExceptions1() throws Exception {
        DefaultOperationObserver observer = new DefaultOperationObserver();
        assertFalse(observer.hasExceptions());
        observer.nextGlobalException(new Exception());
        assertTrue(observer.hasExceptions());
    }

    public void testHasExceptions2() throws Exception {
        DefaultOperationObserver observer = new DefaultOperationObserver();
        assertFalse(observer.hasExceptions());
        observer.nextQueryException(new SelectQuery(), new Exception());
        assertTrue(observer.hasExceptions());
    }
}

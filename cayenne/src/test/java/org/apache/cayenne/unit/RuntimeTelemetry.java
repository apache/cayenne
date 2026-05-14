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
package org.apache.cayenne.unit;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.UnitTestDomain;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.unit.runtime.RuntimeCaseDataNode;

class RuntimeTelemetry {

    public static void runWithQueriesBlocked(CayenneRuntime runtime, Runnable task) {

        UnitTestDomain channel = (UnitTestDomain) runtime.getChannel();
        channel.setBlockingQueries(true);
        try {
            task.run();
        } finally {
            channel.setBlockingQueries(false);
        }
    }

    public static int runWithQueryCounter(CayenneRuntime runtime, Runnable task) {
        DataDomain channel = (DataDomain) runtime.getChannel();
        RuntimeCaseDataNode node = (RuntimeCaseDataNode) channel.getDataNodes().iterator().next();

        int start = node.getQueriesCount();
        int end;
        try {
            task.run();
        } finally {
            end = node.getQueriesCount();
        }
        return end - start;
    }
}

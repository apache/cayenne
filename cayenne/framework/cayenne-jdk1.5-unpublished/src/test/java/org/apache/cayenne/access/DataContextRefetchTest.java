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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.unit.CayenneCase;

/**
 * @deprecated since 3.0
 */
public class DataContextRefetchTest extends CayenneCase {

    public void testRefetchTempId() {
        MockDataNode engine = MockDataNode.interceptNode(getDomain(), getNode());

        try {
            DataContext context = getDomain().createDataContext();
            ObjectId tempID = new ObjectId("Artist");

            try {
                context.refetchObject(tempID);
                fail("Refetching temp ID must have generated an error.");
            }
            catch (CayenneRuntimeException ex) {
                // expected ... but check that no queries were run
                assertEquals("Refetching temp id correctly failed, "
                        + "but DataContext shouldn't have run a query", 0, engine
                        .getRunCount());
            }
        }
        finally {
            engine.stopInterceptNode();
        }
    }

}

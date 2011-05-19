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

import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable2;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

/**
 * This is a test primarily for CAY-1118
 */
@UseServerRuntime(ClientCase.MULTI_TIER_PROJECT)
public class RelationshipChangeTest extends RemoteCayenneCase {

    public void testNullify() {
        ClientMtTable1 o1 = clientContext.newObject(ClientMtTable1.class);
        ClientMtTable2 o2 = clientContext.newObject(ClientMtTable2.class);
        
        o2.setTable1(o1);

        assertEquals(1, o1.getTable2Array().size());
        clientContext.commitChanges();

        o2.setTable1(null);
        assertEquals(0, o1.getTable2Array().size());
    }
    
    public void testChange() {
        ClientMtTable1 o1 = clientContext.newObject(ClientMtTable1.class);
        ClientMtTable2 o2 = clientContext.newObject(ClientMtTable2.class);
        
        ClientMtTable1 o3 = clientContext.newObject(ClientMtTable1.class);
        
        o2.setTable1(o1);

        assertEquals(1, o1.getTable2Array().size());
        clientContext.commitChanges();

        o2.setTable1(o3);
        assertEquals(0, o1.getTable2Array().size());
        assertEquals(1, o3.getTable2Array().size());
    }
}

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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.rop.client.ClientRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ClientCase.MULTI_TIER_PROJECT)
public class NestedObjectContextRollbackTest extends RemoteCayenneCase {
    
    @Inject
    private ClientRuntime runtime;

    public void testRollbackChanges() {
        ObjectContext child1 = runtime.newContext(clientContext);
        
        assertFalse(clientContext.hasChanges());
        assertFalse(child1.hasChanges());
        
        clientContext.newObject(ClientMtTable1.class);
        child1.newObject(ClientMtTable1.class);
        
        assertTrue(clientContext.hasChanges());
        assertTrue(child1.hasChanges());
        
        child1.rollbackChanges();
        assertFalse(clientContext.hasChanges());
        assertFalse(child1.hasChanges());
        
        clientContext.rollbackChanges();
    }
    
    public void testRollbackChangesLocally() {
        ObjectContext child1 = runtime.newContext(clientContext);
        
        assertFalse(clientContext.hasChanges());
        assertFalse(child1.hasChanges());
        
        clientContext.newObject(ClientMtTable1.class);
        child1.newObject(ClientMtTable1.class);
        
        assertTrue(clientContext.hasChanges());
        assertTrue(child1.hasChanges());
        
        child1.rollbackChangesLocally();
        assertTrue(clientContext.hasChanges());
        assertFalse(child1.hasChanges());
        
        clientContext.rollbackChanges();
    }
}

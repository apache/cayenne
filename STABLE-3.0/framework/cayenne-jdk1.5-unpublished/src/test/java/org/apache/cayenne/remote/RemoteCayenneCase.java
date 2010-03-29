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

import org.apache.cayenne.CayenneContext;
import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.remote.service.LocalConnection;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CayenneResources;
import org.apache.cayenne.unit.UnitLocalConnection;

/**
 * JUnit case to test ROP functionality
 */
public abstract class RemoteCayenneCase extends CayenneCase {
    protected CayenneContext context;
    
    protected DataContext parentDataContext;
    
    /**
     * Used serialization policy. Per CAY-979 we're testing on all policies
     */
    protected int serializationPolicy;
    
    @Override
    public void runBare() throws Throwable {
        serializationPolicy = LocalConnection.HESSIAN_SERIALIZATION;
        super.runBare();
        serializationPolicy = LocalConnection.JAVA_SERIALIZATION;
        super.runBare();
        serializationPolicy = LocalConnection.NO_SERIALIZATION;
        super.runBare();
    }
    
    @Override
    public void setUp() throws Exception {
        parentDataContext = createDataContext();
        context = createROPContext();
    }
    
    protected CayenneContext createROPContext() {
        ClientServerChannel clientServerChannel = new ClientServerChannel(parentDataContext);
        UnitLocalConnection connection = new UnitLocalConnection(
                clientServerChannel,
                serializationPolicy);
        ClientChannel channel = new ClientChannel(connection);
        return new CayenneContext(channel, true, true);
    }
    
    @Override
    protected AccessStack buildAccessStack() {
        return CayenneResources.getResources().getAccessStack(MULTI_TIER_ACCESS_STACK);
    }
}

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
package org.apache.cayenne.itest.cpa;

import org.apache.cayenne.CayenneContext;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.remote.ClientChannel;
import org.apache.cayenne.remote.service.LocalConnection;

public class CPAContextCase extends CPATestCase {

    private DataContext context;
    private ObjectContext clientContext;

    public ObjectContext getContext() {
        if (context == null) {
            this.context = ItestSetup.getInstance().createDataContext();
        }
        return context;
    }

    public ObjectContext getContext(boolean reset) {
        if (reset) {
            this.context = null;
        }

        return getContext();
    }

    protected ObjectContext getClientContext() {
        if (clientContext == null) {

            // create with this test case DataContext to allow callers to poke on the
            // server side as well as the client
            ClientServerChannel clientServerChannel = new ClientServerChannel(
                    (DataContext) getContext());
            LocalConnection connection = new LocalConnection(clientServerChannel);
            ClientChannel channel = new ClientChannel(connection);
            clientContext = new CayenneContext(channel);
        }

        return clientContext;
    }

    protected ObjectContext getClientContext(boolean reset) {
        if (reset) {
            clientContext = null;
        }

        return getClientContext();
    }
}

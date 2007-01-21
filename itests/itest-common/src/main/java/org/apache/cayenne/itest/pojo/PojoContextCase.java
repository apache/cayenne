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
package org.apache.cayenne.itest.pojo;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataContext;

public class PojoContextCase extends PojoTestCase {

    protected DataContext context;

    @Override
    protected void setUp() throws Exception {
        this.context = ItestSetup.getInstance().createDataContext();
    }

    public ObjectContext getContext() {
        return context;
    }

    /**
     * Ensure that context can't send any queries doesn the channel.
     */
    protected void blockContextQueries() {
        context.setChannel(new BlockingDataChannel(context.getChannel()));
    }

    protected void unblockContextQueries() {
        DataChannel channel = context.getChannel();
        if (channel instanceof BlockingDataChannel) {
            context.setChannel(((BlockingDataChannel) channel).getChannel());
        }
    }

    /**
     * Ensures that DataDomain can't send queries to the DB.
     */
    protected void blockDomainQueries() {
        ItestSetup.getInstance().getDataDomain().setTransactionDelegate(
                new BlockingTransactionDelegate());
    }

    protected void unblockDomainQueries() {
        ItestSetup.getInstance().getDataDomain().setTransactionDelegate(null);
    }
}

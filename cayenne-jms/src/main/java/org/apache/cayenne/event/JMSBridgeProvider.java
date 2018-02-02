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

package org.apache.cayenne.event;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataRowStore;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;

import java.util.Collections;
import java.util.Map;

public class JMSBridgeProvider implements Provider<EventBridge> {

    @Inject
    protected DataDomain dataDomain;

    @Inject(JMSModule.JMS_BRIDGE_PROPERTIES_MAP)
    Map<String, String> properties;

    @Override
    public EventBridge get() throws DIRuntimeException {
        EventSubject snapshotEventSubject = EventSubject.getSubject(DataRowStore.class, dataDomain.getName());

        return new JMSBridge(
                Collections.singleton(snapshotEventSubject),
                EventBridge.convertToExternalSubject(snapshotEventSubject),
                properties);
    }

}

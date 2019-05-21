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

package org.apache.cayenne.access;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.event.EventBridge;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.event.NoopEventBridge;

/**
 * A default implementation of {@link DataRowStoreFactory}
 *
 * @since 4.0
 */
public class DefaultDataRowStoreFactory implements DataRowStoreFactory {

    Provider<EventBridge> eventBridgeProvider;

    EventManager eventManager;

    RuntimeProperties properties;

    public DefaultDataRowStoreFactory(@Inject Provider<EventBridge> eventBridgeProvider,
                                      @Inject EventManager eventManager,
                                      @Inject RuntimeProperties properties) {
        this.eventBridgeProvider = eventBridgeProvider;
        this.eventManager = eventManager;
        this.properties = properties;
    }

    @Override
    public DataRowStore createDataRowStore(String name) throws DIRuntimeException {
        DataRowStore store = new DataRowStore(name, properties, eventManager);
        setUpEventBridge(store);
        return store;
    }

    private void setUpEventBridge(DataRowStore store) {
        try {
            EventBridge eventBridge = eventBridgeProvider.get();
            if (eventBridge instanceof NoopEventBridge) {
                return;
            }
            store.setEventBridge(eventBridge);
            store.startListeners();
        } catch (Exception ex) {
            throw new CayenneRuntimeException("Error initializing DataRowStore.", ex);
        }
    }
}
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
package org.apache.cayenne.configuration;

import org.apache.cayenne.access.DataRowStore;
import org.apache.cayenne.access.NoSyncObjectStore;
import org.apache.cayenne.access.ObjectMapRetainStrategy;
import org.apache.cayenne.access.ObjectStore;
import org.apache.cayenne.di.Inject;

/**
 * A default implementation of {@link ObjectStoreFactory} which makes decision to
 * turn {@link ObjectStore}'s syncing with parent {@link DataRowStore} on or off 
 * basing on {@link RuntimeProperties}.
 * 
 * @since 3.1
 */
public class DefaultObjectStoreFactory implements ObjectStoreFactory {
    
    @Inject
    protected RuntimeProperties runtimeProperties;
    
    @Inject
    protected ObjectMapRetainStrategy retainStrategy;
    
    public ObjectStore createObjectStore(DataRowStore dataRowCache) {
        boolean sync = runtimeProperties.getBoolean(Constants.SERVER_CONTEXTS_SYNC_PROPERTY, true);
        
        return sync ? new ObjectStore(dataRowCache, retainStrategy.createObjectMap()) 
                : new NoSyncObjectStore(dataRowCache, retainStrategy.createObjectMap());
    }
}

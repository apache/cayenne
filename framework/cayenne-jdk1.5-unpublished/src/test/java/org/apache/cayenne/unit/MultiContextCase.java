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


package org.apache.cayenne.unit;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.util.Util;

/**
 * Superclass of test cases requiring multiple DataContexts with 
 * the same parent DataDomain.
 * 
 */
public abstract class MultiContextCase extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    protected void fixSharedConfiguration() {
        // for context to deserialize properly, 
        // Configuration singleton must have the right default domain
        Configuration config = Configuration.getSharedConfiguration();
        if (getDomain() != config.getDomain()) {
            if (config.getDomain() != null) {
                config.removeDomain(config.getDomain().getName());
            }
            config.addDomain(getDomain());
        }
    }

    /**
     * Helper method to create a new DataContext with the ObjectStore
     * state being the mirror of the given context. This is done by
     * serializing/deserializing the DataContext.
     */
    protected DataContext mirrorDataContext(DataContext context) throws Exception {
        fixSharedConfiguration();

        DataContext mirror = (DataContext) Util.cloneViaSerialization(context);

        assertNotSame(context, mirror);
        assertNotSame(context.getObjectStore(), mirror.getObjectStore());

        if (context.isUsingSharedSnapshotCache()) {
            assertSame(
                context.getObjectStore().getDataRowCache(),
                mirror.getObjectStore().getDataRowCache());
        }
        else {
            assertNotSame(
                context.getObjectStore().getDataRowCache(),
                mirror.getObjectStore().getDataRowCache());
        }

        return mirror;
    }
}

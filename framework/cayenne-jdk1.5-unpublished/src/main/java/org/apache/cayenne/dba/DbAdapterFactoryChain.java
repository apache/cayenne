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

package org.apache.cayenne.dba;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A facade for a collection of DbAdapterFactories. Can be configured to auto-detect all
 * adapters known to Cayenne or can work with custom factories.
 * 
 * @since 1.2
 */
// TODO, Andrus 11/01/2005, how can custom adapters be auto-detected? I.e. is there a way
// to plug a custom factory into configuration loading process? Of course users can simply
// specify the adapter class in the modeler, so this may be a non-issue.
class DbAdapterFactoryChain implements DbAdapterFactory {

    List<DbAdapterFactory> factories;

    DbAdapterFactoryChain(Collection<DbAdapterFactory> factories) {
        this.factories = new ArrayList<DbAdapterFactory>(factories.size());
        this.factories.addAll(factories);
    }

    /**
     * Iterates through predicated factories, stopping when the first one returns non-null
     * DbAdapter. If none of the factories match the database, returns null.
     */
    public DbAdapter createAdapter(DatabaseMetaData md) throws SQLException {

        // match against configured predicated factories

        // iterate in reverse order to allow custom factories to take precedence over the
        // default ones configured in constructor
        for (int i = factories.size() - 1; i >= 0; i--) {
            DbAdapterFactory factory = factories.get(i);
            DbAdapter adapter = factory.createAdapter(md);

            if (adapter != null) {
                return adapter;
            }
        }

        return null;
    }

    /**
     * Removes all configured factories.
     */
    void clearFactories() {
        this.factories.clear();
    }

    /**
     * Adds a new DbAdapterFactory to the factory chain.
     */
    void addFactory(DbAdapterFactory factory) {
        this.factories.add(factory);
    }
}

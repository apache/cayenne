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

import org.apache.cayenne.dba.DbAdapter;

/**
 * A one-time initialization of a {@link DataNode}. Intended to postpone DB operations required for initial setup until
 * the DataNode accessed for the first time. Keeping it lazy allows the stack to start when the DB is not available.
 * A failed initialization is retried on the next use of the node.
 *
 * @since 6.0
 */
@FunctionalInterface
public interface DataNodeInitializer {

    /**
     * Initializes the node, returning its {@link DbAdapter}, either the one configured explicitly or the one detected
     * off the DataSource. The node caches the adapter and makes no further calls to the initializer.
     */
    DbAdapter init();
}

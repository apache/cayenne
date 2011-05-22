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

package org.apache.cayenne.access;

import java.util.Collection;

import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;

/**
 * Defines methods used to run Cayenne queries.
 */
public interface QueryEngine {

    /**
     * Executes a list of queries wrapping them in its own transaction. Results of
     * execution are passed to {@link OperationObserver}object via its callback methods.
     * 
     * @since 1.1 The signature has changed from List to Collection.
     */
    void performQueries(
            Collection<? extends Query> queries,
            OperationObserver resultConsumer);

    /**
     * Returns a resolver for this query engine that is capable of resolving between
     * classes, entity names, and obj/db entities
     */
    EntityResolver getEntityResolver();
}

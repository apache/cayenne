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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;

/**
 * Defines methods used to run Cayenne queries.
 * <p>
 * <i>For more information see <a href="../../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide. </a> </i>
 * </p>
 * 
 * @author Andrei Adamchik
 */
public interface QueryEngine {

    /**
     * Executes queries in the transactional context provided by the caller. It is
     * caller's responsibility to commit or rollback the Transaction and close any
     * connections that were added to it.
     * 
     * @since 1.1
     * @deprecated since 1.2 as thread-bound transactions are used.
     */
    public void performQueries(
            Collection queries,
            OperationObserver resultConsumer,
            Transaction transaction);

    /**
     * Executes a list of queries wrapping them in its own transaction. Results of
     * execution are passed to {@link OperationObserver}object via its callback methods.
     * 
     * @since 1.1 The signiture has changed from List to Collection.
     */
    public void performQueries(Collection queries, OperationObserver resultConsumer);

    /**
     * Returns a DataNode that should handle queries for all DataMap components.
     * 
     * @since 1.1
     * @deprecated since 1.2 not a part of the interface. Only DataDomain has meaningful
     *             implementation.
     */
    public DataNode lookupDataNode(DataMap dataMap);

    /**
     * Returns a resolver for this query engine that is capable of resolving between
     * classes, entity names, and obj/db entities
     */
    public EntityResolver getEntityResolver();

    /**
     * Returns a collection of DataMaps associated with this QueryEngine.
     * 
     * @deprecated since 1.2. Use 'getEntityResolver().getDataMaps()' instead.
     */
    public Collection getDataMaps();
}

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

package org.apache.cayenne.query;

import java.io.Serializable;

import org.apache.cayenne.access.QueryEngine;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.QueryDescriptor;

/**
 * Defines minimal API of a query descriptor that is executable via Cayenne.
 */
public interface Query extends Serializable {

    /**
     * Returns query runtime parameters. The method is called at various stages of the
     * execution by Cayenne access stack to retrieve query parameters. EntityResolver
     * instance is passed to this method, meaning that the query doesn't need to store
     * direct references to Cayenne mapping objects and can resolve them at runtime.
     * 
     * @since 1.2
     */
    QueryMetadata getMetaData(EntityResolver resolver);

    /**
     * A callback method invoked by Cayenne during the routing phase of the query
     * execution. Mapping of DataNodes is provided by QueryRouter. Query should use a
     * {@link QueryRouter#route(QueryEngine, Query, Query)} callback method to route
     * itself. Query can create one or more substitute queries or even provide its own
     * QueryEngine to execute itself.
     * 
     * @since 1.2
     */
    void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery);

    /**
     * A callback method invoked by Cayenne during the final execution phase of the query
     * run. A concrete query implementation is given a chance to decide how it should be
     * handled. Implementors can pick an appropriate method of the SQLActionVisitor to
     * handle itself, create a custom SQLAction of its own, or substitute itself with
     * another query that should be used for SQLAction construction.
     * 
     * @since 1.2
     */
    SQLAction createSQLAction(SQLActionVisitor visitor);
}

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

package org.apache.cayenne.query;

import org.apache.cayenne.access.QueryEngine;
import org.apache.cayenne.map.DataMap;

/**
 * An interface used by Queries to route themselves to an appropriate QueryEngine. As of
 * 1.2 QueryRouter only supports routing by DataMap.
 * 
 * @since 1.2
 */
public interface QueryRouter {

    /**
     * A callback method that allows a query to set its preferred engine during the
     * routing phase. It allows query to further customize its routing, e.g. it is
     * possible to implement query chains that pass multiple queries for execution.
     * 
     * @param engine engine to use for query execution
     * @param query A query to execute.
     * @param substitutedQuery a query that was substituted for "query". Results must be
     *            mapped back to substituted query.
     */
    void route(QueryEngine engine, Query query, Query substitutedQuery);
    
    /**
     * Returns a QueryEngine for a given name. If the name is null, a default
     * QueryEngine is returned. If there's no default engine, an exception is
     * thrown.
     * 
     * @since 3.2
     */
    QueryEngine engineForName(String name);

    /**
     * Returns a QueryEngine that is configured to handle a given DataMap.
     * 
     * @throws org.apache.cayenne.CayenneRuntimeException if an engine can't be found.
     * @throws NullPointerException if a map parameter is null.
     */
    QueryEngine engineForDataMap(DataMap map);
}

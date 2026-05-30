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

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.map.DataMap;

/**
 * An interface used by Queries to route themselves to an appropriate DataNode. As of
 * 1.2 QueryRouter only supports routing by DataMap.
 *
 * @since 1.2
 */
public interface QueryRouter {

    /**
     * A callback method that allows a query to set its preferred node during the
     * routing phase. It allows query to further customize its routing, e.g. it is
     * possible to implement query chains that pass multiple queries for execution.
     *
     * @param node             node to use for query execution
     * @param query            A query to execute.
     * @param substitutedQuery a query that was substituted for "query". Results must be
     *                         mapped back to substituted query.
     */
    void route(DataNode node, Query query, Query substitutedQuery);

    /**
     * Returns a DataNode for a given name. If the name is null, a default
     * DataNode is returned. If there's no default node, an exception is
     * thrown.
     *
     * @since 5.0
     */
    DataNode nodeForName(String name);

    /**
     * Returns a DataNode that is configured to handle a given DataMap.
     *
     * @since 5.0
     */
    DataNode nodeForDataMap(DataMap map);

    /**
     * @since 4.0
     * @deprecated renamed to {@link #nodeForName(String)}.
     */
    @Deprecated(since = "5.0", forRemoval = true)
    default DataNode engineForName(String name) {
        return nodeForName(name);
    }

    /**
     * @deprecated renamed to {@link #nodeForDataMap(DataMap)}.
     */
    @Deprecated(since = "5.0", forRemoval = true)
    default DataNode engineForDataMap(DataMap map) {
        return nodeForDataMap(map);
    }
}

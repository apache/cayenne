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

package org.apache.cayenne.remote;

import org.apache.cayenne.query.Query;

/**
 * A message passed to a DataChannel to request a query execution with result returned as
 * QueryResponse.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class QueryMessage implements ClientMessage {

    protected Query query;

    // for hessian serialization
    private QueryMessage() {

    }

    public QueryMessage(Query query) {
        this.query = query;
    }

    public Query getQuery() {
        return query;
    }

    /**
     * Returns a description of the type of message. In this case always "Query".
     */
    public String toString() {
        return "Query";
    }
}

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

import java.util.Map;

/**
 * Defines a query that can serve as a template for other queries. ParameterizedQuery
 * interface is used mainly in DataContext convenience methods, simplifying execution of
 * the mapped queries.
 * 
 * @since 1.1
 */
public interface ParameterizedQuery extends Query {

    /**
     * Creates a new query based on current query as a template, and using a Map of named
     * parameters. In case of select queries, it is up to the implementing query to name
     * the new query to avoid cache key conflicts.
     */
    Query createQuery(Map<String, ?> parameters);
}

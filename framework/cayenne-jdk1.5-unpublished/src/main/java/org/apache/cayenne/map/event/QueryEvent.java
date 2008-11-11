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

package org.apache.cayenne.map.event;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.query.Query;

/**
 * An event generated when a Query object is added to a DataMap, 
 * removed from a DataMap, or changed within a DataMap.
 * 
 * @since 1.1
 */
public class QueryEvent extends MapEvent {
    protected Query query;
    
    /**
     * Data map containing the query
     */
    protected DataMap map;

    public QueryEvent(Object source, Query query) {
        super(source);
        setQuery(query);
    }

    public QueryEvent(Object source, Query query, String oldName) {
        this(source, query);
        setOldName(oldName);
    }

    public QueryEvent(Object source, Query query, int type) {
        this(source, query);
        setId(type);
    }
    
    /**
     * Creates a query event, specifying DataMap, containing the query
     */
    public QueryEvent(Object source, Query query, DataMap map) {
        this(source, query);
        setDataMap(map);
    }
    
    /**
     * Creates a query event, specifying DataMap, containing the query
     */
    public QueryEvent(Object source, Query query, String oldName, DataMap map) {
        this(source, query, oldName);
        setDataMap(map);
    }
    
    /**
     * Creates a query event, specifying DataMap, containing the query
     */
    public QueryEvent(Object source, Query query, int type, DataMap map) {
        this(source, query, type);
        setDataMap(map);
    }

    @Override
    public String getNewName() {
        return (query != null) ? query.getName() : null;
    }

    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }
    
    /**
     * @return DataMap, containing the query
     */
    public DataMap getDataMap() {
        return map;
    }
    
    /**
     * Sets DataMap, containing the query
     */
    public void setDataMap(DataMap map) {
        this.map = map;
    }
}

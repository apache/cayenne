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

package org.apache.cayenne.modeler.event.model;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.QueryDescriptor;

/**
 * An event generated when a Query object is added to a DataMap,
 * removed from a DataMap, or changed within a DataMap.
 */
public class QueryEvent extends ModelEvent {

    private final QueryDescriptor query;
    private final DataMap map;

    public static QueryEvent ofAdd(Object source, QueryDescriptor query) {
        return new QueryEvent(source, query, null, Type.ADD, null);
    }

    public static QueryEvent ofAdd(Object source, QueryDescriptor query, DataMap map) {
        return new QueryEvent(source, query, map, Type.ADD, null);
    }

    public static QueryEvent ofChange(Object source, QueryDescriptor query) {
        return new QueryEvent(source, query, null, Type.CHANGE, null);
    }

    public static QueryEvent ofChange(Object source, QueryDescriptor query, String oldName) {
        return new QueryEvent(source, query, null, Type.CHANGE, oldName);
    }

    public static QueryEvent ofChange(Object source, QueryDescriptor query, String oldName, DataMap map) {
        return new QueryEvent(source, query, map, Type.CHANGE, oldName);
    }

    public static QueryEvent ofRemove(Object source, QueryDescriptor query) {
        return new QueryEvent(source, query, null, Type.REMOVE, null);
    }

    public static QueryEvent ofRemove(Object source, QueryDescriptor query, DataMap map) {
        return new QueryEvent(source, query, map, Type.REMOVE, null);
    }

    private QueryEvent(Object source, QueryDescriptor query, DataMap map, Type type, String oldName) {
        super(source, type, oldName);
        this.query = query;
        this.map = map;
    }

    public QueryDescriptor getQuery() {
        return query;
    }

    /**
     * @return DataMap that contains the query.
     */
    public DataMap getDataMap() {
        return map;
    }

    @Override
    public String getNewName() {
        return (query != null) ? query.getName() : null;
    }
}

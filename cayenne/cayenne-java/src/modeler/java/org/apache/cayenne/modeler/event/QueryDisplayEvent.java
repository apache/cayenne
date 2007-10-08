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


package org.apache.cayenne.modeler.event;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.query.Query;

/**
 * @since 1.1
 * @author Andrei Adamchik
 */
public class QueryDisplayEvent extends DataMapDisplayEvent {
    protected Query query;
    protected boolean queryChanged = true;

    public QueryDisplayEvent(Object src, Query query, DataMap map, DataDomain domain) {
        super(src, map, domain);
        this.query = query;
    }

    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    public boolean isQueryChanged() {
        return queryChanged;
    }

    public void setQueryChanged(boolean queryChanged) {
        this.queryChanged = queryChanged;
    }
}

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

import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.QueryResultItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts a {@link QueryResponse} into a list of {@link QueryResultItem}s.
 *
 * @since 5.0
 */
final class QueryResultItems {

    private QueryResultItems() {
    }

    static List<QueryResultItem> fromResponse(QueryResponse response) {
        List<QueryResultItem> items = new ArrayList<>(response.size());
        for (response.reset(); response.next(); ) {
            if (response.isList()) {
                items.add(new QueryResultItem.Select<>(response.currentList()));
            } else if (response.isIterator()) {
                items.add(new QueryResultItem.Iterator<>(response.currentIterator()));
            } else if (response.isOutParameters()) {
                items.add(new QueryResultItem.OutParameters(response.currentOutParameters()));
            } else {
                items.add(new QueryResultItem.Update(response.currentUpdateCount()));
            }
        }
        return items;
    }
}

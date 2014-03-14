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
package org.apache.cayenne.cache;

import java.util.List;

import org.apache.cayenne.query.QueryMetadata;

public class MockQueryCache implements QueryCache {

    public void clear() {
    }

    public List get(QueryMetadata metadata) {
        return null;
    }

    public List get(QueryMetadata metadata, QueryCacheEntryFactory factory) {
        return null;
    }

    public void put(QueryMetadata metadata, List results) {
    }

    public void remove(String key) {
    }

    public void removeGroup(String groupKey) {
    }

    public int size() {
        return 0;
    }
}

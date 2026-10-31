/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */

package org.apache.cayenne.dba.sqlserver;

import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.jdbc.DataNodeSelectAction;
import org.apache.cayenne.access.jdbc.LimitResultIterator;
import org.apache.cayenne.access.translator.TranslatedSelect;

/**
 * @since 4.2
 */
public class SQLServerSelectAction extends DataNodeSelectAction {

    /**
     * When using TOP N instead of LIMIT the offset will be processed in-memory.
     */
    private final boolean needsInMemoryOffset;

    public SQLServerSelectAction(TranslatedSelect translated, DataNode dataNode, boolean needsInMemoryOffset) {
        super(translated, dataNode);
        this.needsInMemoryOffset = needsInMemoryOffset;
    }

    @Override
    protected <T> ResultIterator<T> forFetchLimit(ResultIterator<T> iterator) {
        // "TOP (offset + limit)" in the SQL, so the offset rows are skipped here
        if (!needsInMemoryOffset) {
            return super.forFetchLimit(iterator);
        }
        int offset = query.fetchOffset();
        int limit = query.fetchLimit();
        return limit > 0 || offset > 0 ? new LimitResultIterator<>(iterator, offset, limit) : iterator;
    }
}

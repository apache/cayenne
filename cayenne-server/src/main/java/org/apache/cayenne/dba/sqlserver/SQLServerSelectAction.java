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

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.jdbc.SelectAction;
import org.apache.cayenne.query.Select;

/**
 * @since 4.2
 */
public class SQLServerSelectAction extends SelectAction {

    /**
     * When using TOP N instead of LIMIT the offset will be processed in-memory.
     */
    private final boolean needsInMemoryOffset;

    public SQLServerSelectAction(Select<?> query, DataNode dataNode, boolean needsInMemoryOffset) {
        super(query, dataNode);
        this.needsInMemoryOffset = needsInMemoryOffset;
    }

    @Override
    protected int getInMemoryOffset(int queryOffset) {
        return needsInMemoryOffset ? super.getInMemoryOffset(queryOffset) : 0;
    }
}

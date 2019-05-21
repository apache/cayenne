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

package org.apache.cayenne.access.translator.select;

import java.util.Collections;
import java.util.Map;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.MockQueryMetadata;
import org.apache.cayenne.query.QueryMetadata;

/**
 * @since 4.2
 */
class MockQueryMetadataBuilder {

    private ObjEntity objEntity;

    private DbEntity dbEntity;

    private int limit;

    private int offset;

    private boolean suppressDistinct;

    MockQueryMetadataBuilder withDbEntity(DbEntity entity) {
        this.dbEntity = entity;
        return this;
    }

    MockQueryMetadataBuilder withObjEntity(ObjEntity entity) {
        this.objEntity = entity;
        return this;
    }

    MockQueryMetadataBuilder withLimitOffset(int limit, int offset) {
        this.limit = limit;
        this.offset = offset;
        return this;
    }

    MockQueryMetadataBuilder withSuppressDistinct() {
        this.suppressDistinct = true;
        return this;
    }

    QueryMetadata build() {
        return new MockQueryMetadata() {

            @Override
            public ObjEntity getObjEntity() {
                return objEntity;
            }

            @Override
            public DbEntity getDbEntity() {
                return dbEntity;
            }

            @Override
            public int getFetchOffset() {
                return offset;
            }

            @Override
            public int getFetchLimit() {
                return limit;
            }

            @Override
            public Map<String, String> getPathSplitAliases() {
                return Collections.emptyMap();
            }

            @Override
            public boolean isSuppressingDistinct() {
                return suppressDistinct;
            }
        };
    }
}

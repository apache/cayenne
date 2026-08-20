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

package org.apache.cayenne.access.translator.batch;

import org.apache.cayenne.access.jdbc.PSParameter;
import org.apache.cayenne.access.sqlbuilder.SQLGenerationContext;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.BatchQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 4.2
 * @param <T> type of the {@link BatchQuery}
 */
class BatchTranslatorContext<T extends BatchQuery> implements SQLGenerationContext {

    private final T query;
    private final DbAdapter adapter;
    private final List<PSParameter<?>> bindings;

    BatchTranslatorContext(T query, DbAdapter adapter) {
        this.query = query;
        this.adapter = adapter;
        this.bindings = new ArrayList<>();
    }

    @Override
    public DbAdapter getAdapter() {
        return adapter;
    }

    @Override
    public List<PSParameter<?>> getBindings() {
        return bindings;
    }

    @Override
    public DbEntity getRootDbEntity() {
        return query.getDbEntity();
    }

    public T getQuery() {
        return query;
    }

    @Override
    public boolean isSingleTableSQL() {
        return true;
    }
}

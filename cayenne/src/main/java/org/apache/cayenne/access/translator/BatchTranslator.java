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

package org.apache.cayenne.access.translator;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.query.BatchQuery;

/**
 * A stateless service that translates a batch query of type {@code T} into an immutable
 * {@link TranslatedBatch}. Each batch query flavor (insert, update, delete) is handled by its own
 * translator, configured individually in the DI container under the {@link #INSERT}, {@link #UPDATE}
 * and {@link #DELETE} binding names.
 *
 * @param <T> type of the batch query to translate
 * @since 4.0
 */
public interface BatchTranslator<T extends BatchQuery> {

    /**
     * DI binding name of the translator for {@link org.apache.cayenne.query.InsertBatchQuery}.
     *
     * @since 5.0
     */
    String INSERT = "insert";

    /**
     * DI binding name of the translator for {@link org.apache.cayenne.query.UpdateBatchQuery}.
     *
     * @since 5.0
     */
    String UPDATE = "update";

    /**
     * DI binding name of the translator for {@link org.apache.cayenne.query.DeleteBatchQuery}.
     *
     * @since 5.0
     */
    String DELETE = "delete";

    /**
     * Translates the provided batch query into an immutable {@link TranslatedBatch}.
     *
     * @since 5.0
     */
    TranslatedBatch translate(T query, DbAdapter adapter);
}

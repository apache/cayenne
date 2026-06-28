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

import org.apache.cayenne.access.jdbc.PSBatchParameter;
import org.apache.cayenne.access.jdbc.PSParameter;
import org.apache.cayenne.query.BatchQueryRow;

/**
 * An immutable result of translating a batch query: the SQL String shared by all rows of the batch,
 * the widest possible array of per-batch binding templates, and a stateless {@link BatchRowBinder} that
 * resolves a single row's state into per-row bindings.
 *
 * @since 5.0
 */
public record TranslatedBatch(String sql, PSBatchParameter[] bindings, BatchRowBinder binder) {

    /**
     * Resolves the given row's state against the binding templates, returning a fresh per-row bindings array.
     */
    public PSParameter<?>[] updateBindings(BatchQueryRow row) {
        return binder.bind(bindings, row);
    }
}

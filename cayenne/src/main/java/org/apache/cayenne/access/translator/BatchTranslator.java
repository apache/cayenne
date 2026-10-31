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
import org.apache.cayenne.query.resolved.ResolvedBatch;

/**
 * Translates a {@link ResolvedBatch} into a single parametrized statement. One translator is bound per batch kind,
 * under the {@link #INSERT}, {@link #UPDATE} and {@link #DELETE} names.
 *
 * @param <T> the kind of batch translated
 * @since 6.0
 */
public interface BatchTranslator<T extends ResolvedBatch> {

    /**
     * The DI binding name of the insert batch translator.
     */
    String INSERT = "insert";

    /**
     * The DI binding name of the update batch translator.
     */
    String UPDATE = "update";

    /**
     * The DI binding name of the delete batch translator.
     */
    String DELETE = "delete";

    TranslatedBatch translate(T batch, DbAdapter adapter);
}

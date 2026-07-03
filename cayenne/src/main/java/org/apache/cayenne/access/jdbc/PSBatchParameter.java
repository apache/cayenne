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

package org.apache.cayenne.access.jdbc;

import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.map.DbAttribute;

/**
 * An immutable per-batch parameter binding template, carrying the static PreparedStatement position and column
 * metadata shared by all rows of a batch. A per-row {@link PSParameter} is produced by
 * {@link #bind(Object, ExtendedType)}.
 *
 * @since 5.0
 */
public record PSBatchParameter(int psPosition, int psType, int psScale, DbAttribute attribute) {

    /**
     * Resolves this per-batch template into a per-row {@link PSParameter}.
     */
    public <T> PSParameter<T> bind(T value, ExtendedType<T> extendedType) {
        return new PSParameter<>(value, psPosition, psType, psScale, extendedType, attribute);
    }
}

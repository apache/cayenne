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

import org.apache.cayenne.access.DeferredValue;
import org.apache.cayenne.map.DbAttribute;

/**
 * An immutable batch parameter corresponding to a single PreparedStatement placeholder, carrying the values of all
 * batch rows for that placeholder alongside the static position and column metadata. The value of a given row is
 * resolved via {@link #getValue(int)}.
 *
 * <p>A value may be a {@link DeferredValue}, e.g. when it comes from a generated key of another row in the same
 * transaction. Deferred values are resolved when the corresponding row is bound, i.e. after the rows preceding it
 * have been executed.
 *
 * @since 5.0
 */
public record PSBatchParameter(Object[] values, int psPosition, int psType, int psScale, DbAttribute attribute) {

    public Object getValue(int row) {
        Object value = DeferredValue.resolve(values[row]);
        values[row] = value;
        return value;
    }
}

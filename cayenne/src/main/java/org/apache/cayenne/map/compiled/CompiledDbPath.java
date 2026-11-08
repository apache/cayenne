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
package org.apache.cayenne.map.compiled;

import java.util.List;

/**
 * A path resolved against a table: the joins it traverses, in order, and the column it ends in, if it ends in one
 * rather than in a join.
 *
 * @param joins  the joins traversed, possibly none
 * @param column the column the path ends in, or null
 * @since 6.0
 */
public record CompiledDbPath(List<CompiledJoin> joins, CompiledColumn column) {

    /**
     * Returns the component the path ends in: the column or the last join.
     */
    public Object end() {
        return column != null ? column : joins.isEmpty() ? null : joins.getLast();
    }
}

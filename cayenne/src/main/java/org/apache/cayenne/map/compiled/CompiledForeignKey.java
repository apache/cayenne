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
 * A foreign key constraint between two tables: one object per constraint, shared by the two
 * {@link CompiledJoin joins} that traverse it in either direction. The source table holds the
 * foreign key columnPairs.
 *
 * @param source       the table holding the foreign key
 * @param target       the referenced table
 * @param columnPairs      the column pairs, each a source FK column and the target column it references
 * @param toPrimaryKey true if the referenced columnPairs are the whole primary key of the target, so that the source
 *                     columnPairs hold a complete target PK
 * @param identifying  true if the FK columnPairs are a part of the source primary key, so that a source row can't exist
 *                     without its target row
 * @param sourceKey    true if the FK columnPairs are exactly the primary key of the source, so that a target row has at
 *                     most one source row, and the reverse join is to-one
 * @param reflexive    true if the FK references the primary key of its own table
 * @param declared     true if the authored model marks which side holds the FK; false if the compiler inferred it
 *                     from a pair flagged on both sides or neither. Schema generation emits constraints for the
 *                     declared keys only
 * @since 6.0
 */
public record CompiledForeignKey(
        CompiledTable source,
        CompiledTable target,
        List<CompiledColumnPair> columnPairs,
        boolean toPrimaryKey,
        boolean identifying,
        boolean sourceKey,
        boolean reflexive,

        // TODO: To be deleted. Exists only because the compiler tolerates an authored model that does not say which side
        //  of a join pair holds the key. This should be made more strict eventually.
        boolean declared) {

    @Override
    public String toString() {
        return source.name() + " -> " + target.name() + " " + columnPairs;
    }
}

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

import java.util.ArrayList;
import java.util.List;

/**
 * A directional, named traversal of a {@link CompiledForeignKey}: a step in a join path. The forward join goes
 * from the table holding the FK to the referenced table and is to-one; the reverse one goes the other way, and is
 * to-many unless the FK columns are the whole source primary key.
 *
 * @since 6.0
 */
public final class CompiledJoin {

    private final String name;
    private final CompiledForeignKey foreignKey;
    private final boolean forward;
    private final boolean toMany;
    private final List<CompiledColumnPair> columnPairs;

    // written once by the compiler, after both directions are created
    CompiledJoin reverse;

    CompiledJoin(String name, CompiledForeignKey foreignKey, boolean forward, boolean toMany) {
        this.name = name;
        this.foreignKey = foreignKey;
        this.forward = forward;
        this.toMany = toMany;

        if (forward) {
            this.columnPairs = foreignKey.columnPairs();
        } else {
            List<CompiledColumnPair> reversed = new ArrayList<>(foreignKey.columnPairs().size());
            for (CompiledColumnPair pair : foreignKey.columnPairs()) {
                reversed.add(pair.reversed());
            }
            this.columnPairs = reversed;
        }
    }

    public String name() {
        return name;
    }

    /**
     * Returns the constraint this join traverses.
     */
    public CompiledForeignKey foreignKey() {
        return foreignKey;
    }

    /**
     * Returns true if the source of this join holds the foreign key, i.e. this join traverses the
     * constraint from the referencing table to the referenced one.
     */
    public boolean isForeignKey() {
        return forward;
    }

    public CompiledTable source() {
        return forward ? foreignKey.source() : foreignKey.target();
    }

    public CompiledTable target() {
        return forward ? foreignKey.target() : foreignKey.source();
    }

    public boolean isToMany() {
        return toMany;
    }

    /**
     * Returns the column pairs oriented from the source of this join to its target.
     */
    public List<CompiledColumnPair> columnPairs() {
        return columnPairs;
    }

    /**
     * Returns the source columns of the join columnPairs.
     */
    public List<CompiledColumn> sourceColumns() {
        List<CompiledColumn> columns = new ArrayList<>(columnPairs.size());
        for (CompiledColumnPair pair : columnPairs) {
            columns.add(pair.source());
        }
        return columns;
    }

    /**
     * Returns the target columns of the join columnPairs.
     */
    public List<CompiledColumn> targetColumns() {
        List<CompiledColumn> columns = new ArrayList<>(columnPairs.size());
        for (CompiledColumnPair pair : columnPairs) {
            columns.add(pair.target());
        }
        return columns;
    }

    /**
     * Returns the join traversing the same constraint in the opposite direction. Never null: a missing
     * reverse is synthesized when the mapping is compiled.
     */
    public CompiledJoin reverse() {
        return reverse;
    }

    /**
     * Returns true if the source columns of this join hold a complete primary key of the target, so that a
     * row of the source determines the target row. True for a forward join of a
     * {@link CompiledForeignKey#toPrimaryKey() to-PK} constraint, and for the reverse of a constraint whose FK is the
     * whole source key.
     */
    public boolean isToPrimaryKey() {
        return forward ? foreignKey.toPrimaryKey() : foreignKey.sourceKey() && foreignKey.toPrimaryKey();
    }

    /**
     * Returns true if this join holds the FK, and the authored model says so, as opposed to the compiler
     * inferring the FK side of a pair flagged on both sides or neither. Only declared keys are generated as database
     * constraints.
     */
    public boolean isDeclaredForeignKey() {
        return forward && foreignKey.declared();
    }

    /**
     * Returns true if this join holds the FK, and the FK columns are a part of the source primary key.
     */
    public boolean isIdentifying() {
        return forward && foreignKey.identifying();
    }

    /**
     * Returns true if this join holds the FK, and the FK references the primary key of its own table.
     */
    public boolean isReflexive() {
        return forward && foreignKey.reflexive();
    }

    @Override
    public String toString() {
        return source().name() + "." + name + (toMany ? " ->> " : " -> ") + target().name();
    }
}

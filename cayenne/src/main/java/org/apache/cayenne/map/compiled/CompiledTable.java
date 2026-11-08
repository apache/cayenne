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

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.exp.DbPathExp;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.exp.path.CayennePathSegment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A compiled database table.
 *
 * @since 6.0
 */
public final class CompiledTable {

    private final String name;
    private final String catalog;
    private final String schema;
    private final String fullyQualifiedName;
    private final String tableAliasBase;
    private final CompiledDataMap map;
    private final List<CompiledColumn> columns;
    private final Map<String, CompiledColumn> columnsByName;
    private final List<CompiledColumn> primaryKey;
    private final List<CompiledColumn> generatedColumns;
    private final CompiledKeySequence keySequence;
    private final Expression qualifier;

    // written once by the compiler, after the tables are created
    Map<String, CompiledJoin> joins;
    List<CompiledForeignKey> foreignKeys;
    List<CompiledJoin> reflexiveJoins;
    List<CompiledEntity> entities;
    int dependencyIndex;

    CompiledTable(
            CompiledDataMap map,
            String name,
            String catalog,
            String schema,
            String tableAliasBase,
            Expression qualifier,
            List<CompiledColumn> columns,
            Map<String, CompiledColumn> columnsByName,
            List<CompiledColumn> primaryKey,
            List<CompiledColumn> generatedColumns,
            CompiledKeySequence keySequence) {

        this.name = name;
        this.catalog = catalog;
        this.schema = schema;
        this.fullyQualifiedName = fullyQualifiedName(catalog, schema, name);
        this.tableAliasBase = tableAliasBase;
        this.map = map;
        this.columns = columns;
        this.columnsByName = columnsByName;
        this.primaryKey = primaryKey;
        this.generatedColumns = generatedColumns;
        this.keySequence = keySequence;
        this.qualifier = qualifier;
        this.joins = Map.of();
        this.foreignKeys = List.of();
        this.reflexiveJoins = List.of();
        this.entities = List.of();

        for (CompiledColumn column : columns) {
            column.table = this;
        }
    }

    private static String fullyQualifiedName(String catalog, String schema, String name) {
        return (catalog != null && !catalog.isEmpty() ? catalog + '.' : "")
                + (schema != null && !schema.isEmpty() ? schema + '.' : "")
                + name;
    }

    public String name() {
        return name;
    }

    public String catalog() {
        return catalog;
    }

    public String schema() {
        return schema;
    }

    /**
     * Returns the table name including the catalog and the schema, if any of those are present.
     */
    public String fullyQualifiedName() {
        return fullyQualifiedName;
    }

    /**
     * Returns a short mnemonic base used to build table aliases in generated SQL.
     */
    public String tableAliasBase() {
        return tableAliasBase;
    }

    public CompiledDataMap map() {
        return map;
    }

    /**
     * Returns the columns of the table in the order they were mapped.
     */
    public List<CompiledColumn> columns() {
        return columns;
    }

    /**
     * Returns a column by name, or null if no such column exists.
     */
    public CompiledColumn column(String name) {
        return columnsByName.get(name);
    }

    public List<CompiledColumn> primaryKey() {
        return primaryKey;
    }

    /**
     * Returns the names of the primary key columns.
     */
    public List<String> primaryKeyNames() {
        List<String> names = new ArrayList<>(primaryKey.size());
        for (CompiledColumn pk : primaryKey) {
            names.add(pk.name());
        }
        return names;
    }

    /**
     * Returns the columns whose values are generated by the database.
     */
    public List<CompiledColumn> generatedColumns() {
        return generatedColumns;
    }

    /**
     * Returns the key sequence of the table, or null if it has none.
     */
    public CompiledKeySequence keySequence() {
        return keySequence;
    }

    /**
     * Returns the qualifier that is ANDed to all select queries of this table, or null.
     */
    public Expression qualifier() {
        return qualifier;
    }

    /**
     * Returns the joins of this table by name, in either direction of their constraints.
     */
    public Collection<CompiledJoin> joins() {
        return joins.values();
    }

    /**
     * Returns a join by name, or null if no such join exists.
     */
    public CompiledJoin join(String name) {
        return joins.get(name);
    }

    /**
     * Returns the foreign key constraints held by this table.
     */
    public List<CompiledForeignKey> foreignKeys() {
        return foreignKeys;
    }

    /**
     * Returns the FK-holding joins of this table that reference the primary key of the table itself, e.g.
     * a parent reference in a tree table.
     */
    public List<CompiledJoin> reflexiveJoins() {
        return reflexiveJoins;
    }

    /**
     * Returns true if the table has {@link #reflexiveJoins() reflexive joins}.
     */
    public boolean isReflexive() {
        return !reflexiveJoins.isEmpty();
    }

    /**
     * Returns the object entities mapped to this table, including the sub-entities sharing it. Empty for a table
     * of a schema compiled on its own.
     */
    public List<CompiledEntity> entities() {
        return entities;
    }

    /**
     * Returns the dependency level of this table: 0 for a table referencing no other table by a foreign key to its
     * primary key, otherwise one more than the deepest table it references. Tables of a dependency cycle share the
     * level. Sorting tables by the index gives an insert order, and the reverse of it a delete order.
     */
    public int dependencyIndex() {
        return dependencyIndex;
    }

    /**
     * Returns true if a full replacement id is attached to the ObjectId: all PK columns are present and only PK
     * columns are present.
     */
    public boolean isFullReplacementIdAttached(ObjectId id) {
        if (!id.isReplacementIdAttached()) {
            return false;
        }
        Map<String, Object> replacement = id.getReplacementIdMap();
        if (primaryKey.size() != replacement.size()) {
            return false;
        }
        for (CompiledColumn pk : primaryKey) {
            if (!replacement.containsKey(pk.name())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Resolves the path against this table into the {@link CompiledJoin joins} it traverses and the
     * {@link CompiledColumn column} it ends in, if any. An invalid path results in an ExpressionException.
     */
    public CompiledDbPath resolvePath(CayennePath path) throws ExpressionException {
        List<CayennePathSegment> segments = path.segments();
        int last = segments.size() - 1;
        List<CompiledJoin> joins = new ArrayList<>(segments.size());
        CompiledTable table = this;
        for (int i = 0; i <= last; i++) {
            String segment = segments.get(i).value();
            CompiledColumn column = table.column(segment);
            if (column != null) {
                if (i < last) {
                    throw new ExpressionException("Attribute must be the last component of the path: '%s'.",
                            path.value(), null, segment);
                }
                return new CompiledDbPath(joins, column);
            }
            CompiledJoin join = table.join(segment);
            if (join == null) {
                throw new ExpressionException("Can't resolve path component: [%s.%s].", path.value(), null,
                        table.name(), segment);
            }
            joins.add(join);
            table = join.target();
        }
        return new CompiledDbPath(joins, null);
    }

    /**
     * Transforms an Expression rooted in this table to an analogous expression rooted in a related table, reached by
     * the join path.
     */
    public Expression translateToRelatedTable(Expression expression, CayennePath joinPath) {
        if (expression == null) {
            return null;
        }
        if (joinPath == null || joinPath.isEmpty()) {
            return expression;
        }
        CompiledDbPath resolved = resolvePath(joinPath);
        if (resolved.column() != null) {
            throw new ExpressionException("Not a join path: '%s'.", joinPath.value(), null);
        }
        return expression.transform(new RelationshipPathConverter(resolved.joins()));
    }

    @Override
    public String toString() {
        return "CompiledTable[" + name + "]";
    }

    private static final class RelationshipPathConverter implements Function<Object, Object> {

        private final CayennePath reversedPath;

        RelationshipPathConverter(List<CompiledJoin> joins) {
            CayennePath reversed = CayennePath.EMPTY_PATH;
            for (CompiledJoin join : joins) {
                reversed = CayennePath.of(join.reverse().name()).dot(reversed);
            }
            this.reversedPath = reversed;
        }

        @Override
        public Object apply(Object input) {
            if (!(input instanceof DbPathExp pathExp)) {
                return input;
            }
            CayennePath path = (CayennePath) pathExp.getOperand(0);
            return ExpressionFactory.dbPathExp(reversedPath.dot(path));
        }
    }
}

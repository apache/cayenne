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

import org.apache.cayenne.Fault;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.reflect.Accessor;
import org.apache.cayenne.reflect.PropertyException;
import org.apache.cayenne.reflect.generic.ValueComparisonStrategy;

import java.util.List;
import java.util.Objects;

/**
 * A compiled attribute of an {@link CompiledEntity entity}: a property of the persistent class mapped to a column,
 * either of the entity's own table or of a table reached through a path of relationships. Every entity has its own
 * instances, including those of the inherited attributes, as a sub-entity may map an inherited attribute to a
 * different column.
 * <p>
 * The attribute reads and writes its property on the objects of the entity. A "direct" read or write does not
 * disturb the fault status of the object, the other kind resolves the object first.
 *
 * @since 6.0
 */
public final class CompiledAttribute implements CompiledProperty {

    private final String name;
    private final CompiledEntity entity;
    private final String valueClassName;
    private final Class<?> valueClass;
    private final CayennePath dbPath;
    private final List<CompiledJoin> joins;
    private final CompiledColumn column;
    private final boolean primaryKey;
    private final boolean usedForLocking;
    private final boolean inherited;
    private final boolean synthetic;
    private final CompiledEmbeddedAttribute embedded;
    private final Accessor accessor;
    private final ValueComparisonStrategy<Object> comparisonStrategy;

    // the fault of a lazy attribute, written once by the compiler, as it refers to this attribute
    Fault fault;

    CompiledAttribute(
            CompiledEntity entity,
            String name,
            String valueClassName,
            Class<?> valueClass,
            CayennePath dbPath,
            List<CompiledJoin> joins,
            CompiledColumn column,
            boolean primaryKey,
            boolean usedForLocking,
            boolean inherited,
            boolean synthetic,
            CompiledEmbeddedAttribute embedded,
            Accessor accessor,
            ValueComparisonStrategy<Object> comparisonStrategy) {

        this.name = name;
        this.entity = entity;
        this.valueClassName = valueClassName;
        this.valueClass = valueClass;
        this.dbPath = dbPath;
        this.joins = joins;
        this.column = column;
        this.primaryKey = primaryKey;
        this.usedForLocking = usedForLocking;
        this.inherited = inherited;
        this.synthetic = synthetic;
        this.embedded = embedded;
        this.accessor = accessor;
        this.comparisonStrategy = comparisonStrategy;
    }

    @Override
    public String name() {
        return name;
    }

    /**
     * Returns the entity this attribute belongs to.
     */
    @Override
    public CompiledEntity entity() {
        return entity;
    }

    /**
     * Returns the fully qualified name of the class of the property value, as mapped.
     */
    public String valueClassName() {
        return valueClassName;
    }

    /**
     * Returns the class of the property value, or null if the mapping was compiled without class binding.
     */
    public Class<?> valueClass() {
        return valueClass;
    }

    /**
     * Returns the path from the table of the entity to the column of the attribute: zero or more relationship names
     * followed by the column name. Null for an attribute that is not mapped to a column.
     */
    public CayennePath dbPath() {
        return dbPath;
    }

    /**
     * Returns the joins of a {@link #isFlattened() flattened} path, or an empty list.
     */
    public List<CompiledJoin> joins() {
        return joins;
    }

    /**
     * Returns the column of the attribute, or null if the attribute is not mapped to a column, or its path does not
     * resolve.
     */
    public CompiledColumn column() {
        return column;
    }

    /**
     * Returns the name of the column, which is the last component of the {@link #dbPath()}, or null.
     */
    public String columnName() {
        return dbPath == null || dbPath.isEmpty() ? null : dbPath.last().value();
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public boolean isMandatory() {
        return column != null && column.isMandatory();
    }

    /**
     * Returns the maximum length of the column, or -1.
     */
    public int maxLength() {
        return column != null ? column.maxLength() : -1;
    }

    public boolean isUsedForLocking() {
        return usedForLocking;
    }

    /**
     * Returns true if the value of this attribute is not read with the object, but on first access, through its
     * {@link #fault() fault}.
     */
    public boolean isLazy() {
        return fault != null;
    }

    /**
     * Returns the fault that stands for the unread value of a lazy attribute, or null for an attribute read with
     * the object.
     */
    public Fault fault() {
        return fault;
    }

    /**
     * Returns true if the column is in a table other than the table of the entity, reached through relationships.
     */
    public boolean isFlattened() {
        return dbPath != null && dbPath.length() > 1;
    }

    /**
     * Returns true if a super-entity has this attribute, whether or not this entity redeclares it to map it
     * differently.
     */
    public boolean isInherited() {
        return inherited;
    }

    /**
     * Returns true if the attribute has no property in the persistent class: it stands for a column of the entity
     * table that is not mapped, such as a PK column or a discriminator column. A synthetic attribute can't be read or
     * written.
     */
    public boolean isSynthetic() {
        return synthetic;
    }

    /**
     * Returns the attribute holding the embeddable object this attribute is a part of, or null for a regular
     * attribute.
     */
    public CompiledEmbeddedAttribute embedded() {
        return embedded;
    }

    @Override
    public Object readPropertyDirectly(Object object) throws PropertyException {
        return accessor().getValue(object);
    }

    @Override
    public Object readProperty(Object object) throws PropertyException {
        Persistent persistent = persistent(object);
        if (embedded == null) {
            return persistent.readProperty(name);
        }
        resolveFault(persistent);
        return accessor().getValue(persistent);
    }

    @Override
    public void writePropertyDirectly(Object object, Object oldValue, Object newValue) throws PropertyException {
        accessor().setValue(object, newValue);
    }

    @Override
    public void writeProperty(Object object, Object oldValue, Object newValue) throws PropertyException {
        Persistent persistent = persistent(object);
        if (embedded == null) {
            persistent.writeProperty(name, newValue);
            return;
        }
        resolveFault(persistent);
        accessor().setValue(persistent, newValue);
    }

    /**
     * Compares two values of the attribute for equality, using the comparison strategy of its value type.
     */
    @Override
    public boolean equals(Object value1, Object value2) {
        return comparisonStrategy != null ? comparisonStrategy.equals(value1, value2) : Objects.deepEquals(value1, value2);
    }

    private Accessor accessor() {
        if (accessor == null) {
            throw new PropertyException("Attribute '" + entity.name() + "." + name + "' has no property to access");
        }
        return accessor;
    }

    private Persistent persistent(Object object) {
        if (object instanceof Persistent persistent) {
            return persistent;
        }
        throw new PropertyException("Object is not a Persistent: '" + object.getClass().getName() + "'");
    }

    private void resolveFault(Persistent persistent) {
        ObjectContext context = persistent.getObjectContext();
        if (context != null) {
            context.prepareForAccess(persistent, name, false);
        }
    }

    @Override
    public String toString() {
        return "CompiledAttribute[" + entity.name() + "." + name + "]";
    }
}

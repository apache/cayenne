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
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.reflect.PropertyException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A compiled relationship of an {@link CompiledEntity entity}: a property of the persistent class holding a
 * related object or a collection of them, mapped to a path of one or more {@link CompiledJoin DB
 * relationships}. A relationship declared by a super-entity is shared by its sub-entities.
 * <p>
 * The relationship reads and writes its property on the objects of the source entity. A "direct" read or write
 * does not disturb the fault status of the object, the other kind resolves the object first.
 *
 * @since 6.0
 */
public abstract sealed class CompiledRelationship implements CompiledProperty
        permits CompiledToOneRelationship, CompiledToManyRelationship {

    final String name;
    final CompiledEntity source;
    final CompiledEntity target;
    final DeleteRule deleteRule;
    final boolean usedForLocking;
    final List<CompiledJoin> joins;
    final CayennePath dbPath;
    final CayennePath reverseDbPath;

    // written once by the compiler, after all relationships are created
    CompiledRelationship reverse;
    // the reverse on each sub-entity of the target that declares one, by entity
    Map<CompiledEntity, CompiledRelationship> reversesByTarget;
    // the fault standing for an unresolved value, written once by the compiler, as it refers to this relationship
    Fault fault;

    CompiledRelationship(
            CompiledEntity source,
            CompiledEntity target,
            String name,
            DeleteRule deleteRule,
            boolean usedForLocking,
            List<CompiledJoin> joins) {

        this.name = name;
        this.source = source;
        this.target = target;
        this.deleteRule = deleteRule;
        this.usedForLocking = usedForLocking;
        this.joins = joins;

        CayennePath path = CayennePath.EMPTY_PATH;
        CayennePath reversePath = CayennePath.EMPTY_PATH;
        for (CompiledJoin join : joins) {
            path = path.dot(join.name());
            reversePath = CayennePath.of(join.reverse().name()).dot(reversePath);
        }
        this.dbPath = joins.isEmpty() ? null : path;
        this.reverseDbPath = joins.isEmpty() ? null : reversePath;
    }

    @Override
    public String name() {
        return name;
    }

    /**
     * Returns the entity declaring this relationship. For an inherited relationship this is the super-entity.
     */
    /**
     * Returns the source entity, the entity this relationship is a property of.
     */
    @Override
    public CompiledEntity entity() {
        return source;
    }

    public CompiledEntity source() {
        return source;
    }

    public CompiledEntity target() {
        return target;
    }

    public abstract boolean isToMany();

    /**
     * Returns the delete rule, or null if no action is taken on the target objects when the source is deleted.
     */
    public DeleteRule deleteRule() {
        return deleteRule;
    }

    public boolean isUsedForLocking() {
        return usedForLocking;
    }

    /**
     * Returns the joins this relationship maps to, in path order.
     */
    public List<CompiledJoin> joins() {
        return joins;
    }

    /**
     * Returns the path over the names of the mapped DB relationships, or null if none are mapped.
     */
    public CayennePath dbPath() {
        return dbPath;
    }

    /**
     * Returns the path over the names of the reverses of the mapped DB relationships, in reverse order, or null if
     * none are mapped.
     */
    public CayennePath reverseDbPath() {
        return reverseDbPath;
    }

    /**
     * Returns true if the relationship maps to more than one DB relationship.
     */
    public boolean isFlattened() {
        return joins.size() > 1;
    }

    /**
     * Returns true if the source of this relationship holds the foreign key, i.e. this is a to-one over a single
     * {@link CompiledJoin#isForeignKey() FK} DB relationship.
     */
    public boolean isForeignKey() {
        return !isToMany() && joins.size() == 1 && joins.getFirst().isForeignKey();
    }

    /**
     * Returns true if the source join columns hold a complete target primary key, i.e. this is a to-one over a single
     * {@link CompiledJoin#isToPrimaryKey() to-PK} DB relationship.
     */
    public boolean isToPrimaryKey() {
        return joins.size() == 1 && joins.getFirst().isToPrimaryKey();
    }

    /**
     * Returns the relationship of the target entity going in the opposite direction, or null if there is none.
     */
    public CompiledRelationship reverse() {
        return reverse;
    }

    /**
     * Returns the relationship of the given entity, the target of this relationship or one of its sub-entities,
     * that goes in the opposite direction over the same joins, or null if there is none. For the target entity
     * itself this is {@link #reverse()}.
     */
    public CompiledRelationship reverse(CompiledEntity targetEntity) {
        return targetEntity != null ? reversesByTarget.get(targetEntity) : null;
    }

    /**
     * Returns the fault that stands for an unresolved value of this relationship.
     */
    public Fault fault() {
        return fault;
    }

    @Override
    public Object readPropertyDirectly(Object object) throws PropertyException {
        try {
            return persistent(object).readPropertyDirectly(name);
        } catch (PropertyException e) {
            throw e;
        } catch (Throwable th) {
            throw new PropertyException("Error reading Persistent property: " + name, this, object, th);
        }
    }

    @Override
    public Object readProperty(Object object) throws PropertyException {
        try {
            return persistent(object).readProperty(name);
        } catch (PropertyException e) {
            throw e;
        } catch (Throwable th) {
            throw new PropertyException("Error reading Persistent property: " + name, this, object, th);
        }
    }

    @Override
    public void writePropertyDirectly(Object object, Object oldValue, Object newValue) throws PropertyException {
        try {
            persistent(object).writePropertyDirectly(name, newValue);
        } catch (PropertyException e) {
            throw e;
        } catch (Throwable th) {
            throw new PropertyException("Error writing Persistent property: " + name, this, object, th);
        }
    }

    @Override
    public void writeProperty(Object object, Object oldValue, Object newValue) throws PropertyException {
        try {
            persistent(object).writeProperty(name, newValue);
        } catch (PropertyException e) {
            throw e;
        } catch (Throwable th) {
            throw new PropertyException("Error writing Persistent property: " + name, this, object, th);
        }
    }

    /**
     * Compares two values of the relationship for equality: the same object, holder or fault.
     */
    @Override
    public boolean equals(Object value1, Object value2) {
        return Objects.deepEquals(value1, value2);
    }

    /**
     * Injects a value holder into an object that has none yet, so that the relationship can be resolved lazily.
     */
    public abstract void injectValueHolder(Object object) throws PropertyException;

    /**
     * Returns whether a value of this property, as returned by {@link #readPropertyDirectly(Object)}, is an unresolved
     * fault.
     */
    public abstract boolean isFaultValue(Object value);

    /**
     * Turns the property of an object into a fault.
     */
    public abstract void invalidate(Object object);

    final Persistent persistent(Object object) throws PropertyException {
        if (object instanceof Persistent persistent) {
            return persistent;
        }
        throw new PropertyException("Object is not a Persistent: '" + object.getClass().getName() + "'", this, object,
                (Throwable) null);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + source.name() + "." + name + "]";
    }
}

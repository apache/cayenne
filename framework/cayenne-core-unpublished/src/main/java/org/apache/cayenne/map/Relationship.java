/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.map;

import java.io.Serializable;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.cayenne.util.ToStringBuilder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * Defines a relationship between two entities. In a DataMap graph relationships represent
 * "arcs" connecting entity "nodes". Relationships are directional, i.e. they have a
 * notion of source and target entity. This makes DataMap a "digraph".
 */
public abstract class Relationship implements CayenneMapEntry, XMLSerializable,
        Serializable {

    protected String name;
    protected Entity sourceEntity;

    protected String targetEntityName;
    protected boolean toMany;

    /**
     * A flag that specifies whether a Relationship was mapped by the user or added
     * dynamically by Cayenne runtime.
     * 
     * @since 3.0
     */
    protected boolean runtime;

    /**
     * Creates an unnamed relationship.
     */
    public Relationship() {
    }

    /**
     * Creates a named relationship.
     */
    public Relationship(String name) {
        setName(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns relationship source entity.
     */
    public Entity getSourceEntity() {
        return sourceEntity;
    }

    /**
     * Sets relationship source entity.
     */
    public void setSourceEntity(Entity sourceEntity) {
        this.sourceEntity = sourceEntity;
    }

    /**
     * Returns a target entity of the relationship.
     */
    public abstract Entity getTargetEntity();

    /**
     * Sets relationship target entity. Internally calls <code>setTargetEntityName</code>.
     */
    public void setTargetEntity(Entity targetEntity) {
        if (targetEntity != null) {
            setTargetEntityName(targetEntity.getName());
        }
        else {
            setTargetEntityName(null);
        }
    }

    /**
     * Returns the name of a target entity.
     */
    public String getTargetEntityName() {
        return targetEntityName;
    }

    /**
     * Sets the name of relationship target entity.
     */
    public void setTargetEntityName(String targetEntityName) {
        this.targetEntityName = targetEntityName;
    }

    /**
     * Returns a boolean value that determines relationship multiplicity. This defines
     * semantics of the connection between two nodes described by the source and target
     * entities. E.g. to-many relationship between two Persistent object classes means
     * that a source object would have a collection of target objects. This is a read-only
     * property.
     */
    public boolean isToMany() {
        return toMany;
    }

    public Object getParent() {
        return getSourceEntity();
    }

    public void setParent(Object parent) {
        if (parent != null && !(parent instanceof Entity)) {
            throw new IllegalArgumentException("Expected null or Entity, got: " + parent);
        }

        setSourceEntity((Entity) parent);
    }

    /**
     * Returns guaranteed non-null MappingNamespace of this relationship. If it happens to
     * be null, and exception is thrown. This method is intended for internal use by
     * Relationship class.
     */
    final MappingNamespace getNonNullNamespace() {
        Entity entity = getSourceEntity();

        if (entity == null) {
            throw new CayenneRuntimeException("Relationship '"
                    + getName()
                    + "' has no parent Entity.");
        }

        return entity.getNonNullNamespace();
    }

    /**
     * Overrides Object.toString() to return informative description.
     */
    @Override
    public String toString() {
        return new ToStringBuilder(this).append("name", getName()).append(
                "toMany",
                isToMany()).toString();
    }

    /**
     * @since 3.0
     */
    public boolean isRuntime() {
        return runtime;
    }

    /**
     * @since 3.0
     */
    public void setRuntime(boolean synthetic) {
        this.runtime = synthetic;
    }
    
    /**
     * Returns a "complimentary" relationship going in the opposite direction. Returns
     * null if no such relationship is found.
     * @since 3.1
     */
    public abstract Relationship getReverseRelationship();
    
    /**
     * Returns if relationship is mandatory
     * @since 3.1
     */
    public abstract boolean isMandatory();
}

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

package org.apache.cayenne.map;

import java.io.Serializable;

import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.cayenne.util.ToStringBuilder;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * Defines a property descriptor that is a part of an Entity. Two examples of things that
 * are described by attributes are Java class properties and database table columns.
 * 
 */
public abstract class Attribute<E extends Entity<E, A, R>, A extends Attribute<E, A, R>, R extends Relationship<E, A, R>>
        implements CayenneMapEntry, XMLSerializable, Serializable {

    protected String name;
    protected Entity<E, A, R> entity;

    /**
     * Creates an unnamed Attribute.
     */
    public Attribute() {
    }

    /**
     * Creates a named Attribute.
     */
    public Attribute(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("name", getName()).toString();
    }

    @Override
    public abstract void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor<?> delegate);

    /**
     * Returns parent entity that holds this attribute.
     */
    public Entity<E, A, R> getEntity() {
        return entity;
    }

    /**
     * Sets parent entity that holds this attribute.
     */
    public void setEntity(Entity<E, A, R> entity) {
        this.entity = entity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getParent() {
        return getEntity();
    }

    @SuppressWarnings("unchecked")
    public void setParent(Object parent) {
        if (parent != null && !(parent instanceof Entity)) {
            throw new IllegalArgumentException("Expected null or Entity, got: " + parent);
        }

        setEntity((Entity<E, A, R>) parent);
    }
}

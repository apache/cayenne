/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.map;

import java.io.Serializable;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.util.CayenneMapEntry;
import org.objectstyle.cayenne.util.XMLSerializable;

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
    public String toString() {
        return new ToStringBuilder(this).append("name", getName()).append(
                "toMany",
                isToMany()).toString();
    }
}

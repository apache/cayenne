/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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

import org.objectstyle.cayenne.CayenneRuntimeException;

/** Superclass of metadata relationship classes. */
public abstract class Relationship extends MapObject {
    protected String targetEntityName;
    protected boolean toMany;

    public Relationship() {
        super();
    }

    public Relationship(String name) {
        this();
        this.setName(name);
    }

    /** 
     * Returns relationship source entity. 
     */
    public Entity getSourceEntity() {
        return (Entity) this.getParent();
    }

    /** 
     * Sets relationship source entity. 
     */
    public void setSourceEntity(Entity sourceEntity) {
        setParent(sourceEntity);
    }

    /** Returns relationship target entity. */
    public abstract Entity getTargetEntity();

    /** 
     * Sets relationship target entity. Internally
     * calls <code>setTargetEntityName</code>.
     */
    public void setTargetEntity(Entity targetEntity) {
        if (targetEntity != null) {
            this.setTargetEntityName(targetEntity.getName());
        }
        else {
            this.setTargetEntityName(null);
        }
    }

    /**
     * Returns the targetEntityName.
     * @return String
     */
    public String getTargetEntityName() {
        return targetEntityName;
    }

    /**
     * Sets the targetEntityName.
     * @param targetEntityName The targetEntityName to set
     */
    public void setTargetEntityName(String targetEntityName) {
        this.targetEntityName = targetEntityName;
    }

    /** 
     * Tells whether relationship from source to target is to-one or to-many.
     * If one-to-many, getxxx() method of the data object class would 
     * return a list, otherwise it returns a single DataObject
     * There is explicitly no setToMany on Relationship.. only DbRelationship
     * supports such a notion, and ObjRelationship derives it's value from the
     * underlying DbRelationship(s) 
     */
    public boolean isToMany() {
        return toMany;
    }

    final MappingNamespace getNonNullNamespace() {
        Entity entity = getSourceEntity();

        if (entity == null) {
            throw new CayenneRuntimeException(
                "Relationship '" + getName() + "' has no parent Entity.");
        }

        return entity.getNonNullNamespace();
    }
}

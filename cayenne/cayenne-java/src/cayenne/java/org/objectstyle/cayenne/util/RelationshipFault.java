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
package org.objectstyle.cayenne.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.property.Property;
import org.objectstyle.cayenne.query.RelationshipQuery;

/**
 * An abstract superlcass of lazily faulted to-one and to-many relationships.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public abstract class RelationshipFault {

    protected Persistent relationshipOwner;
    protected String relationshipName;

    protected RelationshipFault() {

    }

    public RelationshipFault(Persistent relationshipOwner, String relationshipName) {
        if (relationshipOwner == null) {
            throw new NullPointerException("'relationshipOwner' can't be null.");
        }

        if (relationshipName == null) {
            throw new NullPointerException("'relationshipName' can't be null.");
        }

        this.relationshipOwner = relationshipOwner;
        this.relationshipName = relationshipName;
    }

    public String getRelationshipName() {
        return relationshipName;
    }

    public Persistent getRelationshipOwner() {
        return relationshipOwner;
    }

    protected boolean isTransientParent() {
        int state = relationshipOwner.getPersistenceState();
        return state == PersistenceState.NEW || state == PersistenceState.TRANSIENT;
    }

    protected boolean isUncommittedParent() {
        int state = relationshipOwner.getPersistenceState();
        return state == PersistenceState.MODIFIED || state == PersistenceState.DELETED;
    }

    /**
     * Executes a query that returns related objects. Subclasses would invoke this method
     * whenever they need to resolve a fault.
     */
    protected List resolveFromDB() {
        // non-persistent objects shouldn't trigger a fetch
        if (isTransientParent()) {
            return new ArrayList();
        }

        List resolved = relationshipOwner.getObjectContext().performQuery(
                new RelationshipQuery(
                        relationshipOwner.getObjectId(),
                        relationshipName,
                        false));

        if (resolved.isEmpty()) {
            return resolved;
        }

        // see if reverse relationship is to-one and we can connect source to results....

        EntityResolver resolver = relationshipOwner
                .getObjectContext()
                .getEntityResolver();
        ObjEntity sourceEntity = resolver.lookupObjEntity(relationshipOwner
                .getObjectId()
                .getEntityName());

        ObjRelationship relationship = (ObjRelationship) sourceEntity
                .getRelationship(relationshipName);
        ObjRelationship reverse = relationship.getReverseRelationship();

        if (reverse != null && !reverse.isToMany()) {
            Property property = resolver
                    .getClassDescriptor(reverse.getSourceEntity().getName())
                    .getProperty(reverse.getName());

            Iterator it = resolved.iterator();
            while (it.hasNext()) {
                property.writePropertyDirectly(it.next(), null, relationshipOwner);
            }
        }

        return resolved;
    }
}

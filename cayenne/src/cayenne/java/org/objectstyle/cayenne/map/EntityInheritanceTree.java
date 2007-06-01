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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.exp.Expression;

/**
 * A tree structure representing inheritance hierarchy 
 * of an ObjEntity and its subentities.
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
public class EntityInheritanceTree {
    protected ObjEntity entity;
    protected Collection subentities;
    protected Expression normalizedQualifier;

    public EntityInheritanceTree(ObjEntity entity) {
        this.entity = entity;
    }

    /**
     * Returns a qualifier Expression that matches root entity
     * of this tree and all its subentities.
     */
    public Expression qualifierForEntityAndSubclasses() {
        Expression qualifier = entity.getDeclaredQualifier();

        if (qualifier == null) {
            // match all
            return null;
        }

        if (subentities != null) {
            Iterator it = subentities.iterator();
            while (it.hasNext()) {
                EntityInheritanceTree child = (EntityInheritanceTree) it.next();
                Expression childQualifier = child.qualifierForEntityAndSubclasses();

                // if any child qualifier is null, just return null, since no filtering is possible
                if (childQualifier == null) {
                    return null;
                }

                qualifier = qualifier.orExp(childQualifier);
            }
        }

        return qualifier;
    }

    /**
     * Returns the deepest possible entity in the inheritance hierarchy 
     * that can be used to create objects from a given DataRow.
     */
    public ObjEntity entityMatchingRow(DataRow row) {
        // match depth first
        if (subentities != null) {
            Iterator it = subentities.iterator();
            while (it.hasNext()) {
                EntityInheritanceTree child = (EntityInheritanceTree) it.next();
                ObjEntity matched = child.entityMatchingRow(row);

                if (matched != null) {
                    return matched;
                }
            }
        }

        Expression qualifier = entity.getDeclaredQualifier();
        if (qualifier != null) {
            if (normalizedQualifier == null) {
                normalizedQualifier = entity.translateToDbPath(qualifier);
            }

            return normalizedQualifier.match(row) ? entity : null;
        }

        // no qualifier ... matches all rows
        return entity;
    }

    public void addChildNode(EntityInheritanceTree node) {
        if (subentities == null) {
            subentities = new ArrayList(2);
        }

        subentities.add(node);
    }

    public int getChildrenCount() {
        return (subentities != null) ? subentities.size() : 0;
    }

    public Collection getChildren() {
        return (subentities != null) ? subentities : Collections.EMPTY_LIST;
    }

    public ObjEntity getEntity() {
        return entity;
    }

    public Collection allAttributes() {
        if (subentities == null) {
            return entity.getAttributes();
        }

        Collection c = new ArrayList();
        appendDeclaredAttributes(c);

        // add base attributes if any
        ObjEntity superEntity = entity.getSuperEntity();
        if (superEntity != null) {
            c.addAll(superEntity.getAttributes());
        }

        return c;
    }

    public Collection allRelationships() {
        if (subentities == null) {
            return entity.getRelationships();
        }

        Collection c = new ArrayList();
        appendDeclaredRelationships(c);

        // add base relationships if any
        ObjEntity superEntity = entity.getSuperEntity();
        if (superEntity != null) {
            c.addAll(superEntity.getRelationships());
        }

        return c;
    }

    protected void appendDeclaredAttributes(Collection c) {
        c.addAll(entity.getDeclaredAttributes());

        if (subentities != null) {
            Iterator it = subentities.iterator();
            while (it.hasNext()) {
                EntityInheritanceTree child = (EntityInheritanceTree) it.next();
                child.appendDeclaredAttributes(c);
            }
        }
    }

    protected void appendDeclaredRelationships(Collection c) {
        c.addAll(entity.getDeclaredRelationships());

        if (subentities != null) {
            Iterator it = subentities.iterator();
            while (it.hasNext()) {
                EntityInheritanceTree child = (EntityInheritanceTree) it.next();
                child.appendDeclaredRelationships(c);
            }
        }
    }
}

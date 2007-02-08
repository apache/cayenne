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
package org.objectstyle.cayenne;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.objectstyle.cayenne.map.DeleteRule;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.property.ArcProperty;
import org.objectstyle.cayenne.property.ClassDescriptor;
import org.objectstyle.cayenne.property.CollectionProperty;
import org.objectstyle.cayenne.property.Property;
import org.objectstyle.cayenne.property.PropertyVisitor;
import org.objectstyle.cayenne.property.SingleObjectArcProperty;

/**
 * A CayenneContext helper that processes object deletion.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class ObjectContextDeleteAction {

    private ObjectContext context;

    ObjectContextDeleteAction(ObjectContext context) {
        this.context = context;
    }

    boolean performDelete(Persistent object) throws DeleteDenyException {

        int oldState = object.getPersistenceState();

        if (oldState == PersistenceState.TRANSIENT
                || oldState == PersistenceState.DELETED) {
            return false;
        }

        if (object.getObjectContext() == null) {
            throw new CayenneRuntimeException(
                    "Attempt to delete unregistered non-TRANSIENT object: " + object);
        }

        if (object.getObjectContext() != context) {
            throw new CayenneRuntimeException(
                    "Attempt to delete object regsitered in a different ObjectContext. Object: "
                            + object
                            + ", context: "
                            + context);
        }

        if (oldState == PersistenceState.NEW) {
            deleteNew(object);
        }
        else {
            deletePersistent(object);
        }

        return true;
    }

    private void deleteNew(Persistent object) {
        object.setPersistenceState(PersistenceState.TRANSIENT);
        processDeleteRules(object, PersistenceState.NEW);
        context.getGraphManager().unregisterNode(object.getObjectId());
    }

    private void deletePersistent(Persistent object) {
        int oldState = object.getPersistenceState();
        object.setPersistenceState(PersistenceState.DELETED);
        processDeleteRules(object, oldState);
        context.getGraphManager().nodeRemoved(object.getObjectId());
    }

    private void processDeleteRules(final Persistent object, final int oldState) {

        String entityName = object.getObjectId().getEntityName();
        final ObjEntity entity = context.getEntityResolver().lookupObjEntity(entityName);
        ClassDescriptor descriptor = context.getEntityResolver().getClassDescriptor(
                entityName);

        descriptor.visitProperties(new PropertyVisitor() {

            public boolean visitCollectionArc(CollectionProperty property) {
                ObjRelationship relationship = (ObjRelationship) entity
                        .getRelationship(property.getName());

                processRules(object, property, relationship.getDeleteRule(), oldState);
                return true;
            }

            public boolean visitSingleObjectArc(SingleObjectArcProperty property) {
                ObjRelationship relationship = (ObjRelationship) entity
                        .getRelationship(property.getName());

                processRules(object, property, relationship.getDeleteRule(), oldState);
                return true;
            }

            public boolean visitProperty(Property property) {
                return true;
            }
        });
    }

    private void processRules(
            Persistent object,
            ArcProperty property,
            int deleteRule,
            int oldState) {

        if (deleteRule == DeleteRule.NO_ACTION) {
            return;
        }

        Collection relatedObjects = relatedObjects(object, property);
        if (relatedObjects.isEmpty()) {
            return;
        }

        switch (deleteRule) {

            case DeleteRule.DENY:
                object.setPersistenceState(oldState);
                String message = relatedObjects.size() == 1
                        ? "1 related object"
                        : relatedObjects.size() + " related objects";
                throw new DeleteDenyException(object, property.getName(), message);

            case DeleteRule.NULLIFY:
                ArcProperty reverseArc = property.getComplimentaryReverseArc();

                if (reverseArc != null) {

                    if (reverseArc instanceof CollectionProperty) {
                        Iterator iterator = relatedObjects.iterator();
                        while (iterator.hasNext()) {
                            Object relatedObject = iterator.next();
                            ((CollectionProperty) reverseArc).removeTarget(
                                    relatedObject,
                                    object,
                                    true);
                        }
                    }
                    else {
                        Iterator iterator = relatedObjects.iterator();
                        while (iterator.hasNext()) {
                            Object relatedObject = iterator.next();
                            ((SingleObjectArcProperty) reverseArc).setTarget(
                                    relatedObject,
                                    null,
                                    true);
                        }
                    }
                }

                break;
            case DeleteRule.CASCADE:

                Iterator iterator = relatedObjects.iterator();
                while (iterator.hasNext()) {
                    Persistent relatedObject = (Persistent) iterator.next();

                    // this action object is stateless, so we can use 'performDelete'
                    // recursively.
                    performDelete(relatedObject);
                }

                break;
            default:
                object.setPersistenceState(oldState);
                throw new CayenneRuntimeException("Invalid delete rule: " + deleteRule);
        }
    }

    private Collection relatedObjects(Object object, Property property) {
        Object related = property.readProperty(object);

        if (related == null) {
            return Collections.EMPTY_LIST;
        }
        // return collections by copy, to allow removal of objects from the underlying
        // relationship inside the iterator
        else if (property instanceof CollectionProperty) {
            Collection relatedCollection = (Collection) related;
            return relatedCollection.isEmpty() ? Collections.EMPTY_LIST : new ArrayList(
                    relatedCollection);
        }
        else {
            return Collections.singleton(related);
        }
    }
}

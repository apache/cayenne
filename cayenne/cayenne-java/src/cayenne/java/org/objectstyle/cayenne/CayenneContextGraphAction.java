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

import org.objectstyle.cayenne.property.ArcProperty;
import org.objectstyle.cayenne.property.ClassDescriptor;
import org.objectstyle.cayenne.property.CollectionProperty;
import org.objectstyle.cayenne.property.Property;
import org.objectstyle.cayenne.property.PropertyVisitor;
import org.objectstyle.cayenne.property.SingleObjectArcProperty;

/**
 * An action object that processes graph change calls from Persistent object. It handles
 * GraphManager notifications and bi-directional graph consistency.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class CayenneContextGraphAction {

    CayenneContext context;
    ThreadLocal arcChangeInProcess;

    CayenneContextGraphAction(CayenneContext context) {
        this.context = context;
        this.arcChangeInProcess = new ThreadLocal();
    }

    /**
     * Handles property change in a Peristent object.
     */
    void handlePropertyChange(
            Persistent object,
            String propertyName,
            Object oldValue,
            Object newValue) {

        // translate ObjectContext generic property change callback to GraphManager terms
        // (simple properties vs. relationships)

        ClassDescriptor descriptor = context.getEntityResolver().getClassDescriptor(
                object.getObjectId().getEntityName());
        Property property = descriptor.getProperty(propertyName);

        // relationship property
        if (property instanceof ArcProperty) {

            try {
                handleArcPropertyChange(
                        object,
                        (ArcProperty) property,
                        oldValue,
                        newValue);
            }
            finally {
                setArcChangeInProcess(false);
            }
        }
        // simple property
        else {
            handleSimplePropertyChange(object, propertyName, oldValue, newValue);
        }
    }

    void handleArcPropertyChange(
            Persistent object,
            ArcProperty property,
            Object oldValue,
            Object newValue) {

        boolean arcChangeInProcess = isArchChangeInProcess();

        // prevent reverse actions down the stack
        setArcChangeInProcess(true);

        if (oldValue instanceof Persistent) {
            context.getGraphManager().arcDeleted(
                    object.getObjectId(),
                    ((Persistent) oldValue).getObjectId(),
                    property.getName());

            if (!arcChangeInProcess) {
                unsetReverse(property, object, (Persistent) oldValue);
            }

            markAsDirty(object);
        }

        if (newValue instanceof Persistent) {
            context.getGraphManager().arcCreated(
                    object.getObjectId(),
                    ((Persistent) newValue).getObjectId(),
                    property.getName());

            if (!arcChangeInProcess) {
                setReverse(property, object, (Persistent) newValue);
            }

            markAsDirty(object);
        }
    }

    void handleSimplePropertyChange(
            Persistent object,
            String propertyName,
            Object oldValue,
            Object newValue) {
        context.getGraphManager().nodePropertyChanged(
                object.getObjectId(),
                propertyName,
                oldValue,
                newValue);
        markAsDirty(object);
    }

    boolean isArchChangeInProcess() {
        return arcChangeInProcess.get() != null;
    }

    void setArcChangeInProcess(boolean flag) {
        arcChangeInProcess.set(flag ? Boolean.TRUE : null);
    }

    /**
     * Changes object state to MODIFIED if needed.
     */
    private void markAsDirty(Persistent object) {
        if (object.getPersistenceState() == PersistenceState.COMMITTED) {
            object.setPersistenceState(PersistenceState.MODIFIED);
        }
    }

    private void setReverse(
            ArcProperty property,
            final Persistent sourceObject,
            final Persistent targetObject) {

        ArcProperty reverseArc = property.getComplimentaryReverseArc();
        if (reverseArc != null) {
            reverseArc.visit(new PropertyVisitor() {

                public boolean visitCollectionArc(CollectionProperty property) {
                    property.addTarget(targetObject, sourceObject, false);
                    return false;
                }

                public boolean visitSingleObjectArc(SingleObjectArcProperty property) {
                    property.setTarget(targetObject, sourceObject, false);
                    return false;
                }

                public boolean visitProperty(Property property) {
                    return false;
                }

            });

            context.getGraphManager().arcCreated(
                    targetObject.getObjectId(),
                    sourceObject.getObjectId(),
                    reverseArc.getName());

            markAsDirty(targetObject);
        }
    }

    private void unsetReverse(
            ArcProperty property,
            Persistent sourceObject,
            Persistent targetObject) {

        ArcProperty reverseArc = property.getComplimentaryReverseArc();
        if (reverseArc != null) {
            reverseArc.writePropertyDirectly(targetObject, sourceObject, null);

            context.getGraphManager().arcDeleted(
                    targetObject.getObjectId(),
                    sourceObject.getObjectId(),
                    reverseArc.getName());

            markAsDirty(targetObject);
        }
    }
}

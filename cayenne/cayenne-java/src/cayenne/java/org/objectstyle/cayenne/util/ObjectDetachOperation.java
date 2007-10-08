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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.property.ClassDescriptor;
import org.objectstyle.cayenne.property.CollectionProperty;
import org.objectstyle.cayenne.property.Property;
import org.objectstyle.cayenne.property.PropertyVisitor;
import org.objectstyle.cayenne.property.SingleObjectArcProperty;
import org.objectstyle.cayenne.query.PrefetchTreeNode;

/**
 * An operation that creates a subgraph of detached objects, using the PrefetchTree to
 * delineate the graph boundaries. Target objects can be described by a different set of
 * descriptors, thus allowing server-to-client conversion to happen in the process.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class ObjectDetachOperation {

    protected EntityResolver targetResolver;
    protected Map seen;

    public ObjectDetachOperation(EntityResolver targetResolver) {
        this.targetResolver = targetResolver;
        this.seen = new HashMap();
    }

    public void reset() {
        seen.clear();
    }

    /**
     * "Detaches" an object from its context by creating an unattached copy. The copy is
     * created using target descriptor of this operation that may be different from the
     * object descriptor passed to this method.
     */
    public Object detach(
            Object object,
            ClassDescriptor descriptor,
            final PrefetchTreeNode prefetchTree) {
        if (!(object instanceof Persistent)) {
            throw new CayenneRuntimeException("Expected Persistent, got: " + object);
        }

        final Persistent source = (Persistent) object;
        ObjectId id = source.getObjectId();

        // sanity check
        if (id == null) {
            throw new CayenneRuntimeException("Server returned an object without an id: "
                    + source);
        }

        Object seenTarget = seen.get(id);
        if (seenTarget != null) {
            return seenTarget;
        }

        descriptor = descriptor.getSubclassDescriptor(source.getClass());

        // presumably id's entity name should be of the right subclass.
        final ClassDescriptor targetDescriptor = targetResolver.getClassDescriptor(id
                .getEntityName());

        final Persistent target = (Persistent) targetDescriptor.createObject();
        target.setObjectId(id);
        seen.put(id, target);

        descriptor.visitProperties(new PropertyVisitor() {

            public boolean visitSingleObjectArc(SingleObjectArcProperty property) {
                if (prefetchTree != null) {

                    PrefetchTreeNode child = prefetchTree.getNode(property.getName());

                    if (child != null) {
                        Object destinationSource = property.readProperty(source);

                        Object destinationTarget = destinationSource != null ? detach(
                                destinationSource,
                                property.getTargetDescriptor(),
                                child) : null;

                        SingleObjectArcProperty targetProperty = (SingleObjectArcProperty) targetDescriptor
                                .getProperty(property.getName());
                        Object oldTarget = targetProperty.isFault(target)
                                ? null
                                : targetProperty.readProperty(target);
                        targetProperty
                                .writeProperty(target, oldTarget, destinationTarget);
                    }
                }

                return true;
            }

            public boolean visitCollectionArc(CollectionProperty property) {
                if (prefetchTree != null) {
                    PrefetchTreeNode child = prefetchTree.getNode(property.getName());

                    if (child != null) {
                        Collection collection = (Collection) property
                                .readProperty(source);

                        Collection targetCollection = new ArrayList(collection.size());

                        Iterator it = collection.iterator();
                        while (it.hasNext()) {
                            Object destinationSource = it.next();
                            Object destinationTarget = destinationSource != null
                                    ? detach(destinationSource, property
                                            .getTargetDescriptor(), child)
                                    : null;

                            targetCollection.add(destinationTarget);
                        }

                        CollectionProperty targetProperty = (CollectionProperty) targetDescriptor
                                .getProperty(property.getName());
                        targetProperty.writeProperty(target, null, targetCollection);
                    }
                }

                return true;
            }

            public boolean visitProperty(Property property) {
                Property targetProperty = targetDescriptor
                        .getProperty(property.getName());
                targetProperty.writeProperty(target, null, property.readProperty(source));
                return true;
            }

        });

        return target;
    }
}

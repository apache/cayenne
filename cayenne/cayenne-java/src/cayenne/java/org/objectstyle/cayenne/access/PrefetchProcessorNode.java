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
package org.objectstyle.cayenne.access;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.Fault;
import org.objectstyle.cayenne.ValueHolder;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.PrefetchTreeNode;

/**
 * A specialized PrefetchTreeNode used for disjoint prefetch resolving.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
// TODO: Andrus 2/9/2006 optional to-one relationships (Painting -> Artist) are not
// connected by this algorithm. They are being intercepted later when a corresponding
// fault is being resolved, but this seems like a wasteful approach. Test case that
// succeeds, but goes through this wasteful route is
// DataContextPrefetchTst.testPrefetchingToOneNull().
class PrefetchProcessorNode extends PrefetchTreeNode {

    List dataRows;
    List objects;

    ObjRelationship incoming;
    ObjectResolver resolver;

    Map partitionByParent;
    boolean jointChildren;
    boolean partitionedByParent;

    DataObject lastResolved;

    PrefetchProcessorNode(PrefetchTreeNode parent, String segmentPath) {
        super(parent, segmentPath);
    }

    /**
     * Sets up derived flags and values for faster lookup during traversal. Called after
     * all properties are initialized.
     */
    void afterInit() {

        partitionedByParent = !phantom
                && incoming != null
                && incoming.isSourceIndependentFromTargetChange();

        if (partitionedByParent) {
            partitionByParent = new HashMap();
        }
    }

    /**
     * Creates a temporary association between child and parent objects. Permanent
     * relationship is set using the information created here, by calling
     * 'connectToParents'.
     */
    void linkToParent(DataObject object, DataObject parent) {
        if (parent != null) {

            // if a relationship is to-one (i.e. flattened to-one), can connect right
            // away....
            if (!incoming.isToMany()) {
                parent.writeProperty(getName(), object);
            }
            else {

                List peers = (List) partitionByParent.get(parent);

                // wrap in a list even if relationship is to-one... will unwrap at the end
                // of the processing cycle.
                if (peers == null) {
                    peers = new ArrayList();
                    partitionByParent.put(parent, peers);
                }
                // checking for duplicates is needed in case of nested joint prefetches
                // when there is more than one row with the same combination of adjacent
                // parent and child...
                else if (peers.contains(object)) {
                    return;
                }

                peers.add(object);
            }
        }
    }

    void connectToParents() {

        // to-one's were connected earlier...
        if (isPartitionedByParent()) {

            // depending on whether parent is a "phantom" node,
            // use different strategy

            PrefetchProcessorNode parent = (PrefetchProcessorNode) getParent();
            boolean parentObjectsExist = parent.getObjects() != null
                    && parent.getObjects().size() > 0;
            if (incoming.isToMany()) {
                if (parentObjectsExist) {
                    connectToNodeParents(parent.getObjects());
                }
                else {
                    connectToFaultedParents();
                }
            }
            else {
                // optional to-one ... need to fill in unresolved relationships with
                // null...
                if (parentObjectsExist) {
                    clearNullRelationships(parent.getObjects());
                }
            }
        }
    }

    private final void clearNullRelationships(List parentObjects) {
        Iterator it = parentObjects.iterator();
        while (it.hasNext()) {
            DataObject object = (DataObject) it.next();
            if (object.readPropertyDirectly(name) instanceof Fault) {
                object.writeProperty(name, null);
            }
        }
    }

    private final void connectToNodeParents(List parentObjects) {

        Iterator it = parentObjects.iterator();
        while (it.hasNext()) {
            DataObject object = (DataObject) it.next();
            List related = (List) partitionByParent.get(object);
            connect(object, related);
        }
    }

    private final void connectToFaultedParents() {
        Iterator it = partitionByParent.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();

            DataObject object = (DataObject) entry.getKey();
            List related = (List) entry.getValue();
            connect(object, related);
        }
    }

    private final void connect(DataObject object, List related) {
        if (incoming.isToMany()) {
            ValueHolder toManyList = (ValueHolder) object.readProperty(getName());

            // TODO, Andrus 11/15/2005 - if list is modified, shouldn't we attempt to
            // merge the changes instead of overwriting?
            toManyList.setValueDirectly(related != null ? related : new ArrayList(1));
        }
        else {
            // this should've been handled elsewhere
            throw new CayenneRuntimeException(
                    "To-one relationship wasn't handled properly: " + incoming.getName());
        }
    }

    List getDataRows() {
        return dataRows;
    }

    List getObjects() {
        return objects;
    }

    void setResolver(ObjectResolver resolver) {
        this.resolver = resolver;
    }

    ObjectResolver getResolver() {
        return resolver;
    }

    ObjRelationship getIncoming() {
        return incoming;
    }

    void setIncoming(ObjRelationship incoming) {
        this.incoming = incoming;
    }

    void setDataRows(List dataRows) {
        this.dataRows = dataRows;
    }

    void setObjects(List objects) {
        this.objects = objects;
    }

    boolean isJointChildren() {
        return jointChildren;
    }

    void setJointChildren(boolean jointChildren) {
        this.jointChildren = jointChildren;
    }

    boolean isPartitionedByParent() {
        return partitionedByParent;
    }

    DataObject getLastResolved() {
        return lastResolved;
    }

    void setLastResolved(DataObject lastResolved) {
        this.lastResolved = lastResolved;
    }
}
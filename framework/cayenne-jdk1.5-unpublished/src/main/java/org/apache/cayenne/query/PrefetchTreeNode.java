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

package org.apache.cayenne.query;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.StringTokenizer;

import org.apache.cayenne.map.Entity;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * Defines a node in a prefetch tree.
 *
 * @since 1.2
 */
public class PrefetchTreeNode implements Serializable, XMLSerializable {

    public static final int UNDEFINED_SEMANTICS = 0;
    public static final int JOINT_PREFETCH_SEMANTICS = 1;
    public static final int DISJOINT_PREFETCH_SEMANTICS = 2;
    public static final int DISJOINT_BY_ID_PREFETCH_SEMANTICS = 3;

    protected String name;
    protected boolean phantom;
    protected int semantics;
    protected String ejbqlPathEntityId;
    protected String entityName;

    // transient parent allows cloning parts of the tree via serialization
    protected transient PrefetchTreeNode parent;

    // Using Collection instead of Map for children storage (even though there cases of
    // lookup by segment) is a reasonable tradeoff considering that
    // each node has no more than a few children and lookup by name doesn't happen on
    // traversal, only during creation.
    protected Collection<PrefetchTreeNode> children;

    /**
     * Creates a root node of the prefetch tree. Children can be added to the parent by
     * calling "addPath".
     */
    public PrefetchTreeNode() {
        this(null, null);
    }

    /**
     * Creates a phantom PrefetchTreeNode, initializing it with parent node and a name of
     * a relationship segment connecting this node with the parent.
     */
    protected PrefetchTreeNode(PrefetchTreeNode parent, String name) {
        this.parent = parent;
        this.name = name;
        this.phantom = true;
        this.semantics = UNDEFINED_SEMANTICS;
    }

    public void encodeAsXML(XMLEncoder encoder) {
        traverse(new XMLEncoderOperation(encoder));
    }

    /**
     * Returns the root of the node tree. Root is the topmost parent node that itself has
     * no parent set.
     */
    public PrefetchTreeNode getRoot() {
        return (parent != null) ? parent.getRoot() : this;
    }

    /**
     * Returns full prefetch path, that is a dot separated String of node names starting
     * from root and up to and including this node. Note that root "name" is considered to
     * be an empty string.
     */
    public String getPath() {
        return getPath(null);
    }

    public String getPath(PrefetchTreeNode upTillParent) {
        if (parent == null || upTillParent == this) {
            return "";
        }

        StringBuilder path = new StringBuilder(getName());
        PrefetchTreeNode node = this.getParent();

        // root node has no path
        while (node.getParent() != null && node != upTillParent) {
            path.insert(0, node.getName() + ".");
            node = node.getParent();
        }

        return path.toString();
    }

    /**
     * Returns a subset of nodes with "joint" semantics that are to be prefetched in the
     * same query as the current node. Result excludes this node, regardless of its
     * semantics.
     */
    public Collection<PrefetchTreeNode> adjacentJointNodes() {
        Collection<PrefetchTreeNode> c = new ArrayList<PrefetchTreeNode>();
        traverse(new AdjacentJoinsOperation(c));
        return c;
    }

    /**
     * Returns a collection of PrefetchTreeNodes in this tree with joint semantics.
     */
    public Collection<PrefetchTreeNode> jointNodes() {
        Collection<PrefetchTreeNode> c = new ArrayList<PrefetchTreeNode>();
        traverse(new CollectionBuilderOperation(c, false, false, true, false, false));
        return c;
    }

    /**
     * Returns a collection of PrefetchTreeNodes with disjoint semantics.
     */
    public Collection<PrefetchTreeNode> disjointNodes() {
        Collection<PrefetchTreeNode> c = new ArrayList<PrefetchTreeNode>();
        traverse(new CollectionBuilderOperation(c, true, false, false, false, false));
        return c;
    }

    /**
     * Returns a collection of PrefetchTreeNodes with disjoint semantics
     * @since 3.1
     */
    public Collection<PrefetchTreeNode> disjointByIdNodes() {
        Collection<PrefetchTreeNode> c = new ArrayList<PrefetchTreeNode>();
        traverse(new CollectionBuilderOperation(c, false, true, false, false, false));
        return c;
    }

    /**
     * Returns a collection of PrefetchTreeNodes that are not phantoms.
     */
    public Collection<PrefetchTreeNode> nonPhantomNodes() {
        Collection<PrefetchTreeNode> c = new ArrayList<PrefetchTreeNode>();
        traverse(new CollectionBuilderOperation(c, true, true, true, true, false));
        return c;
    }

    /**
     * Returns a clone of subtree that includes all joint children
     * starting from this node itself and till the first occurrence of non-joint node
     *
     * @since 3.1
     */
    public PrefetchTreeNode cloneJointSubtree() {
        return cloneJointSubtree(null);
    }

    private PrefetchTreeNode cloneJointSubtree(PrefetchTreeNode parent) {
        PrefetchTreeNode cloned = new PrefetchTreeNode(parent, getName());
        if (parent != null) {
            cloned.setSemantics(getSemantics());
            cloned.setPhantom(isPhantom());
        }

        if (children != null) {
            for (PrefetchTreeNode child : children) {
                if (child.isJointPrefetch()) {
                    cloned.addChild(child.cloneJointSubtree(cloned));
                }
            }
        }

        return cloned;
    }

    /**
     * Traverses the tree depth-first, invoking callback methods of the processor when
     * passing through the nodes.
     */
    public void traverse(PrefetchProcessor processor) {

        boolean result = false;

        if (isPhantom()) {
            result = processor.startPhantomPrefetch(this);
        }
        else if (isDisjointPrefetch()) {
            result = processor.startDisjointPrefetch(this);
        }
        else if (isDisjointByIdPrefetch()) {
            result = processor.startDisjointByIdPrefetch(this);
        }
        else if (isJointPrefetch()) {
            result = processor.startJointPrefetch(this);
        }
        else {
            result = processor.startUnknownPrefetch(this);
        }

        // process children unless processing is blocked...
        if (result && children != null) {
            for (PrefetchTreeNode child : children) {
                child.traverse(processor);
            }
        }

        // call finish regardless of whether children were processed
        processor.finishPrefetch(this);
    }

    /**
     * Looks up an existing node in the tree desribed by the dot-separated path. Will
     * return null if no matching child exists.
     */
    public PrefetchTreeNode getNode(String path) {
        if (Util.isEmptyString(path)) {
            throw new IllegalArgumentException("Empty path: " + path);
        }

        PrefetchTreeNode node = this;
        StringTokenizer toks = new StringTokenizer(path, Entity.PATH_SEPARATOR);
        while (toks.hasMoreTokens() && node != null) {
            String segment = toks.nextToken();
            node = node.getChild(segment);
        }

        return node;
    }

    /**
     * Adds a "path" with specified semantics to this prefetch node. All yet non-existent
     * nodes in the created path will be marked as phantom.
     *
     * @return the last segment in the created path.
     */
    public PrefetchTreeNode addPath(String path) {
        if (Util.isEmptyString(path)) {
            throw new IllegalArgumentException("Empty path: " + path);
        }

        PrefetchTreeNode node = this;
        StringTokenizer toks = new StringTokenizer(path, Entity.PATH_SEPARATOR);
        while (toks.hasMoreTokens()) {
            String segment = toks.nextToken();

            PrefetchTreeNode child = node.getChild(segment);
            if (child == null) {
                child = new PrefetchTreeNode(node, segment);
                node.addChild(child);
            }

            node = child;
        }

        return node;
    }

    /**
     * Removes or makes phantom a node defined by this path. If the node for this path
     * doesn't have any children, it is removed, otherwise it is made phantom.
     */
    public void removePath(String path) {

        PrefetchTreeNode node = getNode(path);
        while (node != null) {

            if (node.children != null) {
                node.setPhantom(true);
                break;
            }

            String segment = node.getName();

            node = node.getParent();

            if (node != null) {
                node.removeChild(segment);
            }
        }
    }

    public void addChild(PrefetchTreeNode child) {

        if (Util.isEmptyString(child.getName())) {
            throw new IllegalArgumentException("Child has no segmentPath: " + child);
        }

        if (child.getParent() != this) {
            child.getParent().removeChild(child.getName());
            child.parent = this;
        }

        if (children == null) {
            children = new ArrayList<PrefetchTreeNode>(4);
        }

        children.add(child);
    }

    public void removeChild(PrefetchTreeNode child) {
        if (children != null && child != null) {
            children.remove(child);
            child.parent = null;
        }
    }

    protected void removeChild(String segment) {
        if (children != null) {
            PrefetchTreeNode child = getChild(segment);
            if (child != null) {
                children.remove(child);
                child.parent = null;
            }
        }
    }

    protected PrefetchTreeNode getChild(String segment) {
        if (children != null) {
            for (PrefetchTreeNode child : children) {
                if (segment.equals(child.getName())) {
                    return child;
                }
            }
        }

        return null;
    }

    public PrefetchTreeNode getParent() {
        return parent;
    }

    /**
     * Returns an unmodifiable collection of children.
     */
    public Collection<PrefetchTreeNode> getChildren() {
        return children == null ? Collections.EMPTY_SET : Collections
                .unmodifiableCollection(children);
    }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }

    public String getName() {
        return name;
    }

    public boolean isPhantom() {
        return phantom;
    }

    public void setPhantom(boolean phantom) {
        this.phantom = phantom;
    }

    public int getSemantics() {
        return semantics;
    }

    public void setSemantics(int semantics) {
        this.semantics = semantics;
    }

    public boolean isJointPrefetch() {
        return semantics == JOINT_PREFETCH_SEMANTICS;
    }

    public boolean isDisjointPrefetch() {
        return semantics == DISJOINT_PREFETCH_SEMANTICS;
    }

    public boolean isDisjointByIdPrefetch() {
        return semantics == DISJOINT_BY_ID_PREFETCH_SEMANTICS;
    }

    public String getEjbqlPathEntityId() {
        return ejbqlPathEntityId;
    }

    public void setEjbqlPathEntityId(String ejbqlPathEntityId) {
        this.ejbqlPathEntityId = ejbqlPathEntityId;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }


    // **** custom serialization that supports serializing subtrees...

    // implementing 'readResolve' instead of 'readObject' so that this would work with
    // hessian
    private Object readResolve() throws ObjectStreamException {

        if (hasChildren()) {
            for (PrefetchTreeNode child : children) {
                child.parent = this;
            }
        }

        return this;
    }

    // **** common tree operations

    // An operation that encodes prefetch tree as XML.
    class XMLEncoderOperation implements PrefetchProcessor {

        XMLEncoder encoder;

        XMLEncoderOperation(XMLEncoder encoder) {
            this.encoder = encoder;
        }

        public boolean startPhantomPrefetch(PrefetchTreeNode node) {
            // don't encode phantoms
            return true;
        }

        public boolean startDisjointPrefetch(PrefetchTreeNode node) {
            encoder.print("<prefetch type=\"disjoint\">");
            encoder.print(node.getPath());
            encoder.println("</prefetch>");
            return true;
        }

        public boolean startDisjointByIdPrefetch(PrefetchTreeNode node) {
            encoder.print("<prefetch type=\"disjointById\">");
            encoder.print(node.getPath());
            encoder.println("</prefetch>");
            return true;
        }

        public boolean startJointPrefetch(PrefetchTreeNode node) {
            encoder.print("<prefetch type=\"joint\">");
            encoder.print(node.getPath());
            encoder.println("</prefetch>");
            return true;
        }

        public boolean startUnknownPrefetch(PrefetchTreeNode node) {
            encoder.print("<prefetch>");
            encoder.print(node.getPath());
            encoder.println("</prefetch>");

            return true;
        }

        public void finishPrefetch(PrefetchTreeNode node) {
            // noop
        }
    }

    // An operation that collects all nodes in a single collection.
    class CollectionBuilderOperation implements PrefetchProcessor {

        Collection<PrefetchTreeNode> nodes;
        boolean includePhantom;
        boolean includeDisjoint;
        boolean includeDisjointById;
        boolean includeJoint;
        boolean includeUnknown;

        CollectionBuilderOperation(Collection<PrefetchTreeNode> nodes, boolean includeDisjoint,
                boolean includeDisjointById, boolean includeJoint, boolean includeUnknown, boolean includePhantom) {
            this.nodes = nodes;

            this.includeDisjoint = includeDisjoint;
            this.includeDisjointById = includeDisjointById;
            this.includeJoint = includeJoint;
            this.includeUnknown = includeUnknown;
            this.includePhantom = includePhantom;
        }

        public boolean startPhantomPrefetch(PrefetchTreeNode node) {
            if (includePhantom) {
                nodes.add(node);
            }

            return true;
        }

        public boolean startDisjointPrefetch(PrefetchTreeNode node) {
            if (includeDisjoint) {
                nodes.add(node);
            }
            return true;
        }

        public boolean startDisjointByIdPrefetch(PrefetchTreeNode node) {
            if (includeDisjointById) {
                nodes.add(node);
            }
            return true;
        }

        public boolean startJointPrefetch(PrefetchTreeNode node) {
            if (includeJoint) {
                nodes.add(node);
            }
            return true;
        }

        public boolean startUnknownPrefetch(PrefetchTreeNode node) {
            if (includeUnknown) {
                nodes.add(node);
            }
            return true;
        }

        public void finishPrefetch(PrefetchTreeNode node) {
        }
    }

    class AdjacentJoinsOperation implements PrefetchProcessor {

        Collection<PrefetchTreeNode> nodes;

        AdjacentJoinsOperation(Collection<PrefetchTreeNode> nodes) {
            this.nodes = nodes;
        }

        public boolean startPhantomPrefetch(PrefetchTreeNode node) {
            return true;
        }

        public boolean startDisjointPrefetch(PrefetchTreeNode node) {
            return node == PrefetchTreeNode.this;
        }

        public boolean startDisjointByIdPrefetch(PrefetchTreeNode node) {
            return startDisjointPrefetch(node);
        }

        public boolean startJointPrefetch(PrefetchTreeNode node) {
            if (node != PrefetchTreeNode.this) {
                nodes.add(node);
            }
            return true;
        }

        public boolean startUnknownPrefetch(PrefetchTreeNode node) {
            return node == PrefetchTreeNode.this;
        }

        public void finishPrefetch(PrefetchTreeNode node) {
        }
    }
}

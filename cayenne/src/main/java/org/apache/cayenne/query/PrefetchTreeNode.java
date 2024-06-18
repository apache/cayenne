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

package org.apache.cayenne.query;

import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.exp.path.CayennePathSegment;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Defines a node in a prefetch tree.
 *
 * @since 1.2
 */
public class PrefetchTreeNode implements Serializable, XMLSerializable {

	private static final long serialVersionUID = 1112629504025820837L;

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
	// each node has no more than a few children and lookup by name doesn't
	// happen on traversal, only during creation.
	protected Collection<PrefetchTreeNode> children;

	/**
	 * Creates and returns a prefetch tree spanning a single path. The tree is
	 * made of phantom nodes, up to the leaf node, which is non-phantom and has
	 * specified semantics.
	 * 
	 * @since 4.0
	 */
	public static PrefetchTreeNode withPath(String path, int semantics) {
		return withPath(CayennePath.of(path), semantics);
	}

	/**
	 * Creates and returns a prefetch tree spanning a single path. The tree is
	 * made of phantom nodes, up to the leaf node, which is non-phantom and has
	 * specified semantics.
	 *
	 * @since 5.0
	 */
	public static PrefetchTreeNode withPath(CayennePath path, int semantics) {
		PrefetchTreeNode root = new PrefetchTreeNode();
		PrefetchTreeNode node = root.addPath(path);
		node.setPhantom(false);
		node.setSemantics(semantics);
		return root;
	}

	/**
	 * Creates a root node of the prefetch tree. Children can be added to the
	 * parent by calling "addPath".
	 */
	public PrefetchTreeNode() {
		this(null, null);
	}

	/**
	 * Creates a phantom PrefetchTreeNode, initializing it with parent node and
	 * a name of a relationship segment connecting this node with the parent.
	 */
	protected PrefetchTreeNode(PrefetchTreeNode parent, String name) {
		this.parent = parent;
		this.name = name;
		this.phantom = true;
		this.semantics = UNDEFINED_SEMANTICS;
	}

	@Override
	public void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor delegate) {
		traverse(new XMLEncoderOperation(encoder));
	}

	/**
	 * Returns the root of the node tree. Root is the topmost parent node that
	 * itself has no parent set.
	 */
	public PrefetchTreeNode getRoot() {
		return (parent != null) ? parent.getRoot() : this;
	}

	/**
	 * Returns full prefetch path, that is a dot separated String of node names
	 * starting from root and up to and including this node.
	 * <br>
	 * <b>Note</b> that root "name" is considered to be an empty string.
	 *
	 * @return path from the root
	 *
	 * @since 5.0 returns {@link CayennePath} instead of a plain {@code String}
	 */
	public CayennePath getPath() {
		return getPath(null);
	}

	/**
	 * Returns full prefetch path, that is a dot separated String of node names
	 * starting from root and up to and including this node.
	 * <br>
	 * <b>Note</b> that root "name" is considered to be an empty string.
	 *
	 * @param upTillParent parent we need to stop at, if {@code null} returns path up to the root
	 * @return path from the specified parent node
	 *
	 * @since 5.0 returns {@link CayennePath} instead of a plain {@code String}
	 */
	public CayennePath getPath(PrefetchTreeNode upTillParent) {
		if (parent == null || upTillParent == this) {
			return CayennePath.EMPTY_PATH;
		}

		CayennePath path = CayennePath.of(getName());
		PrefetchTreeNode node = this.getParent();
		// root node has no path
		while (node.getParent() != null && node != upTillParent) {
			path = CayennePath.of(node.getName()).dot(path);
			node = node.getParent();
		}

		return path;
	}

	/**
	 * Returns a subset of nodes with "joint" semantics that are to be
	 * prefetched in the same query as the current node. Result excludes this
	 * node, regardless of its semantics.
	 */
	public Collection<PrefetchTreeNode> adjacentJointNodes() {
		Collection<PrefetchTreeNode> c = new ArrayList<>();
		traverse(new AdjacentJoinsOperation(c));
		return c;
	}

	/**
	 * Returns a collection of PrefetchTreeNodes in this tree with joint
	 * semantics.
	 */
	public Collection<PrefetchTreeNode> jointNodes() {
		Collection<PrefetchTreeNode> c = new ArrayList<>();
		traverse(new CollectionBuilderOperation(c, false, false, true, false, false));
		return c;
	}

	/**
	 * Returns a collection of PrefetchTreeNodes with disjoint semantics.
	 */
	public Collection<PrefetchTreeNode> disjointNodes() {
		Collection<PrefetchTreeNode> c = new ArrayList<>();
		traverse(new CollectionBuilderOperation(c, true, false, false, false, false));
		return c;
	}

	/**
	 * Returns a collection of PrefetchTreeNodes with disjoint semantics
	 * 
	 * @since 3.1
	 */
	public Collection<PrefetchTreeNode> disjointByIdNodes() {
		Collection<PrefetchTreeNode> c = new ArrayList<>();
		traverse(new CollectionBuilderOperation(c, false, true, false, false, false));
		return c;
	}

	/**
	 * Returns a collection of PrefetchTreeNodes that are not phantoms.
	 */
	public Collection<PrefetchTreeNode> nonPhantomNodes() {
		Collection<PrefetchTreeNode> c = new ArrayList<>();
		traverse(new CollectionBuilderOperation(c, true, true, true, true, false));
		return c;
	}

	/**
	 * Returns a clone of subtree that includes all joint children starting from
	 * this node itself and till the first occurrence of non-joint node
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
	 * Traverses the tree depth-first, invoking callback methods of the
	 * processor when passing through the nodes.
	 */
	public void traverse(PrefetchProcessor processor) {

		boolean result;

		if (isPhantom()) {
			result = processor.startPhantomPrefetch(this);
		} else if (isDisjointPrefetch()) {
			result = processor.startDisjointPrefetch(this);
		} else if (isDisjointByIdPrefetch()) {
			result = processor.startDisjointByIdPrefetch(this);
		} else if (isJointPrefetch()) {
			result = processor.startJointPrefetch(this);
		} else {
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
	 * Looks up an existing node in the tree described by the path.
	 * Will return null if no matching child exists.
	 *
	 * @param path expression to look up node for
	 * @return found node or null if non exists
	 *
	 * @since 5.0
	 */
	public PrefetchTreeNode getNode(CayennePath path) {
		if (path.isEmpty()) {
			throw new IllegalArgumentException("Empty path");
		}
		PrefetchTreeNode node = this;
		for(CayennePathSegment segment : path) {
			node = node.getChild(segment.value());
			if(node == null) {
				return null;
			}
		}
		return node;
	}

	/**
	 * Looks up an existing node in the tree described by the dot-separated path.
	 * Will return null if no matching child exists.
	 *
	 * @param path expression to look up node for
	 * @return found node or null if non exists
	 */
	public PrefetchTreeNode getNode(String path) {
		return getNode(CayennePath.of(path));
	}

	/**
	 * Adds a "path" with specified semantics to this prefetch node. All yet
	 * non-existent nodes in the created path will be marked as phantom.
	 *
	 * @return the last segment in the created path.
	 * @since 5.0
	 */
	public PrefetchTreeNode addPath(CayennePath path) {
		PrefetchTreeNode node = this;
		for(CayennePathSegment segment : path) {
			PrefetchTreeNode child = node.getChild(segment.value());
			if (child == null) {
				child = new PrefetchTreeNode(node, segment.value());
				node.addChild(child);
			}

			node = child;
		}
		return node;
	}

	/**
	 * Adds a "path" with specified semantics to this prefetch node. All yet
	 * non-existent nodes in the created path will be marked as phantom.
	 *
	 * @return the last segment in the created path.
	 */
	public PrefetchTreeNode addPath(String path) {
		return addPath(CayennePath.of(path));
	}

	/**
	 * Merges {@link PrefetchTreeNode} into the current prefetch tree, cloning
	 * the nodes added to this tree. Merged nodes semantics (if defined) and
	 * non-phantom status are applied to the nodes of this tree.
	 * 
	 * @param node
	 *            a root node of a tree to merge into this tree. The path of the
	 *            merged node within the resulting tree is determined from its
	 *            name.
	 * 
	 * @since 4.0
	 */
	public void merge(PrefetchTreeNode node) {
		if (node == null) {
			throw new NullPointerException("Null node");
		}

		PrefetchTreeNode start = node.getName() != null ? addPath(node.getName()) : this;
		merge(start, node);
	}

	void merge(PrefetchTreeNode original, PrefetchTreeNode toMerge) {

		if (toMerge.getSemantics() != UNDEFINED_SEMANTICS) {
			original.setSemantics(toMerge.getSemantics());
		}

		if (!toMerge.isPhantom()) {
			original.setPhantom(false);
		}

		for (PrefetchTreeNode childToMerge : toMerge.getChildren()) {

			PrefetchTreeNode childOrigin = original.getChild(childToMerge.getName());
			if (childOrigin == null) {
				childOrigin = original.addPath(childToMerge.getName());
			}

			merge(childOrigin, childToMerge);
		}
	}

	/**
	 * Removes or makes phantom a node defined by this path. If the node for
	 * this path doesn't have any children, it is removed, otherwise it is made
	 * phantom.
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
			children = new ArrayList<>(4);
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
		return children == null ? Collections.<PrefetchTreeNode> emptySet() : children;
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

	// implementing 'readResolve' instead of 'readObject' so that this would
	// work with
	// hessian
	protected Object readResolve() throws ObjectStreamException {

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
			encoder.start("prefetch")
					.attribute("type", "disjoint")
					.cdata(node.getPath().value(), true)
					.end();
			return true;
		}

		public boolean startDisjointByIdPrefetch(PrefetchTreeNode node) {
			encoder.start("prefetch")
					.attribute("type", "disjointById")
					.cdata(node.getPath().value(), true)
					.end();
			return true;
		}

		public boolean startJointPrefetch(PrefetchTreeNode node) {
			encoder.start("prefetch")
					.attribute("type", "joint")
					.cdata(node.getPath().value(), true)
					.end();
			return true;
		}

		public boolean startUnknownPrefetch(PrefetchTreeNode node) {
			encoder.start("prefetch")
					.cdata(node.getPath().value(), true)
					.end();
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

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

package org.apache.cayenne.modeler;

import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.project.Project;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Map;

/**
 * ProjectTreeModel is a model of Cayenne project tree.
 */
public class ProjectTreeModel extends DefaultTreeModel {

	private Filter filter = new Filter();

	/**
	 * Constructor for ProjectTreeModel.
	 */
	public ProjectTreeModel(Project project) {
		super(ProjectTreeFactory.wrapProjectNode(project.getRootNode()));
	}

	/**
	 * Re-inserts a tree node to preserve the correct ordering of items. Assumes
	 * that the tree is already ordered, except for one node.
	 */
	public void positionNode(MutableTreeNode parent, DefaultMutableTreeNode treeNode, Comparator comparator) {

		if (treeNode == null) {
			return;
		}

		if (parent == null && treeNode != getRoot()) {
			parent = (MutableTreeNode) treeNode.getParent();
			if (parent == null) {
				parent = getRootNode();
			}
		}

		Object object = treeNode.getUserObject();

		if (parent != null) {
			int len = parent.getChildCount();
			int ins = -1;
			int rm = -1;

			for (int i = 0; i < len; i++) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getChildAt(i);
				// remember to remove node
				if (node == treeNode) {
					rm = i;
					continue;
				}

				// no more insert checks
				if (ins >= 0) {
					continue;
				}

				// ObjEntities go before DbEntities
				if (comparator.compare(object, node.getUserObject()) <= 0) {
					ins = i;
				}
			}

			if (ins < 0) {
				ins = len;
			}

			if (rm == ins) {
				return;
			}

			// remove
			if (rm >= 0) {
				removeNodeFromParent(treeNode);
				if (rm < ins) {
					ins--;
				}
			}
			try {
				// insert
				insertNodeInto(treeNode, parent, ins);
			} catch (NullPointerException ignored) {
			}
		}
	}

	public void insertNodeInto(MutableTreeNode newChild,
							   MutableTreeNode parent, int index){
		parent.insert(newChild, index);

		int[] newIndexs = new int[1];

		if(filter.pass((DefaultMutableTreeNode) newChild)) {
			newIndexs[0] = getPositionInTreeView(parent, newChild);
			nodesWereInserted(parent, newIndexs);
		}
	}

	/**
	 * Returns root node cast into DefaultMutableTreeNode.
	 */
	public DefaultMutableTreeNode getRootNode() {
		return (DefaultMutableTreeNode) super.getRoot();
	}

	public DefaultMutableTreeNode getNodeForObjectPath(Object[] path) {
		if (path == null || path.length == 0) {
			return null;
		}

		DefaultMutableTreeNode currentNode = getRootNode();

		// adjust for root node being in the path
		int start = 0;
		if (currentNode.getUserObject() == path[0]) {
			start = 1;
		}

		for (int i = start; i < path.length; i++) {
			DefaultMutableTreeNode foundNode = null;
			Enumeration children = currentNode.children();
			while (children.hasMoreElements()) {
				DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
				if (child.getUserObject() == path[i]) {
					foundNode = child;
					break;
				}
			}

			if (foundNode == null) {
				return null;
			} else {
				currentNode = foundNode;
			}
		}

		return currentNode;
	}

	public void setFiltered(Map<String, Boolean> filterMap) {
		filter.setFilterMap(filterMap);
	}

	public int getChildCount(Object parent) {
		int filterCount = 0;
		for (int i = 0, realCount = super.getChildCount(parent); i < realCount; i++) {
			if (filter.pass((DefaultMutableTreeNode) super.getChild(parent, i))) {
				filterCount++;
			}
		}
		return filterCount;
	}

	public Object getChild(Object parent, int index) {
		int cnt = -1;
		for (int i = 0; i < super.getChildCount(parent); i++) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) super.getChild(parent, i);
			if (filter.pass(child)) {
				cnt++;
			}
			if (cnt == index) {
				return child;
			}
		}
		return null;
	}

	private int getPositionInTreeView(Object parent, Object item) {
		int cnt = -1;
		for(int i = 0; i < super.getChildCount(parent); i++) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) super.getChild(parent, i);
			if(filter.pass(child)) {
				cnt++;
			}
			if(child == item){
				return cnt;
			}
		}
		return cnt;
	}

	public void removeNodeFromParent(MutableTreeNode node) {
		MutableTreeNode parent = (MutableTreeNode)node.getParent();

		if(parent == null) {
			throw new IllegalArgumentException("node does not have a parent.");
		}

		int[] childIndex = new int[1];
		Object[] removedArray = new Object[1];

		childIndex[0] = getPositionInTreeView(parent, node);
		parent.remove(parent.getIndex(node));
		removedArray[0] = node;
		nodesWereRemoved(parent, childIndex, removedArray);
	}

	class Filter {
		private Map<String, Boolean> filterMap;
		boolean pass = true;

		public void setFilterMap(Map<String, Boolean> filterMap) {
			this.filterMap = filterMap;
			pass = false;
		}

		public boolean pass(DefaultMutableTreeNode obj) {
			Object root = obj.getUserObject();
			Object firstLeaf = obj.getFirstLeaf().getUserObject();

			return ((pass) || (root instanceof DataMap) || (root instanceof DataNodeDescriptor)
					|| (firstLeaf instanceof DbEntity && filterMap.get("dbEntity"))
					|| (firstLeaf instanceof ObjEntity && filterMap.get("objEntity"))
					|| (firstLeaf instanceof Embeddable && filterMap.get("embeddable"))
					|| (firstLeaf instanceof QueryDescriptor && filterMap.get("query"))
					|| (firstLeaf instanceof Procedure && filterMap.get("procedure")));
		}
	}
}
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
package org.objectstyle.cayenne.modeler;

import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.project.Project;
import org.objectstyle.cayenne.project.ProjectPath;
import org.objectstyle.cayenne.project.ProjectTraversal;
import org.objectstyle.cayenne.project.ProjectTraversalHandler;

/**
 * ProjectTreeModel is a model of Cayenne project tree.
 * 
 * @author Andrei Adamchik
 */
public class ProjectTreeModel extends DefaultTreeModel {

    /**
     * Creates a tree of Swing TreeNodes wrapping Cayenne project. Returns the root node
     * of the tree.
     */
    public static DefaultMutableTreeNode wrapProject(Project project) {
        return wrapProjectNode(project);
    }

    /**
     * Creates a tree of Swing TreeNodes wrapping Cayenne project object. Returns the root
     * node of the tree.
     */
    public static DefaultMutableTreeNode wrapProjectNode(Object node) {
        TraversalHelper helper = new TraversalHelper();
        new ProjectTraversal(helper).traverse(node);
        return helper.getStartNode();
    }

    /**
     * Creates a tree of Swing TreeNodes wrapping Cayenne project object. Returns the root
     * node of the tree.
     */
    public static DefaultMutableTreeNode wrapProjectNode(
            Object node,
            DefaultMutableTreeNode parentPath) {

        TraversalHelper helper = new TraversalHelper();

        // build a project path from tree node
        ProjectPath path = new ProjectPath();
        if (parentPath != null) {
            path = helper.registerNodes(parentPath.getPath());
        }

        new ProjectTraversal(helper).traverse(node, path);
        return helper.getStartNode();
    }

    /**
     * Constructor for ProjectTreeModel.
     * 
     * @param root
     */
    public ProjectTreeModel(Project project) {
        super(wrapProject(project));
    }

    /**
     * Re-inserts a tree node to preserve the correct ordering of items. Assumes that the
     * tree is already ordered, except for one node.
     */
    public void positionNode(
            MutableTreeNode parent,
            DefaultMutableTreeNode treeNode,
            Comparator comparator) {

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

        // insert
        insertNodeInto(treeNode, parent, ins);
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
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) children
                        .nextElement();
                if (child.getUserObject() == path[i]) {
                    foundNode = child;
                    break;
                }
            }

            if (foundNode == null) {
                return null;
            }
            else {
                currentNode = foundNode;
            }
        }

        return currentNode;
    }

    static class TraversalHelper implements ProjectTraversalHandler {

        protected DefaultMutableTreeNode startNode;
        protected Map nodesMap;

        public TraversalHelper() {
            this.nodesMap = new HashMap();
        }

        public DefaultMutableTreeNode getStartNode() {
            return startNode;
        }

        /**
         * Creates a starting point for tree traversal.
         */
        public ProjectPath registerNodes(TreeNode[] nodes) {
            ProjectPath path = new ProjectPath();

            for (int i = 0; i < nodes.length; i++) {
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) nodes[i];

                path = path.appendToPath(treeNode.getUserObject());

                // register node with helper
                registerNode(treeNode);
            }

            return path;
        }

        public void registerNode(DefaultMutableTreeNode node) {
            nodesMap.put(node.getUserObject(), node);
        }

        public void projectNode(ProjectPath nodePath) {

            Object parent = nodePath.getObjectParent();
            Object nodeObj = nodePath.getObject();
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(nodeObj);

            if (startNode == null) {
                startNode = node;
            }
            else {
                DefaultMutableTreeNode nodeParent = (DefaultMutableTreeNode) nodesMap
                        .get(parent);
                nodeParent.add(node);
            }

            registerNode(node);
        }

        public boolean shouldReadChildren(Object node, ProjectPath parentPath) {
            // do not read deatils of linked maps
            if ((node instanceof DataMap)
                    && parentPath != null
                    && (parentPath.getObject() instanceof DataNode)) {
                return false;
            }

            return (node instanceof Project)
                    || (node instanceof DataDomain)
                    || (node instanceof DataMap)
                    || (node instanceof DataNode);
        }
    }

    /**
     * Traversal hanlder that rebuilds the tree from another tree. Used to reorder tree
     * nodes.
     */
    class CopyTraversalHelper extends TraversalHelper {

        public void projectNode(ProjectPath nodePath) {
            DefaultMutableTreeNode node;

            if (startNode == null) {
                startNode = new DefaultMutableTreeNode(nodePath.getObject());
                node = startNode;
            }
            else {
                DefaultMutableTreeNode original = ProjectTreeModel.this
                        .getNodeForObjectPath(nodePath.getPath());
                DefaultMutableTreeNode nodeParent = (DefaultMutableTreeNode) nodesMap
                        .get(nodePath.getObjectParent());
                node = new DefaultMutableTreeNode(original.getUserObject());
                nodeParent.add(node);
            }

            registerNode(node);
        }
    }
}
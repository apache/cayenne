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

package org.apache.cayenne.modeler.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.Relationship;

/**
 * Swing TreeModel for Entity attributes and relationships
 * 
 * @since 1.1
 * @author Andrus Adamchik
 */
public class EntityTreeModel implements TreeModel {
    protected Entity root;
    protected Map sortedChildren;

    // TODO: in the future replace with a more generic filter 
    // to allow arbitrary tree customization
    protected boolean hideAttributes;

    public EntityTreeModel(Entity root) {
        this.root = root;
        sortedChildren = Collections.synchronizedMap(new HashMap());
    }

    public Object getRoot() {
        return root;
    }

    public boolean isHideAttributes() {
        return hideAttributes;
    }

    public void setHideAttributes(boolean hideAttributes) {
        this.hideAttributes = hideAttributes;
    }

    public Object getChild(Object node, int index) {
        return sortedChildren(node)[index];
    }

    public int getChildCount(Object node) {
        return (node instanceof Attribute) ? 0 : sortedChildren(node).length;
    }

    public boolean isLeaf(Object node) {
        return getChildCount(node) == 0;
    }

    public void valueForPathChanged(TreePath arg0, Object arg1) {
        // do nothing...
    }

    public int getIndexOfChild(Object node, Object child) {
        if (node instanceof Attribute) {
            return -1;
        }

        // wonder if linear search will be faster, considering that
        // this comparator uses reflection?
        return Arrays.binarySearch(
            sortedChildren(node),
            child,
            Comparators.getNamedObjectComparator());
    }

    public void addTreeModelListener(TreeModelListener listener) {
        // do nothing...
    }

    public void removeTreeModelListener(TreeModelListener listener) {
        // do nothing...
    }

    private Object[] sortedChildren(Object node) {
        Entity entity = entityForNonLeafNode(node);
        
        // may happen in incomplete relationships
        if(entity == null) {
            return new Object[0];
        }

        synchronized (sortedChildren) {
            String key = entity.getName();
            Object[] sortedForNode = (Object[]) sortedChildren.get(key);

            if (sortedForNode == null) {
                Collection attributes = entity.getAttributes();
                Collection relationships = entity.getRelationships();

                // combine two collections in an array
                int alen = (hideAttributes) ? 0 : attributes.size();
                int rlen = relationships.size();
                sortedForNode = new Object[alen + rlen];

                if (!hideAttributes) {
                    Iterator ait = attributes.iterator();
                    for (int i = 0; i < alen; i++) {
                        sortedForNode[i] = ait.next();
                    }
                }

                Iterator rit = relationships.iterator();
                for (int i = 0; i < rlen; i++) {
                    sortedForNode[alen + i] = rit.next();
                }

                Arrays.sort(sortedForNode, Comparators.getEntityChildrenComparator());
                sortedChildren.put(key, sortedForNode);
            }

            return sortedForNode;
        }
    }

    private Entity entityForNonLeafNode(Object node) {
        if (node instanceof Entity) {
            return (Entity) node;
        }
        else if (node instanceof Relationship) {
            return ((Relationship) node).getTargetEntity();
        }

        String className = (node != null) ? node.getClass().getName() : "null";
        throw new IllegalArgumentException("Unexpected non-leaf node: " + className);
    }
}

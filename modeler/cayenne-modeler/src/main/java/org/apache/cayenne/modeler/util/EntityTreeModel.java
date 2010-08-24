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
import java.util.List;
import java.util.Map;
import java.util.Vector;

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
 */
public class EntityTreeModel implements TreeModel {
    protected Entity root;
    protected Map<Object, Object[]> sortedChildren;

    /**
     * Filter for checking attributes and relationships
     */
    protected EntityTreeFilter filter;
    
    public EntityTreeModel(Entity root) {
        this.root = root;
        sortedChildren = Collections.synchronizedMap(new HashMap<Object, Object[]>());
    }

    public Object getRoot() {
        return root;
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
            Object key = node;
            Object[] sortedForNode = sortedChildren.get(key);

            if (sortedForNode == null) {
                Collection<? extends Attribute> attributes = entity.getAttributes();
                Collection<? extends Relationship> relationships = entity.getRelationships();

                List<Object> nodes = new Vector<Object>();
                                
                // combine two collections in an array
                for (Attribute attr : attributes) {
                    if (filter == null || filter.attributeMatch(node, attr)) {
                        nodes.add(attr);
                    }
                }

                for (Relationship rel : relationships) {
                    if (filter == null || filter.relationshipMatch(node, rel)) {
                        nodes.add(rel);
                    }
                }

                sortedForNode = nodes.toArray();
                
                Arrays.sort(sortedForNode, Comparators.getEntityChildrenComparator());
                sortedChildren.put(key, sortedForNode);
            }

            return sortedForNode;
        }
    }
    
    /**
     * Removes children cache for specified entity.
     */
    public void invalidate() {
        synchronized (sortedChildren) {
            sortedChildren.clear();
        }
    }
    
    /**
     * Removes children cache for specified entity.
     */
    public void invalidateChildren(Entity entity) {
        synchronized (sortedChildren) {
            sortedChildren.remove(entity);
            
            for (Relationship rel : entity.getRelationships()) {
                sortedChildren.remove(rel);
            }
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
    
    /**
     * Sets filter for attrs and rels
     */
    public void setFilter(EntityTreeFilter filter) {
        this.filter = filter;
    }
    
    /**
     * Returns filter for attrs and rels
     */
    public EntityTreeFilter getFilter() {
        return filter;
    }
}

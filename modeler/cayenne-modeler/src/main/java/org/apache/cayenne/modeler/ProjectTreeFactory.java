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
package org.apache.cayenne.modeler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.cayenne.configuration.BaseConfigurationNodeVisitor;
import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.QueryDescriptor;

/**
 * A helper class that wraps a tree of project nodes into Swing tree nodes.
 */
public class ProjectTreeFactory {

    public static DefaultMutableTreeNode wrapProjectNode(ConfigurationNode node) {
        return node.acceptVisitor(new TreeWrapper());
    }

    private static class TreeWrapper extends
            BaseConfigurationNodeVisitor<DefaultMutableTreeNode> {

        private LinkedList<DefaultMutableTreeNode> stack;

        TreeWrapper() {
            stack = new LinkedList<DefaultMutableTreeNode>();
        }

        private <T extends Comparable<T>> Collection<T> sort(Collection<T> unsorted) {
            if (unsorted.size() < 2) {
                return unsorted;
            }

            List<T> sorted = new ArrayList<T>(unsorted);
            Collections.sort(sorted);
            return sorted;
        }

        private DefaultMutableTreeNode makeNode(Object object) {

            if (object == null) {
                throw new NullPointerException("Null object");
            }

            DefaultMutableTreeNode node = new DefaultMutableTreeNode(object);

            if (!stack.isEmpty()) {
                stack.getLast().add(node);
            }

            return node;
        }

        private DefaultMutableTreeNode pushNode(Object object) {
            DefaultMutableTreeNode node = makeNode(object);
            stack.add(node);
            return node;
        }

        private DefaultMutableTreeNode popNode() {
            return stack.removeLast();
        }

        public DefaultMutableTreeNode visitDataChannelDescriptor(
                DataChannelDescriptor channelDescriptor) {

            pushNode(channelDescriptor);

            for (DataMap map : sort(channelDescriptor.getDataMaps())) {
                map.acceptVisitor(this);
            }

            for (DataNodeDescriptor node : sort(channelDescriptor.getNodeDescriptors())) {
                node.acceptVisitor(this);
            }

            return popNode();
        }

        @Override
        public DefaultMutableTreeNode visitDataNodeDescriptor(
                DataNodeDescriptor nodeDescriptor) {

            DataChannelDescriptor parent = null;

            if (!stack.isEmpty()) {
                DefaultMutableTreeNode parentNode = stack.getLast();
                if (parentNode.getUserObject() instanceof DataChannelDescriptor) {
                    parent = (DataChannelDescriptor) parentNode.getUserObject();
                }
            }

            pushNode(nodeDescriptor);

            if (parent != null) {
                List<String> mapNames = new ArrayList<String>(nodeDescriptor
                        .getDataMapNames());
                Collections.sort(mapNames);
                for (String mapName : mapNames) {
                    makeNode(parent.getDataMap(mapName));
                }
            }

            return popNode();
        }

        @Override
        public DefaultMutableTreeNode visitDataMap(DataMap dataMap) {
            pushNode(dataMap);

            // don't have to sort DataMap children, as DataMap stores everything as
            // SortedMap internally

            for (ObjEntity entity : dataMap.getObjEntities()) {
                makeNode(entity);
            }

            for (Embeddable embeddable : dataMap.getEmbeddables()) {
                makeNode(embeddable);
            }

            for (DbEntity entity : dataMap.getDbEntities()) {
                makeNode(entity);
            }

            for (Procedure procedure : dataMap.getProcedures()) {
                makeNode(procedure);
            }

            for (QueryDescriptor query : dataMap.getQueryDescriptors()) {
                makeNode(query);
            }

            return popNode();
        }
    }

}

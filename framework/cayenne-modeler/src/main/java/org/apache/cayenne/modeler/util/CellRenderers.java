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

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DerivedDbEntity;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.MappingNamespace;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.util.CayenneMapEntry;

/**
 * Utility class that serves as a factory for various project renderers.
 * 
 * @since 1.1
 * @author Andrus Adamchik
 */
public final class CellRenderers {

    // common icons
    protected static ImageIcon domainIcon;
    protected static ImageIcon nodeIcon;
    protected static ImageIcon mapIcon;
    protected static ImageIcon dbEntityIcon;
    protected static ImageIcon objEntityIcon;
    protected static ImageIcon relationshipIcon;
    protected static ImageIcon attributeIcon;
    protected static ImageIcon derivedDbEntityIcon;
    protected static ImageIcon procedureIcon;
    protected static ImageIcon queryIcon;

    static {
        domainIcon = ModelerUtil.buildIcon("icon-dom.gif");
        nodeIcon = ModelerUtil.buildIcon("icon-node.gif");
        mapIcon = ModelerUtil.buildIcon("icon-datamap.gif");
        dbEntityIcon = ModelerUtil.buildIcon("icon-dbentity.gif");
        objEntityIcon = ModelerUtil.buildIcon("icon-objentity.gif");
        derivedDbEntityIcon = ModelerUtil.buildIcon("icon-derived-dbentity.gif");
        procedureIcon = ModelerUtil.buildIcon("icon-stored-procedure.gif");
        queryIcon = ModelerUtil.buildIcon("icon-query.gif");
        relationshipIcon = ModelerUtil.buildIcon("icon-relationship.gif");
        attributeIcon = ModelerUtil.buildIcon("icon-attribute.gif");
    }

    public static ImageIcon iconForObject(Object object) {
        if (object == null) {
            return null;
        }

        if (object instanceof DataDomain) {
            return domainIcon;
        }
        else if (object instanceof DataNode) {
            return nodeIcon;
        }
        else if (object instanceof DataMap) {
            return mapIcon;
        }
        else if (object instanceof Entity) {
            Entity entity = (Entity) object;
            if (entity instanceof DerivedDbEntity) {
                return derivedDbEntityIcon;
            }
            else if (entity instanceof DbEntity) {
                return dbEntityIcon;
            }
            else if (entity instanceof ObjEntity) {
                return objEntityIcon;
            }
        }
        else if (object instanceof Procedure) {
            return procedureIcon;
        }
        else if (object instanceof Query) {
            return queryIcon;
        }
        else if (object instanceof Relationship) {
            return relationshipIcon;
        }
        else if (object instanceof Attribute) {
            return attributeIcon;
        }

        return null;
    }

    /**
     * Returns a TreeCellRenderer to display Cayenne project tree nodes with icons.
     */
    public static TreeCellRenderer treeRenderer() {
        return new TreeRenderer();
    }

    /**
     * Returns a ListCellRenderer to display Cayenne project tree nodes without icons.
     */
    public static ListCellRenderer listRenderer() {
        return new ListRenderer(false);
    }

    /**
     * Returns a ListCellRenderer to display Cayenne project tree nodes with icons.
     */
    public static ListCellRenderer listRendererWithIcons() {
        return new ListRenderer(true);
    }

    /**
     * Returns a ListCellRenderer to display Cayenne project tree nodes with icons.
     */
    public static ListCellRenderer entityListRendererWithIcons(MappingNamespace namespace) {
        return new EntityRenderer(namespace);
    }

    final static class EntityRenderer extends DefaultListCellRenderer {
        MappingNamespace namespace;

        EntityRenderer(MappingNamespace namespace) {
            this.namespace = namespace;
        }

        /**
          * Will trim the value to fit defined size.
          */
        public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {

            // the sequence is important - call super with converted value,
            // then set an icon, and then return "this" 
            ImageIcon icon = CellRenderers.iconForObject(value);

            if (value instanceof CayenneMapEntry) {
                CayenneMapEntry mapObject = (CayenneMapEntry) value;
                String label = mapObject.getName();

                if (mapObject instanceof Entity) {
                    Entity entity = (Entity) mapObject;

                    // for different namespace display its name
                    DataMap dataMap = entity.getDataMap();
                    if (dataMap != null && dataMap != this.namespace) {
                        label += " (" + dataMap.getName() + ")";
                    }
                }
                
                value = label;
            }

            super.getListCellRendererComponent(
                list,
                value,
                index,
                isSelected,
                cellHasFocus);

            setIcon(icon);

            return this;
        }
    }

    final static class ListRenderer extends DefaultListCellRenderer {
        boolean showIcons;

        ListRenderer(boolean showIcons) {
            this.showIcons = showIcons;
        }

        /**
          * Will trim the value to fit defined size.
          */
        public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {

            // the sequence is important - call super with converted value,
            // then set an icon, and then return "this" 

            Object renderedValue = ModelerUtil.getObjectName(value);
            if (renderedValue == null) {
                // render NULL as empty string
                renderedValue = " ";
            }

            super.getListCellRendererComponent(
                list,
                renderedValue,
                index,
                isSelected,
                cellHasFocus);

            if (showIcons) {
                setIcon(iconForObject(value));
            }

            return this;
        }
    }

    final static class TreeRenderer extends DefaultTreeCellRenderer {
        public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean sel,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {

            // the sequence is important - call super,
            // then set an icon, and then return "this" 

            super.getTreeCellRendererComponent(
                tree,
                value,
                sel,
                expanded,
                leaf,
                row,
                hasFocus);

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            setIcon(iconForObject(node.getUserObject()));

            return this;
        }
    }
}

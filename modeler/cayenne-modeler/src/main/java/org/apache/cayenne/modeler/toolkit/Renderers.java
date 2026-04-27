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

package org.apache.cayenne.modeler.toolkit;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.MappingNamespace;
import org.apache.cayenne.modeler.toolkit.icon.IconFactory;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.reflect.PropertyUtils;
import org.apache.cayenne.util.CayenneMapEntry;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

/**
 * Utility class that serves as a factory for various project renderers.
 */
public final class Renderers {

    private static final Font defaultFont = UIManager.getFont("Label.font");

    /**
     * Returns a TreeCellRenderer to display Cayenne project tree nodes with icons.
     */
    public static TreeCellRenderer treeRenderer() {
        return new TreeRenderer();
    }

    /**
     * Returns a ListCellRenderer to display Cayenne project tree nodes without icons.
     */
    public static ListCellRenderer<Object> listRenderer() {
        return new ListRenderer(false);
    }

    /**
     * Returns a ListCellRenderer to display Cayenne project tree nodes with icons.
     */
    public static ListCellRenderer<Object> listRendererWithIcons() {
        return new ListRenderer(true);
    }

    /**
     * Returns a ListCellRenderer to display Cayenne project tree nodes with icons.
     */
    public static ListCellRenderer<Object> entityListRendererWithIcons(MappingNamespace namespace) {
        return new EntityRenderer(namespace);
    }

    /**
     * Returns a TableCellRenderer to display Cayenne project entities with icons in table.
     */
    public static TableCellRenderer entityTableRendererWithIcons(ProjectController mediator) {
        return new EntityTableRenderer(mediator);
    }

    /**
     * Converts non-String Object used in renderers (currently CayenneMapEntry instances only) to String
     */
    public static String asString(Object obj, MappingNamespace namespace) {
        if (obj instanceof CayenneMapEntry) {
            CayenneMapEntry mapObject = (CayenneMapEntry) obj;
            String label = mapObject.getName();

            if (mapObject instanceof Entity) {
                Entity<?, ?, ?> entity = (Entity<?, ?, ?>) mapObject;

                DataMap dataMap = entity.getDataMap();
                if (dataMap != null && dataMap != namespace) {
                    label += " (" + dataMap.getName() + ")";
                }
            }

            return label;
        } else if (obj instanceof DataMap) {
            return ((DataMap) obj).getName();
        }

        return obj == null ? null : String.valueOf(obj);
    }

    public static String asString(Object object) {
        if (object == null) {
            return null;
        } else if (object instanceof CayenneMapEntry) {
            return ((CayenneMapEntry) object).getName();
        } else if (object instanceof String) {
            return (String) object;
        } else {
            try {
                // use reflection
                return (String) PropertyUtils.getProperty(object, "name");
            } catch (Exception ex) {
                return null;
            }
        }
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
            Icon icon = IconFactory.iconForObject(value);

            value = asString(value, namespace);

            super.getListCellRendererComponent(
                    list,
                    value,
                    index,
                    isSelected,
                    cellHasFocus);

            setIcon(icon);//isSelected ? FilteredIconFactory.createIcon(icon, FilteredIconFactory.FilterType.SELECTION) : icon);
            setFont(defaultFont);

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

            Object renderedValue = asString(value);
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
                Icon icon = IconFactory.iconForObject(value);
                setIcon(icon);
            }
            setFont(defaultFont);

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
            Icon icon = IconFactory.iconForObject(node.getUserObject());
            setIcon(icon);
            setFont(defaultFont);

            return this;
        }
    }

    final static class EntityTableRenderer extends DefaultTableCellRenderer {

        private final ProjectController controller;

        public EntityTableRenderer(ProjectController controller) {
            this.controller = controller;
        }

        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            Object oldValue = value;
            value = Renderers.asString(value, controller.getSelectedDataMap());

            super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column);

            Icon icon = IconFactory.iconForObject(oldValue);
            setIcon(icon);
            setFont(defaultFont);

            return this;
        }
    }
}

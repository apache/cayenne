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

package org.apache.cayenne.modeler.util;

import java.awt.Component;
import java.awt.Font;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.MappingNamespace;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.util.CayenneMapEntry;

/**
 * Utility class that serves as a factory for various project renderers.
 * 
 * @since 1.1
 */
public final class CellRenderers {

    // common icons
    protected static Icon domainIcon       = ModelerUtil.buildIcon("icon-dom.png");
    protected static Icon nodeIcon         = ModelerUtil.buildIcon("icon-node.png");
    protected static Icon mapIcon          = ModelerUtil.buildIcon("icon-datamap.png");
    protected static Icon dbEntityIcon     = ModelerUtil.buildIcon("icon-dbentity.png");
    protected static Icon objEntityIcon    = ModelerUtil.buildIcon("icon-objentity.png");
    protected static Icon procedureIcon    = ModelerUtil.buildIcon("icon-stored-procedure.png");
    protected static Icon queryIcon        = ModelerUtil.buildIcon("icon-query.png");
    protected static Icon embeddableIcon   = ModelerUtil.buildIcon("icon-embeddable.png");
    protected static Icon relationshipIcon = ModelerUtil.buildIcon("icon-relationship.png");
    protected static Icon attributeIcon    = ModelerUtil.buildIcon("icon-attribute.png");

    protected static Font defaultFont      = UIManager.getFont("Label.font");

    public static Icon iconForObject(Object object) {
        if (object == null) {
            return null;
        }

        if (object instanceof DataChannelDescriptor) {
            return domainIcon;
        } else if (object instanceof DataNodeDescriptor) {
            return nodeIcon;
        } else if (object instanceof DataMap) {
            return mapIcon;
        } else if (object instanceof DbEntity) {
            return dbEntityIcon;
        } else if (object instanceof ObjEntity) {
            return objEntityIcon;
        } else if (object instanceof Procedure) {
            return procedureIcon;
        } else if (object instanceof QueryDescriptor) {
            return queryIcon;
        } else if (object instanceof Relationship) {
            return relationshipIcon;
        } else if (object instanceof Attribute) {
            return attributeIcon;
        } else if (object instanceof Embeddable) {
            return embeddableIcon;
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
     * Converts non-String Object used in renderers (currently CayenneMapEntry
     * instances only) to String
     * 
     * @param obj Object to be converted
     */
    public static String asString(Object obj) {
        return asString(obj, Application.getInstance(). //none of these is suppeosed to be null
           getFrameController().getProjectController().getCurrentDataMap());
    }
    
    /**
     * Converts non-String Object used in renderers (currently CayenneMapEntry
     * instances only) to String
     * 
     * @param obj Object to be converted
     * @param namespace the current namespace
     */
    public static String asString(Object obj, MappingNamespace namespace) {
        if (obj instanceof CayenneMapEntry) {
            CayenneMapEntry mapObject = (CayenneMapEntry) obj;
            String label = mapObject.getName();

            if (mapObject instanceof Entity) {
                Entity entity = (Entity) mapObject;

                // for different namespace display its name
                DataMap dataMap = entity.getDataMap();
                if (dataMap != null && dataMap != namespace) {
                    label += " (" + dataMap.getName() + ")";
                }
            }
            
            return label;
        }
        else if (obj instanceof DataMap) {
            return ((DataMap) obj).getName();
        }
        
        return obj == null ? null : String.valueOf(obj);
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
            Icon icon = CellRenderers.iconForObject(value);

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
                Icon icon = iconForObject(value);
                if(isSelected) {
//                    icon = FilteredIconFactory.createIcon(icon, FilteredIconFactory.FilterType.SELECTION);
                }
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
            Icon icon = iconForObject(node.getUserObject());
            if(sel) {
//                icon = FilteredIconFactory.createIcon(icon, FilteredIconFactory.FilterType.SELECTION);
            }
            setIcon(icon);
            setFont(defaultFont);

            return this;
        }
    }
    
    final static class EntityTableRenderer extends DefaultTableCellRenderer {
        
        private ProjectController mediator;
        
        public EntityTableRenderer(ProjectController mediator) {
            this.mediator = mediator;
        }

        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            Object oldValue = value;
            value = CellRenderers.asString(value, mediator.getCurrentDataMap());

            super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column);

            Icon icon = CellRenderers.iconForObject(oldValue);
            if(isSelected) {
//                icon = FilteredIconFactory.createIcon(icon, FilteredIconFactory.FilterType.SELECTION);
            }
            setIcon(icon);
            setFont(defaultFont);

            return this;
        }
    }
}

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
package org.objectstyle.cayenne.modeler.util;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.map.Attribute;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DerivedDbEntity;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.MapObject;
import org.objectstyle.cayenne.map.MappingNamespace;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.map.Relationship;
import org.objectstyle.cayenne.query.Query;

/**
 * Utility class that serves as a factory for various project renderers.
 * 
 * @since 1.1
 * @author Andrei Adamchik
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

            if (value instanceof MapObject) {
                MapObject mapObject = (MapObject) value;
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

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
package org.objectstyle.cayenne.modeler.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.event.AttributeEvent;
import org.objectstyle.cayenne.map.event.EntityEvent;
import org.objectstyle.cayenne.map.event.ObjAttributeListener;
import org.objectstyle.cayenne.map.event.ObjEntityListener;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.event.AttributeDisplayEvent;
import org.objectstyle.cayenne.modeler.event.EntityDisplayEvent;
import org.objectstyle.cayenne.modeler.event.ObjEntityDisplayListener;
import org.objectstyle.cayenne.modeler.util.CayenneTable;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.ModelerUtil;
import org.objectstyle.cayenne.modeler.util.PanelFactory;
import org.objectstyle.cayenne.modeler.util.UIUtil;

/**
 * Detail view of the ObjEntity attributes.
 * 
 * @author Michael Misha Shengaout
 * @author Andrei Adamchik
 */
public class ObjEntityAttributeTab extends JPanel implements ObjEntityDisplayListener,
        ObjEntityListener, ObjAttributeListener, ExistingSelectionProcessor {

    protected ProjectController mediator;
    protected CayenneTable table;

    public ObjEntityAttributeTab(ProjectController mediator) {
        this.mediator = mediator;

        init();
        initController();
    }

    private void init() {
        table = new CayenneTable();
        table.setDefaultRenderer(String.class, new CellRenderer());

        setLayout(new BorderLayout());
        add(PanelFactory.createTablePanel(table, null), BorderLayout.CENTER);
    }

    private void initController() {
        mediator.addObjEntityDisplayListener(this);
        mediator.addObjEntityListener(this);
        mediator.addObjAttributeListener(this);

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                processExistingSelection();
            }
        });
    }

    /**
     * Selects a specified attribute.
     */
    public void selectAttribute(ObjAttribute attr) {
        if (attr == null) {
            return;
        }

        ObjAttributeTableModel model = (ObjAttributeTableModel) table.getModel();
        java.util.List attrs = model.getObjectList();
        int attrPos = attrs.indexOf(attr);
        if (attrPos >= 0) {
            table.select(attrPos);
        }
    }

    public void processExistingSelection() {
        ObjAttribute attribute = null;
        if (table.getSelectedRow() >= 0) {
            ObjAttributeTableModel model = (ObjAttributeTableModel) table.getModel();
            attribute = model.getAttribute(table.getSelectedRow());

            // scroll table
            UIUtil.scrollToSelectedRow(table);
        }
        AttributeDisplayEvent ev = new AttributeDisplayEvent(this, attribute, mediator
                .getCurrentObjEntity(), mediator.getCurrentDataMap(), mediator
                .getCurrentDataDomain());

        mediator.fireObjAttributeDisplayEvent(ev);
    }

    public void objAttributeChanged(AttributeEvent e) {
        table.select(e.getAttribute());
    }

    public void objAttributeAdded(AttributeEvent e) {
        rebuildTable((ObjEntity) e.getEntity());
        table.select(e.getAttribute());
    }

    public void objAttributeRemoved(AttributeEvent e) {
        ObjAttributeTableModel model = (ObjAttributeTableModel) table.getModel();
        int ind = model.getObjectList().indexOf(e.getAttribute());
        model.removeRow(e.getAttribute());
        table.select(ind);
    }

    public void currentObjEntityChanged(EntityDisplayEvent e) {
        if (e.getSource() == this) {
            return;
        }

        ObjEntity entity = (ObjEntity) e.getEntity();

        // Important: process event even if this is the same entity,
        // since the inheritance structure might have changed
        if (entity != null) {
            rebuildTable(entity);
        }

        // if an entity was selected on a tree,
        // unselect currently selected row
        if (e.isUnselectAttributes()) {
            table.clearSelection();
        }
    }

    protected void rebuildTable(ObjEntity entity) {
        ObjAttributeTableModel model = new ObjAttributeTableModel(entity, mediator, this);
        table.setModel(model);
        table.setRowHeight(25);
        table.setRowMargin(3);
        setUpTableStructure(model);
    }

    protected void setUpTableStructure(ObjAttributeTableModel model) {
        TableColumn nameColumn = table.getColumnModel().getColumn(
                ObjAttributeTableModel.OBJ_ATTRIBUTE);
        nameColumn.setMinWidth(150);

        TableColumn typeColumn = table.getColumnModel().getColumn(
                ObjAttributeTableModel.OBJ_ATTRIBUTE_TYPE);
        typeColumn.setMinWidth(150);

        JComboBox javaTypesCombo = CayenneWidgetFactory.createComboBox(ModelerUtil
                .getRegisteredTypeNames(), false);
        javaTypesCombo.setEditable(true);
        typeColumn.setCellEditor(new DefaultCellEditor(javaTypesCombo));

        TableColumn lockColumn = table.getColumnModel().getColumn(
                ObjAttributeTableModel.LOCKING);
        lockColumn.setMinWidth(100);

        TableColumn dbTypeColumn = table.getColumnModel().getColumn(
                ObjAttributeTableModel.DB_ATTRIBUTE_TYPE);
        dbTypeColumn.setMinWidth(120);

        TableColumn dbNameColumn = table.getColumnModel().getColumn(
                ObjAttributeTableModel.DB_ATTRIBUTE);
        dbNameColumn.setMinWidth(150);

        if (model.getEntity().getDbEntity() != null) {
            JComboBox dbAttributesCombo = CayenneWidgetFactory.createComboBox(ModelerUtil
                    .getDbAttributeNames(mediator, mediator
                            .getCurrentObjEntity()
                            .getDbEntity()), true);

            dbAttributesCombo.setEditable(false);
            dbNameColumn.setCellEditor(new DefaultCellEditor(dbAttributesCombo));
        }
    }

    /**
     * Refreshes attributes view for the updated entity
     */
    public void objEntityChanged(EntityEvent e) {
        if (e.getSource() == this) {
            return;
        }

        ObjAttributeTableModel model = (ObjAttributeTableModel) table.getModel();
        if (model.getDbEntity() != ((ObjEntity) e.getEntity()).getDbEntity()) {
            model.resetDbEntity();
            setUpTableStructure(model);
        }
    }

    public void objEntityAdded(EntityEvent e) {
    }

    public void objEntityRemoved(EntityEvent e) {
    }

    // custom renderer used for inherited attributes highlighting
    final class CellRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column);

            ObjAttributeTableModel model = (ObjAttributeTableModel) table.getModel();
            ObjAttribute attribute = model.getAttribute(row);

            if (attribute != null && attribute.getEntity() != model.getEntity()) {
                setForeground(Color.GRAY);
            }
            else {
                setForeground(isSelected && !hasFocus
                        ? table.getSelectionForeground()
                        : table.getForeground());
            }

            return this;
        }
    }
}
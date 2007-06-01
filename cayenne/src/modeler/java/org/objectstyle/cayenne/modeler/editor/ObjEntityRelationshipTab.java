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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DeleteRule;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.MapObject;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.map.event.EntityEvent;
import org.objectstyle.cayenne.map.event.ObjEntityListener;
import org.objectstyle.cayenne.map.event.ObjRelationshipListener;
import org.objectstyle.cayenne.map.event.RelationshipEvent;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.dialog.objentity.ObjRelationshipInfoController;
import org.objectstyle.cayenne.modeler.event.EntityDisplayEvent;
import org.objectstyle.cayenne.modeler.event.ObjEntityDisplayListener;
import org.objectstyle.cayenne.modeler.event.RelationshipDisplayEvent;
import org.objectstyle.cayenne.modeler.util.CayenneTable;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.CellRenderers;
import org.objectstyle.cayenne.modeler.util.PanelFactory;
import org.objectstyle.cayenne.modeler.util.UIUtil;

/**
 * Displays ObjRelationships for the edited ObjEntity.
 * 
 * @author Michael Misha Shengaout
 * @author Andrei Adamchik
 */
public class ObjEntityRelationshipTab extends JPanel implements ObjEntityDisplayListener,
        ObjEntityListener, ObjRelationshipListener, ExistingSelectionProcessor {
    
    private static final Logger logObj = Logger.getLogger(ObjEntityRelationshipTab.class);

    private static final Object[] deleteRules = new Object[] {
            DeleteRule.deleteRuleName(DeleteRule.NO_ACTION),
            DeleteRule.deleteRuleName(DeleteRule.NULLIFY),
            DeleteRule.deleteRuleName(DeleteRule.CASCADE),
            DeleteRule.deleteRuleName(DeleteRule.DENY),
    };

    ProjectController mediator;

    CayenneTable table;
    JButton resolve;

    public ObjEntityRelationshipTab(ProjectController mediator) {
        this.mediator = mediator;

        init();
        initController();
    }

    private void init() {
        table = new CayenneTable();
        table.setDefaultRenderer(String.class, new StringRenderer());
        table.setDefaultRenderer(ObjEntity.class, new EntityRenderer());

        resolve = new JButton("Edit Relationship");

        JPanel panel = PanelFactory.createTablePanel(table, new JButton[] {
            resolve
        });

        this.setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
    }

    private void initController() {
        mediator.addObjEntityDisplayListener(this);
        mediator.addObjEntityListener(this);
        mediator.addObjRelationshipListener(this);

        resolve.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                if (row < 0) {
                    return;
                }

                ObjRelationshipTableModel model = (ObjRelationshipTableModel) table
                        .getModel();
                new ObjRelationshipInfoController(mediator, model.getRelationship(row))
                        .startup();

                // need to refresh selected row... do this by unselecting/selecting the
                // row
                table.getSelectionModel().clearSelection();
                table.select(row);
            }
        });

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                processExistingSelection();
            }
        });
    }

    /**
     * Selects a specified relationship in the relationships table.
     */
    public void selectRelationship(ObjRelationship rel) {
        if (rel == null) {
            return;
        }

        ObjRelationshipTableModel model = (ObjRelationshipTableModel) table.getModel();
        java.util.List rels = model.getObjectList();
        int relPos = rels.indexOf(rel);
        if (relPos >= 0) {
            table.select(relPos);
        }
    }

    public void processExistingSelection() {
        ObjRelationship rel = null;
        if (table.getSelectedRow() >= 0) {
            ObjRelationshipTableModel model = (ObjRelationshipTableModel) table
                    .getModel();
            rel = model.getRelationship(table.getSelectedRow());
            if (rel.getTargetEntity() != null
                    && ((ObjEntity) rel.getSourceEntity()).getDbEntity() != null
                    && ((ObjEntity) rel.getTargetEntity()).getDbEntity() != null) {
                resolve.setEnabled(true);
            }
            else
                resolve.setEnabled(false);

            // scroll table
            UIUtil.scrollToSelectedRow(table);
        }
        else
            resolve.setEnabled(false);

        RelationshipDisplayEvent ev = new RelationshipDisplayEvent(this, rel, mediator
                .getCurrentObjEntity(), mediator.getCurrentDataMap(), mediator
                .getCurrentDataDomain());
        mediator.fireObjRelationshipDisplayEvent(ev);
    }

    /** Loads obj relationships into table. */
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

    /**
     * Creates a list of ObjEntity names.
     */
    private Object[] createObjEntityComboModel() {
        DataMap map = mediator.getCurrentDataMap();
        
        // this actually happens per CAY-221... can't reproduce though
        if(map == null) {
            logObj.warn("createObjEntityComboModel:: Null DataMap.");
            return new Object[0];
        }
        
        if(map.getNamespace() == null) {
            logObj.warn("createObjEntityComboModel:: Null DataMap namespace - " + map);
            return new Object[0];
        }
        
        Collection objEntities = map.getNamespace().getObjEntities();
        return objEntities.toArray();
    }

    public void objEntityChanged(EntityEvent e) {
    }

    public void objEntityAdded(EntityEvent e) {
        reloadEntityList(e);
    }

    public void objEntityRemoved(EntityEvent e) {
        reloadEntityList(e);
    }

    public void objRelationshipChanged(RelationshipEvent e) {
        table.select(e.getRelationship());
    }

    public void objRelationshipAdded(RelationshipEvent e) {
        rebuildTable((ObjEntity) e.getEntity());
        table.select(e.getRelationship());
    }

    public void objRelationshipRemoved(RelationshipEvent e) {
        ObjRelationshipTableModel model = (ObjRelationshipTableModel) table.getModel();
        int ind = model.getObjectList().indexOf(e.getRelationship());
        model.removeRow(e.getRelationship());
        table.select(ind);
    }

    /**
     * Refresh the list of ObjEntity targets. Also refresh the table in case some
     * ObjRelationships were deleted.
     */
    private void reloadEntityList(EntityEvent e) {
        if (e.getSource() == this)
            return;

        // If current model added/removed, do nothing.
        ObjEntity entity = mediator.getCurrentObjEntity();
        if (entity == e.getEntity() || entity == null) {
            return;
        }

        TableColumn col = table.getColumnModel().getColumn(
                ObjRelationshipTableModel.REL_TARGET);
        DefaultCellEditor editor = (DefaultCellEditor) col.getCellEditor();

        JComboBox combo = (JComboBox) editor.getComponent();
        combo.setRenderer(CellRenderers.entityListRendererWithIcons(entity.getDataMap()));
        combo.setModel(new DefaultComboBoxModel(createObjEntityComboModel()));

        ObjRelationshipTableModel model = (ObjRelationshipTableModel) table.getModel();
        model.fireTableDataChanged();
    }

    protected void rebuildTable(ObjEntity entity) {
        final ObjRelationshipTableModel model = new ObjRelationshipTableModel(
                entity,
                mediator,
                this);

        model.addTableModelListener(new TableModelListener() {

            public void tableChanged(TableModelEvent e) {
                if (table.getSelectedRow() >= 0) {
                    ObjRelationship rel = model.getRelationship(table.getSelectedRow());
                    if (rel.getTargetEntity() != null
                            && ((ObjEntity) rel.getSourceEntity()).getDbEntity() != null
                            && ((ObjEntity) rel.getTargetEntity()).getDbEntity() != null) {
                        resolve.setEnabled(true);
                    }
                    else
                        resolve.setEnabled(false);
                }
            }
        });

        table.setModel(model);
        table.setRowHeight(25);
        table.setRowMargin(3);

        TableColumn lockColumn = table.getColumnModel().getColumn(
                ObjRelationshipTableModel.REL_LOCKING);
        lockColumn.setMinWidth(100);

        TableColumn col = table.getColumnModel().getColumn(
                ObjRelationshipTableModel.REL_NAME);
        col.setMinWidth(150);

        col = table.getColumnModel().getColumn(ObjRelationshipTableModel.REL_TARGET);
        col.setMinWidth(150);
        JComboBox targetCombo = CayenneWidgetFactory.createComboBox(
                createObjEntityComboModel(),
                false);
        targetCombo.setRenderer(CellRenderers.entityListRendererWithIcons(entity
                .getDataMap()));
        targetCombo.setEditable(false);
        targetCombo.setSelectedIndex(-1);
        DefaultCellEditor editor = new DefaultCellEditor(targetCombo);
        editor.setClickCountToStart(1);
        col.setCellEditor(editor);

        col = table.getColumnModel().getColumn(ObjRelationshipTableModel.REL_CARDINALITY);
        col.setMinWidth(150);

        col = table.getColumnModel().getColumn(ObjRelationshipTableModel.REL_DELETERULE);
        col.setMinWidth(60);
        JComboBox deleteRulesCombo = CayenneWidgetFactory.createComboBox(
                deleteRules,
                false);
        deleteRulesCombo.setEditable(false);
        deleteRulesCombo.setSelectedIndex(0); //Default to the first value
        editor = new DefaultCellEditor(deleteRulesCombo);
        editor.setClickCountToStart(1);
        col.setCellEditor(editor);
    }

    class EntityRenderer extends StringRenderer {

        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            if (value instanceof MapObject) {
                MapObject mapObject = (MapObject) value;
                String label = mapObject.getName();

                if (mapObject instanceof Entity) {
                    Entity entity = (Entity) mapObject;

                    // for different namespace display its name
                    DataMap dataMap = entity.getDataMap();
                    if (dataMap != null && dataMap != mediator.getCurrentDataMap()) {
                        label += " (" + dataMap.getName() + ")";
                    }
                }

                value = label;
            }

            return super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column);
        }
    }

    class StringRenderer extends DefaultTableCellRenderer {

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

            ObjRelationshipTableModel model = (ObjRelationshipTableModel) table
                    .getModel();
            ObjRelationship relationship = model.getRelationship(row);

            if (relationship != null
                    && relationship.getSourceEntity() != model.getEntity()) {
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
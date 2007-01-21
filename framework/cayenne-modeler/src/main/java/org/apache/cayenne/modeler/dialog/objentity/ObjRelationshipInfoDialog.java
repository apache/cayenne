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

package org.apache.cayenne.modeler.dialog.objentity;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import org.apache.cayenne.modeler.util.PanelFactory;
import org.scopemvc.core.Selector;
import org.scopemvc.view.swing.SAction;
import org.scopemvc.view.swing.SButton;
import org.scopemvc.view.swing.SComboBox;
import org.scopemvc.view.swing.SLabel;
import org.scopemvc.view.swing.SListCellRenderer;
import org.scopemvc.view.swing.SPanel;
import org.scopemvc.view.swing.STable;
import org.scopemvc.view.swing.STableModel;
import org.scopemvc.view.swing.STextField;
import org.scopemvc.view.swing.SwingView;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A view of the dialog for mapping an ObjRelationship to one or more
 * DbRelationships.
 * 
 * @since 1.1
 * @author Andrus Adamchik
 */
public class ObjRelationshipInfoDialog extends SPanel {

    protected STable pathTable;

    public ObjRelationshipInfoDialog() {
        init();
    }

    protected void init() {
        // create widgets 
        SButton saveButton =
            new SButton(new SAction(ObjRelationshipInfoController.SAVE_CONTROL));
        saveButton.setEnabled(true);

        SButton cancelButton =
            new SButton(new SAction(ObjRelationshipInfoController.CANCEL_CONTROL));
        cancelButton.setEnabled(true);

        SButton newToOneButton =
            new SButton(new SAction(ObjRelationshipInfoController.NEW_TOONE_CONTROL));
        newToOneButton.setEnabled(true);
        SButton newToManyButton =
            new SButton(new SAction(ObjRelationshipInfoController.NEW_TOMANY_CONTROL));
        newToManyButton.setEnabled(true);

        STextField relationshipName = new STextField(25);
        relationshipName.setSelector(ObjRelationshipInfoModel.RELATIONSHIP_NAME_SELECTOR);

        SLabel sourceEntityLabel = new SLabel();
        sourceEntityLabel.setSelector(
            ObjRelationshipInfoModel.SOURCE_ENTITY_NAME_SELECTOR);

        SComboBox targetCombo = new SComboBox();
        targetCombo.setSelector(ObjRelationshipInfoModel.OBJECT_TARGETS_SELECTOR);
        targetCombo.setSelectionSelector(ObjRelationshipInfoModel.OBJECT_TARGET_SELECTOR);
        SListCellRenderer renderer = (SListCellRenderer) targetCombo.getRenderer();
        renderer.setTextSelector("name");

        pathTable = new ObjRelationshipPathTable();
        STableModel pathTableModel = new STableModel(pathTable);
        pathTableModel.setSelector(
            ObjRelationshipInfoModel.DB_RELATIONSHIP_PATH_SELECTOR);
        pathTableModel.setColumnNames(new String[] { "DbRelationships" });
        pathTableModel.setColumnSelectors(
            new Selector[] {
                 EntityRelationshipsModel.RELATIONSHIP_DISPLAY_NAME_SELECTOR });

        pathTable.setModel(pathTableModel);
        pathTable.setSelectionSelector(
            ObjRelationshipInfoModel.SELECTED_PATH_COMPONENT_SELECTOR);
        pathTable.getColumn("DbRelationships").setCellEditor(
            RelationshipPicker.createEditor(this));

        // assemble
        setDisplayMode(SwingView.MODAL_DIALOG);
        setTitle("ObjRelationship Inspector");
        setLayout(new BorderLayout());

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder =
            new PanelBuilder(
                new FormLayout(
                    "right:max(50dlu;pref), 3dlu, fill:min(150dlu;pref), 3dlu, fill:min(150dlu;pref)",
                    "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, top:14dlu, 3dlu, top:p:grow"));
        builder.setDefaultDialogBorder();

        builder.addSeparator("ObjRelationship Information", cc.xywh(1, 1, 5, 1));
        builder.addLabel("Relationship:", cc.xy(1, 3));
        builder.add(relationshipName, cc.xywh(3, 3, 1, 1));
        builder.addLabel("Source:", cc.xy(1, 5));
        builder.add(sourceEntityLabel, cc.xywh(3, 5, 1, 1));
        builder.addLabel("Target:", cc.xy(1, 7));
        builder.add(targetCombo, cc.xywh(3, 7, 1, 1));

        builder.addSeparator("Mapping to DbRelationships", cc.xywh(1, 9, 5, 1));
        builder.add(new JScrollPane(pathTable), cc.xywh(1, 11, 3, 3));
        builder.add(newToOneButton, cc.xywh(5, 11, 1, 1));
        builder.add(newToManyButton, cc.xywh(5, 13, 1, 1));

        add(builder.getPanel(), BorderLayout.CENTER);
        add(
            PanelFactory.createButtonPanel(new JButton[] { saveButton, cancelButton }),
            BorderLayout.SOUTH);
    }

    /**
     * Cancels any editing that might be going on in the path table.
     */
    public void cancelTableEditing() {
        int row = pathTable.getEditingRow();
        if (row < 0) {
            return;
        }

        int column = pathTable.getEditingColumn();
        if (column < 0) {
            return;
        }

        TableCellEditor editor = pathTable.getCellEditor(row, column);
        if (editor != null) {
            editor.cancelCellEditing();
        }
    }

    class ObjRelationshipPathTable extends STable {
        final Dimension preferredSize = new Dimension(203, 100);

        ObjRelationshipPathTable() {
            setRowHeight(25);
            setRowMargin(3);
        }

        public Dimension getPreferredScrollableViewportSize() {
            return preferredSize;
        }
    }

    static final class RelationshipPicker extends DefaultCellEditor {
        JComboBox comboBox;
        SwingView view;

        static TableCellEditor createEditor(SwingView view) {
            JComboBox relationshipCombo = new JComboBox();
            relationshipCombo.setEditable(false);
            return new RelationshipPicker(view, relationshipCombo);
        }

        RelationshipPicker(SwingView view, JComboBox comboBox) {
            super(comboBox);
            this.comboBox = comboBox;
            this.view = view;
        }

        public Component getTableCellEditorComponent(
            JTable table,
            Object value,
            boolean isSelected,
            int row,
            int column) {

            // initialize combo box
            ObjRelationshipInfoModel model =
                (ObjRelationshipInfoModel) view.getBoundModel();

            EntityRelationshipsModel relationshipWrapper =
                (EntityRelationshipsModel) model.getDbRelationshipPath().get(row);

            DefaultComboBoxModel comboModel =
                new DefaultComboBoxModel(relationshipWrapper.getRelationshipNames());
            comboModel.setSelectedItem(value);
            comboBox.setModel(comboModel);

            // call super
            return super.getTableCellEditorComponent(
                table,
                value,
                isSelected,
                row,
                column);
        }
    }
}

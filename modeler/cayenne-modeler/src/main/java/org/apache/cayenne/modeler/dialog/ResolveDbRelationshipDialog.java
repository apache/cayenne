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

package org.apache.cayenne.modeler.dialog;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.pref.TableColumnPreferences;
import org.apache.cayenne.modeler.undo.RelationshipUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneDialog;
import org.apache.cayenne.modeler.util.CayenneTable;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.util.PanelFactory;
import org.apache.cayenne.modeler.util.combo.AutoCompletion;
import org.apache.cayenne.util.Util;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Editor of DbRelationship joins.
 */
public class ResolveDbRelationshipDialog extends CayenneDialog {

    protected DbRelationship relationship;
    protected DbRelationship reverseRelationship;

    protected JLabel sourceName;
    protected JLabel targetName;
    protected JTextField name;
    protected JTextField reverseName;
    protected CayenneTable table;
    protected TableColumnPreferences tablePreferences;
    protected JButton addButton;
    protected JButton removeButton;
    protected JButton saveButton;
    protected JButton cancelButton;

    private boolean cancelPressed;

    private RelationshipUndoableEdit undo;

    private boolean editable;

    public ResolveDbRelationshipDialog(DbRelationship relationship) {
        this(relationship, true);
    }

    public ResolveDbRelationshipDialog(DbRelationship relationship, boolean editable) {
        super(Application.getFrame(), "DbRelationship Inspector", true);
        this.editable = editable;

        if(!validateAndSetRelationship(relationship)) {
            this.cancelPressed = true;
            return;
        }

        initView();
        initController();
        initWithModel();

        this.undo = new RelationshipUndoableEdit(relationship);

        this.pack();
        this.centerWindow();

    }

    @Override
    public void setVisible(boolean b) {
        if(b && cancelPressed) {
            return;
        }
        super.setVisible(b);
    }

    /**
     * Creates graphical components.
     */
    private void initView() {

        // create widgets
        sourceName = new JLabel();
        targetName = new JLabel();
        name = new JTextField(25);
        reverseName = new JTextField(25);

        addButton = new JButton("Add");
        addButton.setEnabled(this.editable);

        removeButton = new JButton("Remove");
        removeButton.setEnabled(this.editable);

        saveButton = new JButton("Done");

        cancelButton = new JButton("Cancel");
        cancelButton.setEnabled(this.editable);

        table = new AttributeTable();

        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablePreferences = new TableColumnPreferences(getClass(), "dbentity/dbjoinTable");

        getRootPane().setDefaultButton(saveButton);

        // assemble
        getContentPane().setLayout(new BorderLayout());

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(
                new FormLayout(
                        "right:max(50dlu;pref), 3dlu, fill:min(150dlu;pref), 3dlu, fill:min(50dlu;pref)",
                        "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 9dlu, p, 3dlu, top:14dlu, 3dlu, top:p:grow"));
        builder.setDefaultDialogBorder();

        builder.addSeparator("DbRelationship Information", cc.xywh(1, 1, 5, 1));

        builder.addLabel("Source Entity:", cc.xy(1, 3));
        builder.add(sourceName, cc.xywh(3, 3, 1, 1));

        builder.addLabel("Target Entity:", cc.xy(1, 5));
        builder.add(targetName, cc.xywh(3, 5, 1, 1));

        builder.addLabel("Relationship Name:", cc.xy(1, 7));
        builder.add(name, cc.xywh(3, 7, 1, 1));

        builder.addLabel("Reverse Relationship Name:", cc.xy(1, 9));
        builder.add(reverseName, cc.xywh(3, 9, 1, 1));

        builder.addSeparator("Joins", cc.xywh(1, 11, 5, 1));
        builder.add(new JScrollPane(table), cc.xywh(1, 13, 3, 3, "fill, fill"));

        JPanel joinButtons = new JPanel(new FlowLayout(FlowLayout.LEADING));
        joinButtons.add(addButton);
        joinButtons.add(removeButton);

        builder.add(joinButtons, cc.xywh(5, 13, 1, 3));

        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        JButton[] buttons = {cancelButton, saveButton};
        getContentPane().add(PanelFactory.createButtonPanel(buttons), BorderLayout.SOUTH);
    }

    private boolean validateAndSetRelationship(DbRelationship relationship) {
        this.relationship = relationship;
        this.reverseRelationship = relationship.getReverseRelationship();
        // sanity check
        if (relationship.getSourceEntity() == null) {
            throw new CayenneRuntimeException("Null source entity: %s", relationship);
        }
        if (relationship.getSourceEntity().getDataMap() == null) {
            throw new CayenneRuntimeException("Null DataMap: %s", relationship.getSourceEntity());
        }

        // warn if no target entity
        if (relationship.getTargetEntity() == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select target DbEntity first",
                    "Select target",
                    JOptionPane.INFORMATION_MESSAGE);
            return false;
        }

        return true;
    }

    private void initWithModel() {
        // init UI components
        sourceName.setText(relationship.getSourceEntityName());
        targetName.setText(relationship.getTargetEntityName());
        name.setText(relationship.getName());
        if (reverseRelationship != null) {
            reverseName.setText(reverseRelationship.getName());
        }

        table.setModel(new DbJoinTableModel(relationship, getMediator(), this, true));
        TableColumn sourceColumn = table.getColumnModel().getColumn(DbJoinTableModel.SOURCE);
        JComboBox comboBox = Application.getWidgetFactory().createComboBox(
                ModelerUtil.getDbAttributeNames(relationship.getSourceEntity()), true);

        AutoCompletion.enable(comboBox);
        sourceColumn.setCellEditor(Application.getWidgetFactory().createCellEditor(comboBox));

        TableColumn targetColumn = table.getColumnModel().getColumn(DbJoinTableModel.TARGET);
        comboBox = Application.getWidgetFactory().createComboBox(
                ModelerUtil.getDbAttributeNames(relationship.getTargetEntity()), true);
        AutoCompletion.enable(comboBox);

        targetColumn.setCellEditor(Application.getWidgetFactory().createCellEditor(comboBox));

        tablePreferences.bind(table, null, null, null, DbJoinTableModel.SOURCE, true);
    }

    private void initController() {
        addButton.addActionListener(e -> {
            DbJoinTableModel model = (DbJoinTableModel) table.getModel();

            DbJoin join = new DbJoin(relationship);
            model.addRow(join);

            undo.addDbJoinAddUndo(join);

            table.select(model.getRowCount() - 1);
        });

        removeButton.addActionListener(e -> {
            DbJoinTableModel model = (DbJoinTableModel) table.getModel();
            stopEditing();
            int row = table.getSelectedRow();

            DbJoin join = model.getJoin(row);
            undo.addDbJoinRemoveUndo(join);

            model.removeRow(join);
        });

        saveButton.addActionListener(e -> {
            cancelPressed = false;

            if (editable) {
                save();
            }

            dispose();
        });

        cancelButton.addActionListener(e -> {
            cancelPressed = true;
            setVisible(false);
        });
    }

    public boolean isCancelPressed() {
        return cancelPressed;
    }

    private void stopEditing() {
        // Stop whatever editing may be taking place
        int col_index = table.getEditingColumn();
        if (col_index >= 0) {
            TableColumn col = table.getColumnModel().getColumn(col_index);
            col.getCellEditor().stopCellEditing();
        }
    }

    private void save() {
        stopEditing();

        DbJoinTableModel model = (DbJoinTableModel) table.getModel();
        boolean updatingReverse = model.getObjectList().size() > 0;

        // handle name update
        handleNameUpdate(relationship, name.getText().trim());

        model.commit();

        // check "to dep pk" setting,
        // maybe this is no longer valid
        if (relationship.isToDependentPK() && !relationship.isValidForDepPk()) {
            relationship.setToDependentPK(false);
        }

        // If new reverse DbRelationship was created, add it to the target
        // Don't create reverse with no joins - makes no sense...
        if (updatingReverse) {

            // If didn't find anything, create reverseDbRel
            if (reverseRelationship == null) {
                reverseRelationship = new DbRelationship();
                reverseRelationship.setName(NameBuilder
                        .builder(reverseRelationship, relationship.getTargetEntity())
                        .baseName(reverseName.getText().trim())
                        .name());

                reverseRelationship.setSourceEntity(relationship.getTargetEntity());
                reverseRelationship.setTargetEntityName(relationship.getSourceEntity());
                reverseRelationship.setToMany(!relationship.isToMany());
                relationship.getTargetEntity().addRelationship(reverseRelationship);

                // fire only if the relationship is to the same entity...
                // this is needed to update entity view...
                if (relationship.getSourceEntity() == relationship.getTargetEntity()) {
                    getMediator().fireDbRelationshipEvent(
                            new RelationshipEvent(
                                    this,
                                    reverseRelationship,
                                    reverseRelationship.getSourceEntity(),
                                    MapEvent.ADD));
                }
            } else {
                handleNameUpdate(reverseRelationship, reverseName.getText().trim());
            }

            Collection<DbJoin> reverseJoins = getReverseJoins();
            reverseRelationship.setJoins(reverseJoins);

            // check if joins map to a primary key of this entity
            if (!relationship.isToDependentPK() && reverseRelationship.isValidForDepPk()) {
                reverseRelationship.setToDependentPK(true);
            }
        }

        Application.getInstance().getUndoManager().addEdit(undo);

        getMediator().fireDbRelationshipEvent(
                new RelationshipEvent(this, relationship, relationship.getSourceEntity()));
    }

    private void handleNameUpdate(DbRelationship relationship, String userInputName) {
        if(Util.nullSafeEquals(relationship.getName(), userInputName)) {
            return;
        }

        String sourceEntityName = NameBuilder
                .builder(relationship, relationship.getSourceEntity())
                .baseName(userInputName)
                .name();

        if (Util.nullSafeEquals(sourceEntityName, relationship.getName())) {
            return;
        }
        String oldName = relationship.getName();
        relationship.setName(sourceEntityName);
        undo.addNameUndo(relationship, oldName, sourceEntityName);

        getMediator().fireDbRelationshipEvent(
                new RelationshipEvent(this, relationship, relationship.getSourceEntity(), oldName));
    }

    private Collection<DbJoin> getReverseJoins() {
        Collection<DbJoin> joins = relationship.getJoins();

        if ((joins == null) || (joins.size() == 0)) {
            return Collections.emptyList();
        }

        List<DbJoin> reverseJoins = new ArrayList<>(joins.size());

        // Loop through the list of attribute pairs, create reverse pairs
        // and put them to the reverse list.
        for (DbJoin pair : joins) {
            DbJoin reverseJoin = pair.createReverseJoin();

            // since reverse relationship is not yet initialized,
            // reverse join will not have it set automatically
            reverseJoin.setRelationship(reverseRelationship);
            reverseJoins.add(reverseJoin);
        }

        return reverseJoins;
    }

    final class AttributeTable extends CayenneTable {

        final Dimension preferredSize = new Dimension(203, 100);

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return preferredSize;
        }
    }
}

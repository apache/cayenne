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

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.CustomExpDbJoin;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.DbRelationshipDialogView;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.RelationshipDisplayEvent;
import org.apache.cayenne.modeler.undo.CreateRelationshipUndoableEdit;
import org.apache.cayenne.modeler.undo.RelationshipUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.util.combo.AutoCompletion;
import org.apache.cayenne.project.extension.info.ObjectInfo;
import org.apache.cayenne.util.Util;

/**
 * @since 4.2
 */
public class DbRelationshipDialog extends CayenneController {

    private DbRelationship relationship;
    private DbRelationship reverseRelationship;

    private DbRelationshipDialogView view;

    private boolean isCreate = false;

    private ProjectController projectController;

    private RelationshipUndoableEdit undo;

    public DbRelationshipDialog(ProjectController projectController) {
        this.view = new DbRelationshipDialogView();
        this.projectController = projectController;
    }

    @Override
    public Component getView() {
        return view;
    }

    public DbRelationshipDialog createNewRaltionship(DbEntity dbEntity) {
        isCreate = true;

        DbRelationship rel = new DbRelationship();
        rel.setName(NameBuilder.builder(rel, dbEntity).name());
        rel.setSourceEntity(dbEntity);

        return modifyRaltionship(rel);
    }

    public DbRelationshipDialog modifyRaltionship(DbRelationship dbRelationship) {
        this.undo = new RelationshipUndoableEdit(dbRelationship);

        this.relationship = dbRelationship;
        this.reverseRelationship = this.relationship.getReverseRelationship();

        if (relationship.getSourceEntity() == null) {
            throw new CayenneRuntimeException("Null source entity: %s", relationship);
        }
        if (relationship.getSourceEntity().getDataMap() == null) {
            throw new CayenneRuntimeException("Null DataMap: %s", relationship.getSourceEntity());
        }

        initController();
        initFromModel();

        return this;
    }

    public void startUp() {
        view.setVisible(true);
        view.dispose();
    }

    private void initFromModel() {
        TargetComboBoxModel targetComboBoxModel = new TargetComboBoxModel(relationship
                .getSourceEntity().getDataMap().getDbEntities());
        view.getTargetEntities().setModel(targetComboBoxModel);

        view.getSourceName().setText(relationship.getSourceEntityName());
        view.getToDepPk().setSelected(relationship.isToDependentPK());
        view.getToMany().setSelected(relationship.isToMany());

        view.getNameField().setText(relationship.getName());
        if(reverseRelationship != null) {
            view.getReverseName().setText(reverseRelationship.getName());
        }

        if(relationship.getTargetEntity() == null) {
            enableOptions(false);
        } else {
            enableInfo();
        }

        view.getComment().setText(ObjectInfo
                .getFromMetaData(projectController.getApplication().getMetaData(),
                        relationship,
                        ObjectInfo.COMMENT));

        for(DbJoin dbJoin : relationship.getJoins()) {
            if(dbJoin instanceof CustomExpDbJoin) {
                view.getUseExpressionForJoin().setSelected(true);
                view.getCustomExpressionField().setText(((CustomExpDbJoin)dbJoin).getJoinExpression().toString());

                view.showExpressionField(true);
            }
        }
    }

    private void initController() {
        view.getTargetEntities().addActionListener(action -> {
            String selectedItem = (String)view.getTargetEntities().getSelectedItem();
            if(relationship.getTargetEntityName() == null) {
                relationship.setTargetEntityName(selectedItem);
            } else if(!relationship.getTargetEntityName().equals(selectedItem)){
                if (WarningDialogByDbTargetChange.showWarningDialog(projectController, relationship)) {
                    // clear joins...
                    relationship.removeAllJoins();
                    relationship.setTargetEntityName(selectedItem);
                } else {
                    view.getTargetEntities().setSelectedItem(relationship.getTargetEntityName());
                }
                relationship.setToDependentPK(false);
                view.getToDepPk().setSelected(relationship.isValidForDepPk());
                projectController.fireDbRelationshipEvent(new RelationshipEvent(this, relationship, relationship.getSourceEntity()));
            }
            enableInfo();
        });

        view.getAddButton().addActionListener(e -> {
            DbJoinTableModel model = (DbJoinTableModel) view.getTable().getModel();

            DbJoin join = new DbJoin(relationship);
            relationship.addJoin(join);
            model.addRow(join);

            view.getTable().select(model.getRowCount() - 1);
        });

        view.getRemoveButton().addActionListener(e -> {
            DbJoinTableModel model = (DbJoinTableModel) view.getTable().getModel();
            stopEditing();
            int row = view.getTable().getSelectedRow();

            DbJoin join = model.getJoin(row);

            relationship.removeJoin(join);
            if(relationship.isValidForDepPk()) {
                view.getToDepPk().setEnabled(true);
            } else {
                view.getToDepPk().setEnabled(false);
                view.getToDepPk().setSelected(false);
                relationship.setToDependentPK(false);
            }

            model.removeRow(join);
        });

        view.getSaveButton().addActionListener(e -> {
            view.setCancelPressed(false);
            save();
            view.dispose();
            view.setVisible(false);
        });

        view.getCancelButton().addActionListener(e -> {
            view.setCancelPressed(true);
            view.setVisible(false);
        });

        view.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e) {
                view.setCancelPressed(true);
            }
        });

        view.getToDepPk().setEnabled(relationship.isValidForDepPk());
        view.getToDepPk().addActionListener(selected -> {
            boolean isSelected = view.getToDepPk().isSelected();
            DbRelationship reverseRelationship = relationship.getReverseRelationship();
            if(reverseRelationship != null && reverseRelationship.isToDependentPK() && isSelected) {
                boolean setToDepPk = JOptionPane.showConfirmDialog(Application.getFrame(), "Unset reverse relationship's \"To Dep PK\" setting?",
                        "Warning", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
                relationship.setToDependentPK(setToDepPk);
                reverseRelationship.setToDependentPK(!setToDepPk);
            } else {
                relationship.setToDependentPK(view.getToDepPk().isSelected());
            }
        });

        view.getUseExpressionForJoin().addActionListener(select -> {
            if(view.getUseExpressionForJoin().isSelected()) {
                view.showExpressionField(true);
            } else {
                view.showExpressionField(false);
            }
        });
    }

    private void enableInfo() {
        enableOptions(true);

        view.getTable().setModel(new DbJoinTableModel(relationship,
                projectController, this, true));

        view.getTable().getModel().addTableModelListener(change -> {
            if(change.getLastRow() != Integer.MAX_VALUE) {
                if(relationship.isValidForDepPk()) {
                    view.getToDepPk().setEnabled(true);
                } else {
                    view.getToDepPk().setEnabled(false);
                }
            }
        });

        TableColumn sourceColumn = view.getTable().getColumnModel().getColumn(DbJoinTableModel.SOURCE);
        JComboBox comboBox = Application.getWidgetFactory().createComboBox(
                ModelerUtil.getDbAttributeNames(relationship.getSourceEntity()), true);

        AutoCompletion.enable(comboBox);
        sourceColumn.setCellEditor(Application.getWidgetFactory().createCellEditor(comboBox));

        TableColumn targetColumn = view.getTable().getColumnModel().getColumn(DbJoinTableModel.TARGET);
        comboBox = Application.getWidgetFactory().createComboBox(
                ModelerUtil.getDbAttributeNames(relationship.getTargetEntity()), true);
        AutoCompletion.enable(comboBox);

        targetColumn.setCellEditor(Application.getWidgetFactory().createCellEditor(comboBox));

        view.getTablePreferences().bind(view.getTable(), null, null, null, DbJoinTableModel.SOURCE, true);
    }

    private void enableOptions(boolean enable) {
        view.enableOptions(enable);
    }

    private void stopEditing() {
        // Stop whatever editing may be taking place
        int col_index = view.getTable().getEditingColumn();
        if (col_index >= 0) {
            TableColumn col = view.getTable().getColumnModel().getColumn(col_index);
            col.getCellEditor().stopCellEditing();
        }
    }

    private void save() {
        stopEditing();

        DbJoinTableModel model = (DbJoinTableModel) view.getTable().getModel();
        boolean updatingReverse = model.getObjectList().size() > 0 ||
                !view.getCustomExpressionField().getText().isEmpty();

        // handle name update
        handleNameUpdate(relationship, view.getNameField().getText().trim());

        model.commit();

        relationship.setToMany(view.getToMany().isSelected());

        ObjectInfo.putToMetaData(projectController.getApplication().getMetaData(),
                relationship,
                ObjectInfo.COMMENT, view.getComment().getText());

        if(view.getUseExpressionForJoin().isSelected()) {
            relationship.setUseJoinExp(true);
            relationship.removeAllJoins();
            DbJoin dbJoin = new CustomExpDbJoin(relationship,
                    ExpressionFactory.exp(view.getCustomExpressionField().getText()));
            relationship.addJoin(dbJoin);
        } else {
            relationship.setUseJoinExp(false);
            relationship.getJoins().removeIf(dbJoin -> dbJoin instanceof CustomExpDbJoin);
        }

        // If new reverse DbRelationship was created, add it to the target
        // Don't create reverse with no joins - makes no sense...
        if (updatingReverse) {

            // If didn't find anything, create reverseDbRel
            if (reverseRelationship == null) {
                reverseRelationship = new DbRelationship();
                reverseRelationship.setName(NameBuilder
                        .builder(reverseRelationship, relationship.getTargetEntity())
                        .baseName(view.getReverseName().getText().trim())
                        .name());

                reverseRelationship.setSourceEntity(relationship.getTargetEntity());
                reverseRelationship.setTargetEntityName(relationship.getSourceEntity());
                reverseRelationship.setToMany(!relationship.isToMany());
                relationship.getTargetEntity().addRelationship(reverseRelationship);

                // fire only if the relationship is to the same entity...
                // this is needed to update entity view...
                if (relationship.getSourceEntity() == relationship.getTargetEntity()) {
                    projectController
                            .fireDbRelationshipEvent(
                            new RelationshipEvent(
                                    this,
                                    reverseRelationship,
                                    reverseRelationship.getSourceEntity(),
                                    MapEvent.ADD));
                }
            } else {
                handleNameUpdate(reverseRelationship, view.getReverseName().getText().trim());
            }

            Collection<DbJoin> reverseJoins = getReverseJoins();
            reverseRelationship.setJoins(reverseJoins);

            // check if joins map to a primary key of this entity
            if (!relationship.isToDependentPK() && reverseRelationship.isValidForDepPk()) {
                reverseRelationship.setToDependentPK(true);
            }
        }

        fireDbRelationshipEvent(isCreate);
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

        projectController
                .fireDbRelationshipEvent(
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

    private void fireDbRelationshipEvent(boolean isCreate) {
        if(!isCreate) {
            projectController
                    .fireDbRelationshipEvent(
                            new RelationshipEvent(this, relationship, relationship.getSourceEntity(), MapEvent.CHANGE));

            Application.getInstance().getUndoManager().addEdit(undo);
        } else {
            DbEntity dbEntity = relationship.getSourceEntity();
            if(dbEntity.getRelationship(relationship.getName()) == null) {
                dbEntity.addRelationship(relationship);
            }

            projectController.fireDbRelationshipEvent(new RelationshipEvent(this, relationship, dbEntity, MapEvent.ADD));

            RelationshipDisplayEvent rde = new RelationshipDisplayEvent(this, relationship, dbEntity, projectController.getCurrentDataMap(),
                    (DataChannelDescriptor) projectController.getProject().getRootNode());

            projectController.fireDbRelationshipDisplayEvent(rde);

            Application.getInstance().getUndoManager().addEdit(
                    new CreateRelationshipUndoableEdit(relationship.getSourceEntity(), new DbRelationship[]{relationship}));
        }
    }

    public Optional<DbRelationship> getRelationship() {
        return view.isCancelPressed() ? Optional.empty() : Optional.of(relationship);
    }

    final class TargetComboBoxModel extends DefaultComboBoxModel<String> {

        TargetComboBoxModel(Collection<DbEntity> dbEntities) {
            super();
            dbEntities.forEach(dbEntity -> this.addElement(dbEntity.getName()));
            if(relationship.getTargetEntity() == null) {
                this.setSelectedItem(null);
            } else {
                this.setSelectedItem(relationship.getTargetEntity().getName());
            }
        }

    }
}

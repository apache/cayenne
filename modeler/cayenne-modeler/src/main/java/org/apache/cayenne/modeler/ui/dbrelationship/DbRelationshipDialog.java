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

package org.apache.cayenne.modeler.ui.dbrelationship;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.modeler.event.display.DbRelationshipDisplayEvent;
import org.apache.cayenne.modeler.event.model.DbRelationshipEvent;
import org.apache.cayenne.modeler.pref.adapters.CMTablePrefs;
import org.apache.cayenne.modeler.project.DbRelationshipOps;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.toolkit.ProjectDialog;
import org.apache.cayenne.modeler.toolkit.buttons.CMButtonPanel;
import org.apache.cayenne.modeler.toolkit.combobox.AutoCompletion;
import org.apache.cayenne.modeler.toolkit.combobox.CMAutoCompleteComboBoxCellEditor;
import org.apache.cayenne.modeler.toolkit.combobox.CMComboBox;
import org.apache.cayenne.modeler.toolkit.table.CMTable;
import org.apache.cayenne.modeler.undo.CreateDbRelationshipUndoableEdit;
import org.apache.cayenne.modeler.undo.DbRelationshipUndoableEdit;
import org.apache.cayenne.project.extension.info.ObjectInfo;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Modal dialog for creating or editing a {@link DbRelationship} — name, target entity,
 * cardinality and dep-PK flags, and an editable join table.
 */
public class DbRelationshipDialog extends ProjectDialog {

    private static final Comparator<DbEntity> DB_ENTITY_COMPARATOR = Comparator
            .comparing((Function<DbEntity, String>) ent -> ent.getDataMap().getName())
            .thenComparing(DbEntity::getName);

    private final JTextField name;
    private final JComboBox<String> targetEntities;
    private final JCheckBox toDepPk;
    private final JCheckBox toMany;
    private final JTextField comment;
    private final JLabel sourceName;
    private final JTextField reverseName;
    private final CMTable table;
    private final JButton addButton;
    private final JButton removeButton;
    private final JButton saveButton;
    private final JButton cancelButton;

    private final DbRelationship relationship;
    private final DbRelationship reverseRelationship;
    private final boolean create;
    private boolean saved;
    private final DbRelationshipUndoableEdit undo;

    // dialog-local editing state, applied to the relationship only on save
    private DbEntity currentTarget;
    private boolean unsetReverseDepPk;

    public static Optional<DbRelationship> openForCreate(ProjectSession session, Window owner, DbEntity sourceEntity) {
        DbRelationship relationship = new DbRelationship();
        relationship.setName(NameBuilder.builder(relationship, sourceEntity).name());
        relationship.setSourceEntity(sourceEntity);

        DbRelationshipDialog dialog = new DbRelationshipDialog(session, owner, relationship, true);
        dialog.open();
        return dialog.saved ? Optional.of(relationship) : Optional.empty();
    }

    public static void openForEdit(ProjectSession session, Window owner, DbRelationship relationship) {
        new DbRelationshipDialog(session, owner, relationship, false).open();
    }

    private DbRelationshipDialog(ProjectSession session, Window owner, DbRelationship relationship, boolean create) {
        super(session, owner, "Create dbRelationship", ModalityType.APPLICATION_MODAL);

        if (relationship.getSourceEntity() == null) {
            throw new CayenneRuntimeException("Null source entity: %s", relationship);
        }
        if (relationship.getSourceEntity().getDataMap() == null) {
            throw new CayenneRuntimeException("Null DataMap: %s", relationship.getSourceEntity());
        }

        this.relationship = relationship;
        this.create = create;
        this.undo = new DbRelationshipUndoableEdit(session, relationship);
        this.reverseRelationship = relationship.getReverseRelationship();
        this.currentTarget = relationship.getTargetEntity();

        this.name = new JTextField(25);
        this.targetEntities = new JComboBox<>();
        this.toDepPk = new JCheckBox();
        this.toMany = new JCheckBox();
        this.comment = new JTextField(25);
        this.sourceName = new JLabel();
        this.reverseName = new JTextField(25);
        this.addButton = new JButton("Add");
        this.removeButton = new JButton("Remove");
        this.saveButton = new JButton("Done");
        this.cancelButton = new JButton("Cancel");
        this.table = new AttributeTable();
        this.table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        initLayout();
        initBindings();
        initFromModel();
    }

    private void initLayout() {
        getRootPane().setDefaultButton(saveButton);
        getContentPane().setLayout(new BorderLayout());

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(
                new FormLayout(
                        "right:max(50dlu;pref), 3dlu, fill:min(150dlu;pref), 3dlu, fill:min(50dlu;pref)",
                        "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, " +
                                "p, 3dlu, p, 9dlu, p, 3dlu, top:14dlu, 3dlu, top:p:grow"));
        builder.setDefaultDialogBorder();

        builder.addSeparator("Create dbRelationship", cc.xywh(1, 1, 5, 1));
        builder.addLabel("Relationship Name:", cc.xy(1, 3));
        builder.add(name, cc.xywh(3, 3, 1, 1));
        builder.addLabel("Source Entity:", cc.xy(1, 5));
        builder.add(sourceName, cc.xywh(3, 5, 1, 1));
        builder.addLabel("Target Entity:", cc.xy(1, 7));
        builder.add(targetEntities, cc.xywh(3, 7, 1, 1));
        builder.addLabel("To Dep PK:", cc.xy(1, 9));
        builder.add(toDepPk, cc.xywh(3, 9, 1, 1));
        builder.addLabel("To Many:", cc.xy(1, 11));
        builder.add(toMany, cc.xywh(3, 11, 1, 1));
        builder.addLabel("Comment:", cc.xy(1, 13));
        builder.add(comment, cc.xywh(3, 13, 1, 1));
        builder.addSeparator("DbRelationship Information", cc.xywh(1, 15, 5, 1));
        builder.addLabel("Reverse Relationship Name:", cc.xy(1, 17));
        builder.add(reverseName, cc.xywh(3, 17, 1, 1));
        builder.addSeparator("Joins", cc.xywh(1, 19, 5, 1));
        builder.add(new JScrollPane(table), cc.xywh(1, 21, 3, 3, "fill, fill"));

        JPanel joinButtons = new JPanel(new FlowLayout(FlowLayout.LEADING));
        joinButtons.add(addButton);
        joinButtons.add(removeButton);
        builder.add(joinButtons, cc.xywh(5, 21, 1, 3));

        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        getContentPane().add(new CMButtonPanel(cancelButton, saveButton), BorderLayout.SOUTH);
    }

    private void initFromModel() {
        targetEntities.setModel(new TargetComboBoxModel(session.entityResolver().getDbEntities()));
        sourceName.setText(relationship.getSourceEntityName());
        toDepPk.setSelected(relationship.isToDependentPK());
        toMany.setSelected(relationship.isToMany());

        name.setText(relationship.getName());
        if (reverseRelationship != null) {
            reverseName.setText(reverseRelationship.getName());
        }

        if (currentTarget == null) {
            enableOptions(false);
        } else {
            enableInfo();
        }

        comment.setText(ObjectInfo.getFromMetaData(app.getMetaData(), relationship, ObjectInfo.COMMENT));
    }

    private void initBindings() {
        targetEntities.addActionListener(action -> {
            DbEntity selectedItem = ((TargetComboBoxModel) targetEntities.getModel()).selected;
            boolean joinsReset = false;
            if (currentTarget == null) {
                currentTarget = selectedItem;
            } else if (currentTarget != selectedItem) {
                if (showWarningDialog(relationship)) {
                    currentTarget = selectedItem;
                    joinsReset = true;
                } else {
                    targetEntities.setSelectedItem(currentTarget.getName());
                }
            }
            enableInfo();
            if (joinsReset) {
                // joins to the old target make no sense for the new one; reset the uncommitted table rows
                ((DbJoinTableModel) table.getModel()).removeAllRows();
                toDepPk.setSelected(false);
                toDepPk.setEnabled(false);
            }
        });

        addButton.addActionListener(e -> {
            DbJoinTableModel model = (DbJoinTableModel) table.getModel();
            model.addRow(new DbJoin(relationship));
            table.select(model.getRowCount() - 1);
        });

        removeButton.addActionListener(e -> {
            DbJoinTableModel model = (DbJoinTableModel) table.getModel();
            stopEditing();
            int row = table.getSelectedRow();
            model.removeRow(model.getJoin(row));

            if (model.isValidForDepPk()) {
                toDepPk.setEnabled(true);
            } else {
                toDepPk.setEnabled(false);
                toDepPk.setSelected(false);
            }
        });

        saveButton.addActionListener(e -> {
            save();
            saved = true;
            dispose();
        });

        cancelButton.addActionListener(e -> dispose());

        toDepPk.setEnabled(relationship.isValidForDepPk());
        toDepPk.addActionListener(selected -> {
            unsetReverseDepPk = false;
            if (toDepPk.isSelected() && reverseRelationship != null && reverseRelationship.isToDependentPK()) {
                unsetReverseDepPk = JOptionPane.showConfirmDialog(
                        app.getFrame(),
                        "Unset reverse relationship's \"To Dep PK\" setting?",
                        "Warning", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
                if (!unsetReverseDepPk) {
                    // both sides can't be dep PK, and the user chose to keep it on the reverse
                    toDepPk.setSelected(false);
                }
            }
        });
    }

    private void enableInfo() {
        enableOptions(true);

        // carry uncommitted joins over from the previous model, if any; the relationship itself only
        // reflects the table state after "Done"
        List<DbJoin> joins = table.getModel() instanceof DbJoinTableModel previous
                ? previous.getObjectList()
                : relationship.getJoins();
        DbJoinTableModel model = new DbJoinTableModel(relationship, session, this, joins, currentTarget);
        table.setModel(model);
        model.addTableModelListener(change -> {
            if (change.getLastRow() != Integer.MAX_VALUE) {
                toDepPk.setEnabled(model.isValidForDepPk());
            }
        });

        TableColumn sourceColumn = table.getColumnModel().getColumn(DbJoinTableModel.SOURCE);
        JComboBox<String> sourceCombo = new CMComboBox<>(
                dbAttributeNames(relationship.getSourceEntity()).stream().sorted().toArray(String[]::new));
        AutoCompletion.enable(sourceCombo, session::getSelectedDataMap);
        sourceColumn.setCellEditor(new CMAutoCompleteComboBoxCellEditor(sourceCombo));

        TableColumn targetColumn = table.getColumnModel().getColumn(DbJoinTableModel.TARGET);
        JComboBox<String> targetCombo = new CMComboBox<>(
                dbAttributeNames(currentTarget).stream().sorted().toArray(String[]::new));
        AutoCompletion.enable(targetCombo, session::getSelectedDataMap);
        targetColumn.setCellEditor(new CMAutoCompleteComboBoxCellEditor(targetCombo));

        new CMTablePrefs(app.getPrefsManager().uiNode("dbEntity/dbjoinTable"))
                .bind(table, null, DbJoinTableModel.SOURCE);
    }

    private void enableOptions(boolean enable) {
        saveButton.setEnabled(enable);
        reverseName.setEnabled(enable);
        addButton.setEnabled(enable);
        removeButton.setEnabled(enable);
    }

    private void stopEditing() {
        int colIndex = table.getEditingColumn();
        if (colIndex >= 0) {
            TableColumn col = table.getColumnModel().getColumn(colIndex);
            col.getCellEditor().stopCellEditing();
        }
    }

    private void save() {
        stopEditing();

        // "Done" with no edits must behave like "Cancel" - no mutation, no events, no dirty flag
        if (!create && !hasChanges()) {
            return;
        }

        handleNameUpdate(relationship, name.getText().trim());

        // the single place where the dialog editing state is written to the relationship
        relationship.setTargetEntityName(currentTarget != null ? currentTarget.getName() : null);
        relationship.setToMany(toMany.isSelected());
        relationship.setToDependentPK(toDepPk.isSelected());
        if (unsetReverseDepPk && reverseRelationship != null) {
            reverseRelationship.setToDependentPK(false);
        }

        ((DbJoinTableModel) table.getModel()).commit();

        // check after commit, as it filters out empty joins
        boolean updatingReverse = !relationship.getJoins().isEmpty();

        ObjectInfo.putToMetaData(app.getMetaData(), relationship, ObjectInfo.COMMENT, comment.getText());

        // If new reverse DbRelationship was created, add it to the target.
        // Don't create reverse with no joins - makes no sense...
        if (updatingReverse) {

            DbRelationship reverse = reverseRelationship;
            if (reverse == null) {
                reverse = new DbRelationship();
                reverse.setName(NameBuilder
                        .builder(reverse, relationship.getTargetEntity())
                        .baseName(reverseName.getText().trim())
                        .name());

                reverse.setSourceEntity(relationship.getTargetEntity());
                reverse.setTargetEntityName(relationship.getSourceEntity());
                reverse.setToMany(!relationship.isToMany());
                relationship.getTargetEntity().addRelationship(reverse);

                // fire only if the relationship is to the same entity (needed to update entity view)
                if (relationship.getSourceEntity() == relationship.getTargetEntity()) {
                    session.fireDbRelationshipEvent(DbRelationshipEvent.ofAdd(
                            this, reverse, reverse.getSourceEntity()));
                }
            } else {
                handleNameUpdate(reverse, reverseName.getText().trim());
            }

            reverse.setJoins(getReverseJoins(reverse));

            if (!relationship.isToDependentPK() && reverse.isValidForDepPk()) {
                reverse.setToDependentPK(true);
            }
        }

        if (create) {
            DbEntity dbEntity = relationship.getSourceEntity();
            if (dbEntity.getRelationship(relationship.getName()) == null) {
                dbEntity.addRelationship(relationship);
            }

            session.fireDbRelationshipEvent(DbRelationshipEvent.ofAdd(this, relationship, dbEntity));

            DbRelationshipDisplayEvent rde = new DbRelationshipDisplayEvent(
                    this,
                    (DataChannelDescriptor) session.project().getRootNode(),
                    session.getSelectedDataMap(),
                    dbEntity,
                    relationship);

            session.displayDbRelationship(rde);
            app.getUndoManager().addEdit(
                    new CreateDbRelationshipUndoableEdit(session, relationship.getSourceEntity(),
                            new DbRelationship[]{relationship}));

        } else {
            session.fireDbRelationshipEvent(
                    DbRelationshipEvent.ofChange(this, relationship, relationship.getSourceEntity()));
            app.getUndoManager().addEdit(undo);
        }
    }

    /**
     * Compares the dialog widget state against the untouched relationship, covering every input that
     * {@link #save()} would write. Must be called before any mutation.
     */
    private boolean hasChanges() {
        if (!Objects.equals(relationship.getName(), name.getText().trim())) {
            return true;
        }
        if (!Objects.equals(relationship.getTargetEntityName(), currentTarget != null ? currentTarget.getName() : null)) {
            return true;
        }
        if (relationship.isToMany() != toMany.isSelected()) {
            return true;
        }
        if (relationship.isToDependentPK() != toDepPk.isSelected()) {
            return true;
        }
        if (unsetReverseDepPk && reverseRelationship != null && reverseRelationship.isToDependentPK()) {
            return true;
        }
        if (reverseRelationship != null
                && !Objects.equals(reverseRelationship.getName(), reverseName.getText().trim())) {
            return true;
        }
        if (joinsChanged()) {
            return true;
        }

        String oldComment = ObjectInfo.getFromMetaData(app.getMetaData(), relationship, ObjectInfo.COMMENT);
        return !Objects.equals(oldComment != null ? oldComment : "", comment.getText());
    }

    /**
     * Compares the join table rows against the relationship's joins, ignoring blank rows (the ones
     * {@link DbJoinTableModel#commit()} drops) and row order, so that merely sorting the table by a column
     * doesn't count as a change.
     */
    private boolean joinsChanged() {
        List<String> edited = ((DbJoinTableModel) table.getModel()).getObjectList().stream()
                .filter(j -> j.getSourceName() != null || j.getTargetName() != null)
                .map(j -> j.getSourceName() + "→" + j.getTargetName())
                .sorted()
                .toList();
        List<String> existing = relationship.getJoins().stream()
                .map(j -> j.getSourceName() + "→" + j.getTargetName())
                .sorted()
                .toList();
        return !edited.equals(existing);
    }

    private void handleNameUpdate(DbRelationship rel, String userInputName) {
        if (Objects.equals(rel.getName(), userInputName)) {
            return;
        }

        String newName = NameBuilder
                .builder(rel, rel.getSourceEntity())
                .baseName(userInputName)
                .name();

        if (Objects.equals(newName, rel.getName())) {
            return;
        }
        String oldName = rel.getName();

        // the entity maps relationships by name, so an attached relationship must be re-keyed, not just renamed
        DbEntity sourceEntity = rel.getSourceEntity();
        if (sourceEntity.getRelationship(oldName) == rel) {
            sourceEntity.removeRelationship(oldName);
            rel.setName(newName);
            sourceEntity.addRelationship(rel);
        } else {
            rel.setName(newName);
        }

        session.fireDbRelationshipEvent(DbRelationshipEvent.ofChange(this, rel, rel.getSourceEntity(), oldName));
    }

    private Collection<DbJoin> getReverseJoins(DbRelationship reverse) {
        Collection<DbJoin> joins = relationship.getJoins();

        if ((joins == null) || (joins.isEmpty())) {
            return Collections.emptyList();
        }

        List<DbJoin> reverseJoins = new ArrayList<>(joins.size());

        // create reversed pairs
        for (DbJoin pair : joins) {
            DbJoin reverseJoin = pair.createReverseJoin();
            // since reverse relationship is not yet initialized, the reverse join is wired by hand
            reverseJoin.setRelationship(reverse);
            reverseJoins.add(reverseJoin);
        }

        return reverseJoins;
    }

    private boolean showWarningDialog(DbRelationship relationship) {
        DataChannelDescriptor domain = (DataChannelDescriptor) session.project().getRootNode();
        Collection<ObjRelationship> objRelationships = DbRelationshipOps.objRelationshipsUsingDbRelationship(domain, relationship);
        Collection<ObjAttribute> objAttributes = DbRelationshipOps.objAttributesUsingDbRelationship(domain, relationship);

        if (objAttributes.isEmpty() && objRelationships.isEmpty()) {
            int result = JOptionPane.showConfirmDialog(
                    app.getFrame(),
                    "Changing target entity will reset all joins.",
                    "Warning", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            return (result == JOptionPane.OK_OPTION);
        }

        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(new BorderLayout());
        JLabel textLabel = new JLabel(String.format("<html><p>Following ObjAttributes and ObjRelationships "
                + "<br>will be affected by change of DbRelationship <br> '%s'"
                + " target and must be fixed manually. "
                + "<br>Are you sure you want to proceed?</p><br></html>", relationship.getName()));
        dialogPanel.add(textLabel, BorderLayout.NORTH);
        DefaultListModel<String> model = new DefaultListModel<>();
        JList<String> objects = new JList<>(model);
        JScrollPane scrollPane = new JScrollPane(objects);

        if (!objRelationships.isEmpty()) {
            model.addElement("Relationships: ");
            for (ObjRelationship objRelationship : objRelationships) {
                model.addElement(objRelationship.getSourceEntity().getName() + "." + objRelationship.getName());
            }
        }
        if (!objAttributes.isEmpty()) {
            model.addElement("Attributes: ");
            for (ObjAttribute objAttribute : objAttributes) {
                model.addElement(objAttribute.getEntity().getName() + "." + objAttribute.getName());
            }
        }

        dialogPanel.add(scrollPane, BorderLayout.SOUTH);
        int result = JOptionPane.showConfirmDialog(
                app.getFrame(),
                dialogPanel,
                "Warning", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return result == JOptionPane.OK_OPTION;
    }

    private static Collection<String> dbAttributeNames(DbEntity entity) {
        Set<String> keys = entity.getAttributeMap().keySet();
        List<String> names = new ArrayList<>(keys.size() + 1);
        names.add("");
        names.addAll(keys);
        return names;
    }

    private static class AttributeTable extends CMTable {
        private final Dimension preferredSize = new Dimension(203, 100);

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return preferredSize;
        }
    }

    private final class TargetComboBoxModel extends AbstractListModel<String> implements ComboBoxModel<String> {

        private final List<DbEntity> entities;
        private DbEntity selected;

        TargetComboBoxModel(Collection<DbEntity> dbEntities) {
            this.entities = new ArrayList<>(dbEntities);
            this.entities.sort(DB_ENTITY_COMPARATOR);
            selected = relationship.getTargetEntity();
        }

        private String getTitle(DbEntity entity) {
            if (entity == null) {
                return "";
            }
            return relationship.getSourceEntity().getDataMap() == entity.getDataMap()
                    ? entity.getName()
                    : entity.getName() + " (" + entity.getDataMap().getName() + ')';
        }

        @Override
        public int getSize() {
            return entities.size();
        }

        @Override
        public String getElementAt(int index) {
            return getTitle(entities.get(index));
        }

        @Override
        public void setSelectedItem(Object anItem) {
            String title = (String) anItem;
            if (title != null) {
                int spacer = title.indexOf(' ');
                if (spacer != -1) {
                    title = title.substring(0, spacer);
                }
            }
            selected = session.entityResolver().getDbEntity(title);
        }

        @Override
        public Object getSelectedItem() {
            return getTitle(selected);
        }
    }
}

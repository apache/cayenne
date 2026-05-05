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
package org.apache.cayenne.modeler.ui.project.editor.objentity.relinfo;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.modeler.event.display.ObjRelationshipDisplayEvent;
import org.apache.cayenne.modeler.event.model.ObjRelationshipEvent;
import org.apache.cayenne.modeler.toolkit.MultiColumnBrowser;
import org.apache.cayenne.modeler.toolkit.buttons.CMButtonPanel;
import org.apache.cayenne.modeler.toolkit.combobox.CMComboBox;
import org.apache.cayenne.modeler.toolkit.ProjectDialog;
import org.apache.cayenne.modeler.ui.dbrelationship.DbRelationshipDialog;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.undo.CreateRelationshipUndoableEdit;
import org.apache.cayenne.modeler.undo.RelationshipUndoableEdit;
import org.apache.cayenne.modeler.util.Comparators;
import org.apache.cayenne.modeler.util.EntityTreeModel;
import org.apache.cayenne.modeler.util.EntityTreeRelationshipFilter;
import org.apache.cayenne.project.extension.info.ObjectInfo;
import org.apache.cayenne.util.DeleteRuleUpdater;
import org.apache.cayenne.util.Util;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Modal "ObjRelationship inspector" — name, target entity, semantics, collection type,
 * delete rule, and mapping to a path of DbRelationships chosen via a multi-column browser.
 */
public class ObjRelationshipInfoDialog extends ProjectDialog implements TreeSelectionListener {

    private static final Dimension BROWSER_CELL_DIM = new Dimension(130, 200);

    private static final String COLLECTION_TYPE_MAP = "java.util.Map";
    private static final String COLLECTION_TYPE_SET = "java.util.Set";
    private static final String COLLECTION_TYPE_COLLECTION = "java.util.Collection";
    private static final String DEFAULT_MAP_KEY = "ID (default)";

    private static final String[] DELETE_RULES = new String[]{
            DeleteRule.deleteRuleName(DeleteRule.NO_ACTION),
            DeleteRule.deleteRuleName(DeleteRule.NULLIFY),
            DeleteRule.deleteRuleName(DeleteRule.CASCADE),
            DeleteRule.deleteRuleName(DeleteRule.DENY),
    };

    private final JLabel sourceEntityLabel;
    private final JComboBox<String> targetCombo;
    private final JTextField relationshipName;
    private final JLabel semanticsLabel;
    private final JLabel collectionTypeLabel;
    private final JComboBox<String> collectionTypeCombo;
    private final JLabel mapKeysLabel;
    private final JComboBox<String> mapKeysCombo;
    private final JComboBox<String> deleteRule;
    private final JCheckBox usedForLocking;
    private final JTextField comment;
    private final MultiColumnBrowser pathBrowser;
    private final JButton newRelButton;
    private final JButton saveButton;
    private final JButton cancelButton;

    private ObjRelationship relationship;
    private List<DbRelationship> dbRelationships;
    private List<DbRelationship> savedDbRelationships;
    private ObjEntity objectTarget;
    private List<ObjEntity> objectTargets;
    private final List<String> mapKeys = new ArrayList<>();
    private String targetCollection;
    private String mapKey;

    private RelationshipUndoableEdit undo;
    private boolean isCreate;

    public ObjRelationshipInfoDialog(ProjectSession session, Window owner) {
        super(session, owner, "ObjRelationship Inspector", ModalityType.APPLICATION_MODAL);

        this.cancelButton = new JButton("Cancel");
        this.saveButton = new JButton("Done");
        this.newRelButton = new JButton("New DbRelationship");
        this.relationshipName = new JTextField(25);
        this.semanticsLabel = new JLabel();
        this.sourceEntityLabel = new JLabel();
        this.collectionTypeLabel = new JLabel("Collection Type:");
        this.collectionTypeCombo = new CMComboBox<>();
        this.targetCombo = new CMComboBox<>();
        this.mapKeysLabel = new JLabel("Map Key:");
        this.mapKeysCombo = new CMComboBox<>();
        this.deleteRule = new CMComboBox<>(DELETE_RULES);
        this.usedForLocking = new JCheckBox();
        this.comment = new JTextField();

        this.pathBrowser = new ObjRelationshipPathBrowser();
        this.pathBrowser.setPreferredColumnSize(BROWSER_CELL_DIM);
        this.pathBrowser.setDefaultRenderer();

        // collection-type combo entries — wire before any listeners so we don't fire spurious events
        collectionTypeCombo.addItem(COLLECTION_TYPE_COLLECTION);
        collectionTypeCombo.addItem(ObjRelationship.DEFAULT_COLLECTION_TYPE);
        collectionTypeCombo.addItem(COLLECTION_TYPE_MAP);
        collectionTypeCombo.addItem(COLLECTION_TYPE_SET);

        initLayout();

        pathBrowser.addTreeSelectionListener(this);
    }

    public ObjRelationshipInfoDialog createRelationship(ObjEntity objEntity) {
        ObjRelationship rel = new ObjRelationship();
        rel.setName(NameBuilder.builder(rel, objEntity).name());
        rel.setSourceEntity(objEntity);
        DeleteRuleUpdater.updateObjRelationship(rel);
        isCreate = true;
        return modifyRelationship(rel);
    }

    public ObjRelationshipInfoDialog modifyRelationship(ObjRelationship rel) {
        this.relationship = rel;
        this.undo = new RelationshipUndoableEdit(session, rel);

        // current limitation is that an ObjRelationship must have source
        // and target entities present, with DbEntities chosen.
        validateCanMap();

        initFromModel();
        initBindings();
        return this;
    }

    private void initLayout() {
        getRootPane().setDefaultButton(saveButton);
        setLayout(new BorderLayout());

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(
                new FormLayout(
                        "right:max(50dlu;pref), 3dlu, fill:min(150dlu;pref), 3dlu, 300dlu, 3dlu, fill:min(120dlu;pref)",
                        "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, top:14dlu, 3dlu, top:p:grow"));
        builder.setDefaultDialogBorder();

        builder.addSeparator("ObjRelationship Information", cc.xywh(1, 1, 5, 1));
        builder.addLabel("Source Entity:", cc.xy(1, 3));
        builder.add(sourceEntityLabel, cc.xywh(3, 3, 1, 1));
        builder.addLabel("Target Entity:", cc.xy(1, 5));
        builder.add(targetCombo, cc.xywh(3, 5, 1, 1));
        builder.addLabel("Relationship Name:", cc.xy(1, 7));
        builder.add(relationshipName, cc.xywh(3, 7, 1, 1));
        builder.addLabel("Semantics:", cc.xy(1, 9));
        builder.add(semanticsLabel, cc.xywh(3, 9, 5, 1));
        builder.add(collectionTypeLabel, cc.xy(1, 11));
        builder.add(collectionTypeCombo, cc.xywh(3, 11, 1, 1));
        builder.add(mapKeysLabel, cc.xy(1, 13));
        builder.add(mapKeysCombo, cc.xywh(3, 13, 1, 1));
        builder.addLabel("Delete rule:", cc.xy(1, 15));
        builder.add(deleteRule, cc.xywh(3, 15, 1, 1));
        builder.addLabel("Used for locking:", cc.xy(1, 17));
        builder.add(usedForLocking, cc.xywh(3, 17, 1, 1));
        builder.addLabel("Comment:", cc.xy(1, 19));
        builder.add(comment, cc.xywh(3, 19, 1, 1));
        builder.addSeparator("Mapping to DbRelationships", cc.xywh(1, 21, 5, 1));

        JPanel buttonsPane = new JPanel(new FlowLayout(FlowLayout.LEADING));
        buttonsPane.add(newRelButton);
        builder.add(buttonsPane, cc.xywh(1, 23, 5, 1));
        builder.add(new JScrollPane(
                pathBrowser,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), cc.xywh(1, 25, 5, 3));

        add(builder.getPanel(), BorderLayout.CENTER);
        add(new CMButtonPanel(cancelButton, saveButton), BorderLayout.SOUTH);
    }

    private void initBindings() {
        cancelButton.addActionListener(e -> dispose());
        saveButton.addActionListener(e -> saveMapping());
        newRelButton.addActionListener(e -> createRelationship());
        collectionTypeCombo.addActionListener(e -> setCollectionType());
        mapKeysCombo.addActionListener(e -> setMapKey());
        targetCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                Object targetName = e.getItem();
                for (ObjEntity target : objectTargets) {
                    if (Objects.equals(target.getName(), targetName)) {
                        setObjectTarget(target);
                        return;
                    }
                }
            }
        });
        deleteRule.addActionListener(e -> setDeleteRule());
        usedForLocking.addActionListener(e -> setUsedForLocking());
        comment.addActionListener(e -> setComment());
    }

    private void initFromModel() {
        sourceEntityLabel.setText(relationship.getSourceEntity().getName());
        relationshipName.setText(relationship.getName());
        this.mapKey = relationship.getMapKey();
        this.targetCollection = relationship.getCollectionType();
        if (targetCollection == null) {
            targetCollection = ObjRelationship.DEFAULT_COLLECTION_TYPE;
        }
        this.objectTarget = relationship.getTargetEntity();
        if (objectTarget != null) {
            updateTargetCombo(objectTarget.getDbEntity());
            targetCombo.setSelectedItem(objectTarget.getName());
        }
        usedForLocking.setSelected(relationship.isUsedForLocking());
        deleteRule.setSelectedItem(DeleteRule.deleteRuleName(relationship.getDeleteRule()));
        comment.setText(ObjectInfo.getFromMetaData(app.getMetaData(), relationship, ObjectInfo.COMMENT));

        setSemantics();
        // setup path
        dbRelationships = new ArrayList<>(relationship.getDbRelationships());
        this.savedDbRelationships = dbRelationships;
        initMapKeys();
        updateCollectionChoosers();
        // add dummy last relationship if we are not connected
        connectEnds();

        if (pathBrowser.getModel() == null) {
            EntityTreeModel treeModel = new EntityTreeModel(getStartEntity());
            treeModel.setFilter(new EntityTreeRelationshipFilter());

            pathBrowser.setModel(treeModel);
            setSelectionPath(savedDbRelationships);
        }

        saveButton.setEnabled(!dbRelationships.isEmpty());
    }

    /**
     * Selects path in browser
     */
    private void setSelectionPath(List<DbRelationship> rels) {
        Object[] path = new Object[rels.size() + 1];
        path[0] = getStartEntity();
        System.arraycopy(rels.toArray(), 0, path, 1, rels.size());
        pathBrowser.setSelectionPath(new TreePath(path));
    }

    private void setCollectionType() {
        targetCollection = (String) collectionTypeCombo.getSelectedItem();

        if (COLLECTION_TYPE_MAP.equals(targetCollection)) {
            mapKeysLabel.setEnabled(true);
            mapKeysCombo.setEnabled(true);
            setMapKey();
        } else {
            mapKeysLabel.setEnabled(false);
            mapKeysCombo.setEnabled(false);
        }
    }

    private void setMapKey() {
        this.mapKey = (String) mapKeysCombo.getSelectedItem();
    }

    private void updateCollectionChoosers() {
        boolean collectionTypeEnabled = isToMany();
        collectionTypeCombo.setEnabled(collectionTypeEnabled);
        collectionTypeLabel.setEnabled(collectionTypeEnabled);
        if (collectionTypeEnabled) {
            collectionTypeCombo.setSelectedItem(targetCollection);
        }

        boolean mapKeysEnabled = collectionTypeEnabled
                && COLLECTION_TYPE_MAP.equals(collectionTypeCombo.getSelectedItem());
        mapKeysCombo.setEnabled(mapKeysEnabled);
        mapKeysLabel.setEnabled(mapKeysEnabled);
        if (mapKeysEnabled) {
            mapKeysCombo.setSelectedItem(mapKey);
        }
    }

    private void saveMapping() {
        if (!dbRelationships.equals(savedDbRelationships)) {
            if (savedDbRelationships.isEmpty() || JOptionPane.showConfirmDialog(
                    this,
                    "You have changed Db Relationship path. Do you want it to be saved?", "Save ObjRelationship",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                this.savedDbRelationships = new ArrayList<>(dbRelationships);
            }
        }
        configureRelationship();
        if (isCreate) {
            relationship.getSourceEntity().addRelationship(relationship);
            fireObjRelationshipEvent(this);
            app.getUndoManager().addEdit(new CreateRelationshipUndoableEdit(
                    session, relationship.getSourceEntity(), new ObjRelationship[]{relationship}));
        } else {
            session.fireObjRelationshipEvent(ObjRelationshipEvent.ofChange(this, relationship,
                    relationship.getSourceEntity()));
            app.getUndoManager().addEdit(undo);
        }

        sourceEntityLabel.setText(relationship.getSourceEntity().getName());
        dispose();
    }

    private void fireObjRelationshipEvent(Object src) {
        session.fireObjRelationshipEvent(ObjRelationshipEvent.ofAdd(src, relationship, relationship.getSourceEntity()));

        ObjRelationshipDisplayEvent rde = new ObjRelationshipDisplayEvent(
                src,
                (DataChannelDescriptor) session.project().getRootNode(),
                session.getSelectedDataMap(),
                relationship.getSourceEntity(),
                relationship);

        session.displayObjRelationship(rde);
    }

    /**
     * Creates a new relationship connecting currently selected source entity
     * with ObjRelationship target entity.
     */
    private void createRelationship() {
        DbEntity dbEntity = relationship.getSourceEntity().getDbEntity();

        DbRelationshipDialog dialog =
                new DbRelationshipDialog(session, this).createNewRelationship(dbEntity);
        dialog.open();

        Optional<DbRelationship> dbRelationship = dialog.getRelationship();
        if (dbRelationship.isPresent()) {
            Object[] oldPath = new Object[]{getStartEntity()};

            EntityTreeModel treeModel = (EntityTreeModel) pathBrowser.getModel();
            treeModel.invalidate();

            pathBrowser.setSelectionPath(new TreePath(new Object[]{getStartEntity()}));
            pathBrowser.repaint();

            Object[] path = new Object[oldPath.length + 1];
            System.arraycopy(oldPath, 0, path, 0, path.length - 1);
            path[path.length - 1] = dbRelationship.get();
            pathBrowser.setSelectionPath(new TreePath(path));
        }
    }

    private void setDeleteRule() {
        relationship.setDeleteRule(DeleteRule.deleteRuleForName(
                String.valueOf(deleteRule.getSelectedItem())));
    }

    private void setUsedForLocking() {
        relationship.setUsedForLocking(usedForLocking.isSelected());
    }

    private void setComment() {
        ObjectInfo.putToMetaData(app.getMetaData(), relationship, ObjectInfo.COMMENT, comment.getText());
    }

    private void setSemantics() {
        StringBuilder semantics = new StringBuilder(20);
        semantics.append(relationship.isToMany() ? "to many" : "to one");
        if (relationship.isReadOnly()) {
            semantics.append(", read-only");
        }
        semanticsLabel.setText(semantics.toString());
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        TreePath selectedPath = e.getPath();

        // first item in the path is Entity, so we must have at least two elements to
        // constitute a valid ordering path
        if (selectedPath == null || selectedPath.getPathCount() < 2) {
            return;
        }

        List<DbRelationship> relPath = new ArrayList<>(selectedPath.getPathCount() - 1);
        for (int i = 1; i < selectedPath.getPathCount(); i++) {
            relPath.add((DbRelationship) selectedPath.getPathComponent(i));
        }
        setDbRelationships(relPath);
        updateCollectionChoosers();
    }

    private void setObjectTarget(ObjEntity objectTarget) {
        if (this.objectTarget != objectTarget) {
            this.objectTarget = objectTarget;
            initMapKeys();
        }
    }

    private void initMapKeys() {
        mapKeys.clear();
        mapKeys.add(DEFAULT_MAP_KEY);

        // Object target can be null when selected target DbEntity has no ObjEntities
        if (objectTarget == null) {
            return;
        }

        for (ObjAttribute attribute : objectTarget.getAttributes()) {
            mapKeys.add(attribute.getName());
        }
        mapKeysCombo.removeAllItems();
        for (String s : mapKeys) {
            mapKeysCombo.addItem(s);
        }

        if (mapKey != null && !mapKeys.contains(mapKey)) {
            mapKey = DEFAULT_MAP_KEY;
            mapKeysCombo.setSelectedItem(mapKey);
        }
    }

    /**
     * Places in objectTargets list all ObjEntities for specified DbEntity.
     */
    private void updateTargetCombo(DbEntity dbTarget) {
        objectTargets = new ArrayList<>();
        if (dbTarget != null) {
            objectTargets.addAll(dbTarget.getDataMap().getMappedEntities(dbTarget));
            objectTargets.sort(Comparators.forNamedObjects());
        }
        targetCombo.removeAllItems();
        for (ObjEntity s : objectTargets) {
            targetCombo.addItem(s.getName());
        }
    }

    private void setDbRelationships(List<DbRelationship> rels) {
        dbRelationships = rels;
        saveButton.setEnabled(true);
        updateTargetCombo(!rels.isEmpty() ? rels.get(rels.size() - 1).getTargetEntity() : null);
        updateCollectionChoosers();
    }

    private boolean isToMany() {
        // copied algorithm from ObjRelationship.calculateToMany(), only iterating
        // through the unsaved dbrels selection.
        for (DbRelationship rel : dbRelationships) {
            if (rel != null && rel.isToMany()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Stores current state of the model in the internal ObjRelationship.
     */
    private boolean configureRelationship() {
        boolean hasChanges = false;

        boolean oldToMany = relationship.isToMany();
        boolean oldPathNotEmpty = !relationship.getDbRelationships().isEmpty();

        String userInputName = relationshipName.getText();
        String oldName = relationship.getName();
        if (!Util.nullSafeEquals(oldName, userInputName)) {
            ObjEntity source = relationship.getSourceEntity();
            ObjRelationship clash = source.getRelationship(userInputName);
            if (clash != null && clash != relationship) {
                throw new IllegalArgumentException("An attempt to override relationship '" + oldName + "'");
            }
            source.removeRelationship(oldName);
            relationship.setName(userInputName);
            source.addRelationship(relationship);
            hasChanges = true;
        }

        if (!savedDbRelationships.isEmpty()) {
            DbEntity lastEntity = savedDbRelationships.get(savedDbRelationships.size() - 1).getTargetEntity();

            if (objectTarget == null || objectTarget.getDbEntity() != lastEntity) {
                // Entities in combobox and path browser do not match — rely on the
                // browser and auto-select one of lastEntity's ObjEntities.
                Collection<ObjEntity> objEntities = lastEntity.getDataMap().getMappedEntities(lastEntity);
                objectTarget = objEntities.isEmpty() ? null : objEntities.iterator().next();
            }
        }

        if (objectTarget == null || !Util.nullSafeEquals(objectTarget.getName(), relationship.getTargetEntityName())) {
            hasChanges = true;
            // event notification is propagated via the modeler events from the caller —
            // the model itself doesn't know about the mediator.
            relationship.setTargetEntityName(objectTarget);
        }

        // path modifications
        List<DbRelationship> oldPath = relationship.getDbRelationships();
        if (oldPath.size() != savedDbRelationships.size()) {
            hasChanges = true;
            updatePath();
        } else {
            for (int i = 0; i < oldPath.size(); i++) {
                DbRelationship next = savedDbRelationships.get(i);
                if (oldPath.get(i) != next) {
                    hasChanges = true;
                    updatePath();
                    break;
                }
            }
        }

        String collectionType = ObjRelationship.DEFAULT_COLLECTION_TYPE.equals(targetCollection)
                || !relationship.isToMany() ? null : targetCollection;
        if (!Util.nullSafeEquals(collectionType, relationship.getCollectionType())) {
            hasChanges = true;
            relationship.setCollectionType(collectionType);
        }

        // map key only makes sense for Map relationships
        String mapKey = COLLECTION_TYPE_MAP.equals(collectionType) && !DEFAULT_MAP_KEY.equals(this.mapKey)
                ? this.mapKey : null;
        if (!Util.nullSafeEquals(mapKey, relationship.getMapKey())) {
            hasChanges = true;
            relationship.setMapKey(mapKey);
        }

        // CAY-436: if to-many flipped during edit, reset delete rule to default
        if (oldPathNotEmpty && hasChanges && relationship.isToMany() != oldToMany) {
            DeleteRuleUpdater.updateObjRelationship(relationship);
        }

        return hasChanges;
    }

    private void updatePath() {
        relationship.clearDbRelationships();
        for (DbRelationship nextPathComponent : dbRelationships) {
            if (nextPathComponent == null) {
                break;
            }
            relationship.addDbRelationship(nextPathComponent);
        }
    }

    /**
     * Connects last selected DbRelationship in the path to the last DbEntity,
     * creating a dummy relationship if needed.
     */
    private void connectEnds() {
        DbRelationship last = null;

        int size = dbRelationships.size();
        if (size > 0) {
            last = dbRelationships.get(size - 1);
        }

        DbEntity target = getEndEntity();

        if (target != null && (last == null || last.getTargetEntity() != target)) {
            DbEntity source = (last == null) ? getStartEntity() : last.getTargetEntity();
            if (source != null) {
                DbRelationship anyConnector = source.getAnyRelationship(target);
                if (anyConnector != null) {
                    dbRelationships.add(anyConnector);
                }
            }
        }
    }

    /**
     * Checks if the entity can be edited with this inspector.
     * NOTE: As of CAY-1077, the inspector can be opened even with no target entity set.
     */
    private void validateCanMap() {
        if (relationship.getSourceEntity() == null) {
            throw new CayenneRuntimeException("Can't map relationship without source entity.");
        }
        if (getStartEntity() == null) {
            JOptionPane.showMessageDialog(this, "Can't map relationship without source DbEntity. Set source DbEntity.");
            throw new CayenneRuntimeException("Can't map relationship without source DbEntity.");
        }
    }

    private DbEntity getStartEntity() {
        return relationship.getSourceEntity().getDbEntity();
    }

    private DbEntity getEndEntity() {
        // Object target can be null when selected target DbEntity has no ObjEntities
        return objectTarget == null ? null : objectTarget.getDbEntity();
    }
}

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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.modeler.event.model.MapEvent;
import org.apache.cayenne.modeler.event.model.RelationshipEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.ui.dbrelationship.DbRelationshipDialogController;
import org.apache.cayenne.modeler.event.display.RelationshipDisplayEvent;
import org.apache.cayenne.modeler.mvc.ChildController;
import org.apache.cayenne.modeler.undo.CreateRelationshipUndoableEdit;
import org.apache.cayenne.modeler.undo.RelationshipUndoableEdit;
import org.apache.cayenne.modeler.util.Comparators;
import org.apache.cayenne.modeler.util.EntityTreeModel;
import org.apache.cayenne.modeler.util.EntityTreeRelationshipFilter;
import org.apache.cayenne.modeler.swing.MultiColumnBrowser;
import org.apache.cayenne.project.extension.info.ObjectInfo;
import org.apache.cayenne.util.DeleteRuleUpdater;
import org.apache.cayenne.util.Util;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ObjRelationshipInfoController extends ChildController<ProjectController> implements TreeSelectionListener {

    private static final String COLLECTION_TYPE_MAP = "java.util.Map";
    private static final String COLLECTION_TYPE_SET = "java.util.Set";
    private static final String COLLECTION_TYPE_COLLECTION = "java.util.Collection";
    private static final String DEFAULT_MAP_KEY = "ID (default)";

    private ObjRelationship relationship;
    private List<DbRelationship> dbRelationships;

    private List<DbRelationship> savedDbRelationships;
    private ObjEntity objectTarget;
    private List<ObjEntity> objectTargets;
    private final List<String> mapKeys;
    private String targetCollection;
    private String mapKey;
    private final ObjRelationshipInfoView view;

    private RelationshipUndoableEdit undo;
    private boolean isCreate;

    public void startupAction() {
        view.pack();
        view.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        view.setModal(true);
        makeCloseableOnEscape();
        centerView();
        view.setVisible(true);
    }

    public ObjRelationshipInfoController(ProjectController controller) {
        super(controller);
        this.view = new ObjRelationshipInfoView();
        getPathBrowser().addTreeSelectionListener(this);

        view.getCollectionTypeCombo().addItem(COLLECTION_TYPE_COLLECTION);
        view.getCollectionTypeCombo().addItem(ObjRelationship.DEFAULT_COLLECTION_TYPE);
        view.getCollectionTypeCombo().addItem(COLLECTION_TYPE_MAP);
        view.getCollectionTypeCombo().addItem(COLLECTION_TYPE_SET);

        this.mapKeys = new ArrayList<>();
    }

    public ObjRelationshipInfoController createRelationship(ObjEntity objEntity) {
        ObjRelationship rel = new ObjRelationship();
        rel.setName(NameBuilder.builder(rel, objEntity).name());
        rel.setSourceEntity(objEntity);
        DeleteRuleUpdater.updateObjRelationship(rel);
        isCreate = true;
        return modifyRelationship(rel);
    }

    public ObjRelationshipInfoController modifyRelationship(ObjRelationship rel) {
        this.relationship = rel;
        this.undo = new RelationshipUndoableEdit(rel);
        // validate -
        // current limitation is that an ObjRelationship must have source
        // and target entities present, with DbEntities chosen.
        validateCanMap();

        initFromModel();
        initController();
        return this;
    }

    private void initController() {
        view.getCancelButton().addActionListener(e -> view.dispose());
        view.getSaveButton().addActionListener(e -> saveMapping());
        view.getNewRelButton().addActionListener(e -> createRelationship());
        view.getCollectionTypeCombo().addActionListener(e -> setCollectionType());
        view.getMapKeysCombo().addActionListener(e -> setMapKey());
        view.getTargetCombo().addItemListener(e -> {
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
        view.getDeleteRule().addActionListener(e -> setDeleteRule());
        view.getUsedForLocking().addActionListener(e -> setUsedForLocking());
        view.getComment().addActionListener(e -> setComment());
    }

    private void initFromModel() {
        view.getSourceEntityLabel().setText(relationship.getSourceEntity().getName());
        this.view.getRelationshipName().setText(relationship.getName());
        this.mapKey = relationship.getMapKey();
        this.targetCollection = relationship.getCollectionType();
        if (targetCollection == null) {
            targetCollection = ObjRelationship.DEFAULT_COLLECTION_TYPE;
        }
        this.objectTarget = relationship.getTargetEntity();
        if (objectTarget != null) {
            updateTargetCombo(objectTarget.getDbEntity());
            view.getTargetCombo().setSelectedItem(objectTarget.getName());
        }
        view.getUsedForLocking().setSelected(relationship.isUsedForLocking());
        view.getDeleteRule().setSelectedItem(DeleteRule.deleteRuleName(relationship.getDeleteRule()));
        view.getComment().setText(
                ObjectInfo.getFromMetaData(getApplication().getMetaData(),
                        relationship,
                        ObjectInfo.COMMENT));

        setSemantics();
        // setup path
        dbRelationships = new ArrayList<>(relationship.getDbRelationships());
        this.savedDbRelationships = dbRelationships;
        initMapKeys();
        updateCollectionChoosers();
        // add dummy last relationship if we are not connected
        connectEnds();

        if (view.getPathBrowser().getModel() == null) {
            EntityTreeModel treeModel = new EntityTreeModel(getStartEntity());
            treeModel.setFilter(new EntityTreeRelationshipFilter());

            view.getPathBrowser().setModel(treeModel);

            setSelectionPath(getSavedDbRelationships());
        }

        view.getSaveButton().setEnabled(!this.dbRelationships.isEmpty());
    }

    /**
     * Selects path in browser
     */
    private void setSelectionPath(List<DbRelationship> rels) {
        Object[] path = new Object[rels.size() + 1];
        path[0] = getStartEntity();

        System.arraycopy(rels.toArray(), 0, path, 1, rels.size());

        view.getPathBrowser().setSelectionPath(new TreePath(path));
    }

    private void setCollectionType() {
        setTargetCollection((String) view.getCollectionTypeCombo().getSelectedItem());

        if (COLLECTION_TYPE_MAP.equals(targetCollection)) {
            view.getMapKeysLabel().setEnabled(true);
            view.getMapKeysCombo().setEnabled(true);
            setMapKey();
        } else {
            view.getMapKeysLabel().setEnabled(false);
            view.getMapKeysCombo().setEnabled(false);
        }
    }

    public void setMapKey() {
        setMapKey((String) view.getMapKeysCombo().getSelectedItem());
    }

    @Override
    public Component getView() {
        return view;
    }

    /**
     * Updates 'collection type' and 'map keys' comboboxes
     */
    protected void updateCollectionChoosers() {
        boolean collectionTypeEnabled = isToMany();
        view.getCollectionTypeCombo().setEnabled(collectionTypeEnabled);
        view.getCollectionTypeLabel().setEnabled(collectionTypeEnabled);
        if (collectionTypeEnabled) {
            view.getCollectionTypeCombo().setSelectedItem(targetCollection);
        }

        boolean mapKeysEnabled = collectionTypeEnabled
                && ObjRelationshipInfoController.COLLECTION_TYPE_MAP.equals(view.getCollectionTypeCombo().getSelectedItem());
        view.getMapKeysCombo().setEnabled(mapKeysEnabled);
        view.getMapKeysLabel().setEnabled(mapKeysEnabled);
        if (mapKeysEnabled) {
            view.getMapKeysCombo().setSelectedItem(mapKey);
        }
    }

    protected void saveMapping() {
        if (!getDbRelationships().equals(getSavedDbRelationships())) {
            if (getSavedDbRelationships().isEmpty() || JOptionPane.showConfirmDialog(
                    view,
                    "You have changed Db Relationship path. Do you want it to be saved?", "Save ObjRelationship",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                this.savedDbRelationships = new ArrayList<>(dbRelationships);
            }
        }
        configureRelationship();
        if (isCreate) {
            relationship.getSourceEntity().addRelationship(relationship);
            fireObjRelationshipEvent(this);
            Application.getInstance().getUndoManager().addEdit(
                    new CreateRelationshipUndoableEdit(relationship.getSourceEntity(), new ObjRelationship[]{relationship}));
        } else {
            parent.fireObjRelationshipEvent(new RelationshipEvent(this, relationship,
                    relationship.getSourceEntity(), MapEvent.CHANGE));
            Application.getInstance().getUndoManager().addEdit(undo);
        }

        view.getSourceEntityLabel().setText(relationship.getSourceEntity().getName());
        view.dispose();
    }

    private void fireObjRelationshipEvent(Object src) {
        parent.fireObjRelationshipEvent(new RelationshipEvent(src, relationship, relationship.getSourceEntity(), MapEvent.ADD));

        RelationshipDisplayEvent rde = new RelationshipDisplayEvent(
                src,
                relationship,
                relationship.getSourceEntity(),
                parent.getSelectedDataMap(), (DataChannelDescriptor)
                parent.getProject().getRootNode());

        parent.displayObjRelationship(rde);
    }

    public MultiColumnBrowser getPathBrowser() {
        return view.getPathBrowser();
    }

    /**
     * Creates a new relationship connecting currently selected source entity
     * with ObjRelationship target entity. User is allowed to edit the
     * relationship, change its name, and create joins.
     */
    protected void createRelationship() {

        DbEntity dbEntity = relationship.getSourceEntity().getDbEntity();

        DbRelationshipDialogController dbRelationshipDialogController =
                new DbRelationshipDialogController(parent).createNewRelationship(dbEntity);

        dbRelationshipDialogController.startUp();

        Optional<DbRelationship> dbRelationship = dbRelationshipDialogController.getRelationship();
        if (dbRelationship.isPresent()) {
            MultiColumnBrowser pathBrowser = getPathBrowser();
            Object[] oldPath = new Object[]{getStartEntity()};

            // Update the view
            EntityTreeModel treeModel = (EntityTreeModel) pathBrowser.getModel();
            treeModel.invalidate();

            pathBrowser.setSelectionPath(new TreePath(new Object[]{getStartEntity()}));
            pathBrowser.repaint();

            Object[] path = new Object[oldPath.length + 1];
            System.arraycopy(oldPath, 0, path, 0, path.length - 1);

            path[path.length - 1] = dbRelationship;
            pathBrowser.setSelectionPath(new TreePath(path));
        }
    }

    private void setDeleteRule() {
        relationship.setDeleteRule(DeleteRule.deleteRuleForName(
                String.valueOf(view.getDeleteRule().getSelectedItem())));
    }

    private void setUsedForLocking() {
        relationship.setUsedForLocking(view.getUsedForLocking().isSelected());
    }

    private void setComment() {
        ObjectInfo.putToMetaData(getApplication().getMetaData(),
                relationship,
                ObjectInfo.COMMENT,
                view.getComment().getText());
    }

    private void setSemantics() {
        StringBuilder semantics = new StringBuilder(20);
        semantics.append(relationship.isToMany() ? "to many" : "to one");
        if (relationship.isReadOnly()) {
            semantics.append(", read-only");
        }
        view.getSemanticsLabel().setText(semantics.toString());
    }

    /**
     * Sets list of DB Relationships current ObjRelationship is mapped to
     */
    public void valueChanged(TreeSelectionEvent e) {
        TreePath selectedPath = e.getPath();

        // first item in the path is Entity, so we must have
        // at least two elements to constitute a valid ordering path
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

    public void setObjectTarget(ObjEntity objectTarget) {
        if (this.objectTarget != objectTarget) {
            this.objectTarget = objectTarget;

            // init available map keys
            initMapKeys();
        }
    }

    private void initMapKeys() {
        this.mapKeys.clear();

        mapKeys.add(DEFAULT_MAP_KEY);

        // Object target can be null when selected target DbEntity has no ObjEntities
        if (objectTarget == null) {
            return;
        }

        for (ObjAttribute attribute : this.objectTarget.getAttributes()) {
            mapKeys.add(attribute.getName());
        }
        view.getMapKeysCombo().removeAllItems();
        for (String s : mapKeys)
            view.getMapKeysCombo().addItem(s);

        if (mapKey != null && !mapKeys.contains(mapKey)) {
            mapKey = DEFAULT_MAP_KEY;
            view.getMapKeysCombo().setSelectedItem(mapKey);
        }
    }

    /**
     * Places in objectTargets list all ObjEntities for specified DbEntity
     */
    protected void updateTargetCombo(DbEntity dbTarget) {
        // copy those that have DbEntities mapped to dbTarget, and then sort

        this.objectTargets = new ArrayList<>();

        if (dbTarget != null) {
            objectTargets.addAll(dbTarget.getDataMap().getMappedEntities(dbTarget));
            objectTargets.sort(Comparators.forNamedObjects());
        }
        view.getTargetCombo().removeAllItems();
        for (ObjEntity s : objectTargets) {
            view.getTargetCombo().addItem(s.getName());
        }
    }

    /**
     * @return list of DB Relationships current ObjRelationship is mapped to
     */
    public List<DbRelationship> getDbRelationships() {
        return dbRelationships;
    }

    /**
     * @return list of saved DB Relationships
     */
    public List<DbRelationship> getSavedDbRelationships() {
        return savedDbRelationships;
    }

    /**
     * Sets list of DB Relationships current ObjRelationship is mapped to
     */
    public void setDbRelationships(List<DbRelationship> rels) {
        this.dbRelationships = rels;
        view.getSaveButton().setEnabled(true);

        updateTargetCombo(!rels.isEmpty() ? rels.get(rels.size() - 1).getTargetEntity() : null);
        updateCollectionChoosers();
    }

    public String getRelationshipName() {
        return view.getRelationshipName().getText();
    }

    public boolean isToMany() {
        // copied algorithm from ObjRelationship.calculateToMany(), only
        // iterating through
        // the unsaved dbrels selection.

        for (DbRelationship relationship : dbRelationships) {
            if (relationship != null && relationship.isToMany()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Stores current state of the model in the internal ObjRelationship.
     */
    public boolean configureRelationship() {
        boolean hasChanges = false;

        boolean oldToMany = relationship.isToMany();
        boolean oldPathNotEmpty = !relationship.getDbRelationships().isEmpty();

        String relationshipName = getRelationshipName();
        String oldName = relationship.getName();
        if (!Util.nullSafeEquals(oldName, relationshipName)) {
            ObjEntity source = relationship.getSourceEntity();
            ObjRelationship clash = source.getRelationship(relationshipName);
            if (clash != null && clash != relationship) {
                throw new IllegalArgumentException("An attempt to override relationship '" + oldName + "'");
            }
            source.removeRelationship(oldName);
            relationship.setName(relationshipName);
            source.addRelationship(relationship);
            hasChanges = true;
        }

        if (!savedDbRelationships.isEmpty()) {
            DbEntity lastEntity = savedDbRelationships.get(savedDbRelationships.size() - 1).getTargetEntity();

            if (objectTarget == null || objectTarget.getDbEntity() != lastEntity) {
                /*
                 * Entities in combobox and path browser do not match. In this
                 * case, we rely on the browser and automatically select one of
                 * lastEntity's ObjEntities
                 */
                Collection<ObjEntity> objEntities = lastEntity.getDataMap().getMappedEntities(lastEntity);
                objectTarget = objEntities.isEmpty() ? null : objEntities.iterator().next();
            }
        }

        if (objectTarget == null || !Util.nullSafeEquals(objectTarget.getName(), relationship.getTargetEntityName())) {
            hasChanges = true;

            // note on events notification - this needs to be propagated
            // via old modeler events, but we leave this to the controller
            // since model knows nothing about Modeler mediator.
            relationship.setTargetEntityName(objectTarget);
        }

        // check for path modifications
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
        String mapKey = COLLECTION_TYPE_MAP.equals(collectionType) && !DEFAULT_MAP_KEY.equals(this.mapKey) ? this.mapKey
                : null;
        if (!Util.nullSafeEquals(mapKey, relationship.getMapKey())) {
            hasChanges = true;
            relationship.setMapKey(mapKey);
        }

        /*
         * As of CAY-436 here we check if to-many property has changed during
         * the editing, and if so, delete rule must be reset to default value
         */
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

    // Connects last selected DbRelationship in the path to the
    // last DbEntity, creating a dummy relationship if needed.
    private void connectEnds() {
        DbRelationship last = null;

        int size = dbRelationships.size();
        if (size > 0) {
            last = dbRelationships.get(size - 1);
        }

        DbEntity target = getEndEntity();

        if (target != null && (last == null || last.getTargetEntity() != target)) {
            // try to connect automatically, if we can't use dummy connector
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
     * Checks if the entity can be edited with this inspector. NOTE: As of
     * CAY-1077, relationship inspector can be opened even if no target entity
     * was set.
     */
    private void validateCanMap() {
        if (relationship.getSourceEntity() == null) {
            throw new CayenneRuntimeException("Can't map relationship without source entity.");
        }

        if (getStartEntity() == null) {
            JOptionPane.showMessageDialog(view, "Can't map relationship without source DbEntity. Set source DbEntity.");
            throw new CayenneRuntimeException("Can't map relationship without source DbEntity.");
        }
    }

    public DbEntity getStartEntity() {
        return relationship.getSourceEntity().getDbEntity();
    }

    public DbEntity getEndEntity() {
        // Object target can be null when selected target DbEntity has no ObjEntities
        if (objectTarget == null) {
            return null;
        }

        return objectTarget.getDbEntity();
    }

    public void setMapKey(String mapKey) {
        this.mapKey = mapKey;
    }

    public void setTargetCollection(String targetCollection) {
        this.targetCollection = targetCollection;
    }
}

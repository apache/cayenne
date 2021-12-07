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
package org.apache.cayenne.modeler.dialog.objentity;

import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.dbsync.naming.ObjectNameGenerator;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ClassLoadingService;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.DbRelationshipDialog;
import org.apache.cayenne.modeler.event.RelationshipDisplayEvent;
import org.apache.cayenne.modeler.undo.CreateRelationshipUndoableEdit;
import org.apache.cayenne.modeler.undo.RelationshipUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.modeler.util.Comparators;
import org.apache.cayenne.modeler.util.EntityTreeModel;
import org.apache.cayenne.modeler.util.EntityTreeRelationshipFilter;
import org.apache.cayenne.modeler.util.MultiColumnBrowser;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.project.extension.info.ObjectInfo;
import org.apache.cayenne.util.DeleteRuleUpdater;
import org.apache.cayenne.util.Util;

public class ObjRelationshipInfo extends CayenneController implements TreeSelectionListener {

    private static final String COLLECTION_TYPE_MAP = "java.util.Map";
    private static final String COLLECTION_TYPE_SET = "java.util.Set";
    private static final String COLLECTION_TYPE_COLLECTION = "java.util.Collection";
    private static final String DEFAULT_MAP_KEY = "ID (default)";

    protected ObjRelationship relationship;

    private List<DbRelationship> dbRelationships;

    private List<DbRelationship> savedDbRelationships;
    private ObjEntity objectTarget;
    private List<ObjEntity> objectTargets;
    private List<String> targetCollections;
    private List<String> mapKeys;
    private String targetCollection;
    private String mapKey;
    private ObjRelationshipInfoView view;
    private String currentPath;
    private ProjectController mediator;

    private RelationshipUndoableEdit undo;
    private boolean isCreate = false;

    /**
     * Starts options dialog.
     */
    public void startupAction() {
        view.pack();
        view.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        view.setModal(true);
        makeCloseableOnEscape();
        centerView();
        view.setVisible(true);
    }

    public ObjRelationshipInfo(ProjectController mediator) {
        super(mediator);
        this.view = new ObjRelationshipInfoView();
        this.mediator = mediator;
        getPathBrowser().addTreeSelectionListener(this);

        this.targetCollections = new ArrayList<>(4);
        targetCollections.add(COLLECTION_TYPE_COLLECTION);
        targetCollections.add(ObjRelationship.DEFAULT_COLLECTION_TYPE);
        targetCollections.add(COLLECTION_TYPE_MAP);
        targetCollections.add(COLLECTION_TYPE_SET);

        for (String s : targetCollections) {
            view.getCollectionTypeCombo().addItem(s);
        }

        this.mapKeys = new ArrayList<>();
    }

    public ObjRelationshipInfo createRelationship(ObjEntity objEntity) {
        ObjRelationship rel = new ObjRelationship();
        rel.setName(NameBuilder.builder(rel, objEntity).name());
        rel.setSourceEntity(objEntity);
        DeleteRuleUpdater.updateObjRelationship(rel);
        isCreate = true;
        return modifyRelationship(rel);
    }

    public ObjRelationshipInfo modifyRelationship(ObjRelationship rel) {
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
                ObjectInfo.getFromMetaData(mediator.getApplication().getMetaData(),
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
     * Reverts current path to saved path
     */
    protected void revertPath() {
        setSelectionPath(getSavedDbRelationships());
        setDbRelationships(getSavedDbRelationships());
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
                && ObjRelationshipInfo.COLLECTION_TYPE_MAP.equals(view.getCollectionTypeCombo().getSelectedItem());
        view.getMapKeysCombo().setEnabled(mapKeysEnabled);
        view.getMapKeysLabel().setEnabled(mapKeysEnabled);
        if (mapKeysEnabled) {
            view.getMapKeysCombo().setSelectedItem(mapKey);
        }
    }

    /**
     * Clears paths and selections in browser
     */
    protected void clearPath() {
        getPathBrowser().clearSelection();
        setDbRelationships(new ArrayList<>());
    }

    protected void saveMapping() {
        if (!getDbRelationships().equals(getSavedDbRelationships())) {
            if (getSavedDbRelationships().isEmpty() || JOptionPane.showConfirmDialog(getView(),
                    "You have changed Db Relationship path. Do you want it to be saved?", "Save ObjRelationship",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                this.savedDbRelationships = new ArrayList<>(dbRelationships);
            }
        }
        configureRelationship();
        if(isCreate) {
            relationship.getSourceEntity().addRelationship(relationship);
            fireObjRelationshipEvent(this);
            Application.getInstance().getUndoManager().addEdit(
                    new CreateRelationshipUndoableEdit(relationship.getSourceEntity(), new ObjRelationship[]{relationship}));
        } else {
            mediator.fireObjRelationshipEvent(new RelationshipEvent(this, relationship,
                    relationship.getSourceEntity(), MapEvent.CHANGE));
            Application.getInstance().getUndoManager().addEdit(undo);
        }

        view.getSourceEntityLabel().setText(relationship.getSourceEntity().getName());
        view.dispose();
    }

    private void fireObjRelationshipEvent(Object src) {
        mediator.fireObjRelationshipEvent(new RelationshipEvent(src, relationship, relationship.getSourceEntity(), MapEvent.ADD));

        RelationshipDisplayEvent rde = new RelationshipDisplayEvent(src, relationship, relationship.getSourceEntity(), mediator.getCurrentDataMap(),
                (DataChannelDescriptor) mediator.getProject().getRootNode());

        mediator.fireObjRelationshipDisplayEvent(rde);
    }

    /**
     * @return relationship path browser
     */
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

        DbRelationshipDialog dbRelationshipDialog = new DbRelationshipDialog(mediator)
                .createNewRelationship(dbEntity);

        dbRelationshipDialog.startUp();

        Optional<DbRelationship> dbRelationship = dbRelationshipDialog.getRelationship();
        if(dbRelationship.isPresent()) {
            MultiColumnBrowser pathBrowser = getPathBrowser();
            Object[] oldPath = new Object[] { getStartEntity() };

            // Update the view
            EntityTreeModel treeModel = (EntityTreeModel) pathBrowser.getModel();
            treeModel.invalidate();

            pathBrowser.setSelectionPath(new TreePath(new Object[] { getStartEntity() }));
            pathBrowser.repaint();

            Object[] path = new Object[oldPath.length + 1];
            System.arraycopy(oldPath, 0, path, 0, path.length - 1);

            path[path.length - 1] = dbRelationship;
            pathBrowser.setSelectionPath(new TreePath(path));
        }
    }

    public ObjectNameGenerator createNamingStrategy(String strategyClass) {
        try {
            ClassLoadingService classLoader = application.getClassLoadingService();
            return classLoader.loadClass(ObjectNameGenerator.class, strategyClass).newInstance();
        } catch (Throwable th) {
            JOptionPane.showMessageDialog(
                    view,
                    "Naming Strategy Initialization Error: " + th.getMessage(),
                    "Naming Strategy Initialization Error",
                    JOptionPane.ERROR_MESSAGE);
            return null;
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
        ObjectInfo.putToMetaData(mediator.getApplication().getMetaData(),
                relationship,
                ObjectInfo.COMMENT,
                view.getComment().getText());
    }

    private void setSemantics() {
        StringBuilder semantics =  new StringBuilder(20);
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
    @SuppressWarnings("unchecked")
    protected void updateTargetCombo(DbEntity dbTarget) {
        // copy those that have DbEntities mapped to dbTarget, and then sort

        this.objectTargets = new ArrayList<>();

        if (dbTarget != null) {
            objectTargets.addAll(dbTarget.getDataMap().getMappedEntities(dbTarget));
            objectTargets.sort(Comparators.getNamedObjectComparator());
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
     * @return last relationship in the path, or <code>null</code> if path is
     *         empty
     */
    public DbRelationship getLastRelationship() {
        return dbRelationships.size() == 0 ? null : dbRelationships.get(dbRelationships.size() - 1);
    }

    /**
     * Sets list of DB Relationships current ObjRelationship is mapped to
     */
    public void setDbRelationships(List<DbRelationship> rels) {
        this.dbRelationships = rels;
        view.getSaveButton().setEnabled(true);

        updateTargetCombo(rels.size() > 0 ? rels.get(rels.size() - 1).getTargetEntity() : null);
        updateCollectionChoosers();
    }

    /**
     * Returns currently selected target of the ObjRelationship.
     */
    public ObjEntity getObjectTarget() {
        return objectTarget;
    }

    /**
     * Returns a list of ObjEntities available for target mapping.
     */
    public List<ObjEntity> getObjectTargets() {
        return objectTargets;
    }

    public String getRelationshipName() {
        return view.getRelationshipName().getText();
    }

    public void setRelationshipName(String relationshipName) {
        view.getRelationshipName().setText(relationshipName);
    }

    /**
     * Processes relationship path when path component at index was changed.
     */
    public void relationshipChanged(int index) {
        // strip everything starting from the index
        breakChain(index);

        // connect the ends
        connectEnds();
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
        if (!Util.nullSafeEquals(relationship.getName(), relationshipName)) {
            ProjectUtil.setRelationshipName(relationship.getSourceEntity(), relationship, relationshipName);
            hasChanges = true;
        }

        if (savedDbRelationships.size() > 0) {
            DbEntity lastEntity = savedDbRelationships.get(savedDbRelationships.size() - 1).getTargetEntity();

            if (objectTarget == null || objectTarget.getDbEntity() != lastEntity) {
                /*
                 * Entities in combobox and path browser do not match. In this
                 * case, we rely on the browser and automatically select one of
                 * lastEntity's ObjEntities
                 */
                Collection<ObjEntity> objEntities = lastEntity.getDataMap().getMappedEntities(lastEntity);
                objectTarget = objEntities.size() == 0 ? null : objEntities.iterator().next();
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

    private void breakChain(int index) {
        // strip everything starting from the index
        while (dbRelationships.size() > (index + 1)) {
            // remove last
            dbRelationships.remove(dbRelationships.size() - 1);
        }
    }

    // Connects last selected DbRelationship in the path to the
    // last DbEntity, creating a dummy relationship if needed.
    private void connectEnds() {
        Relationship last = null;

        int size = dbRelationships.size();
        if (size > 0) {
            last = dbRelationships.get(size - 1);
        }

        Entity target = getEndEntity();

        if (target != null && (last == null || last.getTargetEntity() != target)) {
            // try to connect automatically, if we can't use dummy connector
            Entity source = (last == null) ? getStartEntity() : last.getTargetEntity();
            if (source != null) {
                Relationship anyConnector = source.getAnyRelationship(target);
                if (anyConnector != null) {
                    dbRelationships.add((DbRelationship) anyConnector);
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
            JOptionPane.showMessageDialog(getView(), "Can't map relationship without source DbEntity. Set source DbEntity.");
            throw new CayenneRuntimeException("Can't map relationship without source DbEntity.");
        }
    }

    public DbEntity getStartEntity() {
        return relationship.getSourceEntity().getDbEntity();
    }

    public DbEntity getEndEntity() {
        /*
         * Object target can be null when selected target DbEntity has no
         * ObjEntities
         */
        if (objectTarget == null) {
            return null;
        }

        return objectTarget.getDbEntity();
    }

    public String getMapKey() {
        return mapKey;
    }

    public void setMapKey(String mapKey) {
        this.mapKey = mapKey;
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public String getTargetCollection() {
        return targetCollection;
    }

    public void setTargetCollection(String targetCollection) {
        this.targetCollection = targetCollection;
    }

    public List getMapKeys() {
        return mapKeys;
    }

    public List<String> getTargetCollections() {
        return targetCollections;
    }
}

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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.map.naming.ExportedKey;
import org.apache.cayenne.map.naming.ObjectNameGenerator;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ClassLoadingService;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.ResolveDbRelationshipDialog;
import org.apache.cayenne.modeler.util.*;
import org.apache.cayenne.util.DeleteRuleUpdater;
import org.apache.cayenne.util.Util;

public class ObjRelationshipInfo extends CayenneController implements TreeSelectionListener {

    static final String COLLECTION_TYPE_MAP = "java.util.Map";
    static final String COLLECTION_TYPE_SET = "java.util.Set";
    static final String COLLECTION_TYPE_COLLECTION = "java.util.Collection";
    static final String DEFAULT_MAP_KEY = "ID (default)";

    protected ObjRelationship relationship;

    protected List<DbRelationship> dbRelationships;

    protected List<DbRelationship> savedDbRelationships;
    protected ObjEntity objectTarget;
    protected List<ObjEntity> objectTargets;
    protected List<String> targetCollections;
    protected List<String> mapKeys;
    protected String relationshipName;
    protected String targetCollection;
    protected String mapKey;
    protected ObjRelationshipInfoView view;
    protected String currentPath;
    protected ProjectController mediator;

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

    public ObjRelationshipInfo(ProjectController mediator, ObjRelationship relationship) {
        super(mediator);
        this.view = new ObjRelationshipInfoView(mediator);
        this.mediator = mediator;
        ObjEntity target = getObjectTarget();
        getPathBrowser().addTreeSelectionListener(this);
        setObjectTarget(target);
        view.sourceEntityLabel.setText(relationship.getSourceEntity().getName());
        this.relationship = relationship;
        this.relationshipName = relationship.getName();
        view.relationshipName.setText(relationshipName);
        this.mapKey = relationship.getMapKey();
        this.targetCollection = relationship.getCollectionType();
        if (targetCollection == null) {
            targetCollection = ObjRelationship.DEFAULT_COLLECTION_TYPE;
        }

        this.objectTarget = (ObjEntity) relationship.getTargetEntity();
        if (objectTarget != null) {
            updateTargetCombo(objectTarget.getDbEntity());
        }

        // validate -
        // current limitation is that an ObjRelationship must have source
        // and target entities present, with DbEntities chosen.
        validateCanMap();

        this.targetCollections = new ArrayList<String>(4);
        targetCollections.add(COLLECTION_TYPE_COLLECTION);
        targetCollections.add(ObjRelationship.DEFAULT_COLLECTION_TYPE);
        targetCollections.add(COLLECTION_TYPE_MAP);
        targetCollections.add(COLLECTION_TYPE_SET);

        for (String s : targetCollections) {
            view.collectionTypeCombo.addItem(s);
        }

        this.mapKeys = new ArrayList<String>();
        initMapKeys();

        // setup path
        dbRelationships = new ArrayList<DbRelationship>(relationship.getDbRelationships());
        selectPath();
        updateCollectionChoosers();

        // add dummy last relationship if we are not connected
        connectEnds();
        initFromModel();
        initController();
    }

    private void initController() {
        view.getCancelButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                view.dispose();
            }
        });
        view.getSaveButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveMapping();
            }
        });
        view.getNewRelButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                createRelationship();
            }
        });
        view.getSelectPathButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                selectPath();
            }
        });
        view.getCollectionTypeCombo().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setCollectionType();
            }
        });
        view.getMapKeysCombo().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setMapKey();
            }
        });
    }

    void initFromModel() {

        if (view.pathBrowser.getModel() == null) {
            EntityTreeModel treeModel = new EntityTreeModel(getStartEntity());
            treeModel.setFilter(new EntityTreeFilter() {

                public boolean attributeMatch(Object node, Attribute attr) {
                    // attrs not allowed here
                    return false;
                }

                public boolean relationshipMatch(Object node, Relationship rel) {
                    if (!(node instanceof Relationship)) {
                        return true;
                    }

                    /**
                     * We do not allow A->B->A chains, where relationships are
                     * to-one
                     */
                    DbRelationship prev = (DbRelationship) node;
                    return !(!rel.isToMany() && prev.getReverseRelationship() == rel);
                }

            });

            view.pathBrowser.setModel(treeModel);

            setSelectionPath(getSavedDbRelationships());
        }
    }

    /**
     * Selects path in browser
     */
    void setSelectionPath(List<DbRelationship> rels) {
        Object[] path = new Object[rels.size() + 1];
        path[0] = getStartEntity();

        System.arraycopy(rels.toArray(), 0, path, 1, rels.size());

        view.pathBrowser.setSelectionPath(new TreePath(path));
    }

    public void setCollectionType() {
        setTargetCollection((String) view.collectionTypeCombo.getSelectedItem());

        if (COLLECTION_TYPE_MAP.equals(targetCollection)) {
            view.mapKeysLabel.setEnabled(true);
            view.mapKeysCombo.setEnabled(true);
            setMapKey();
        } else {
            view.mapKeysLabel.setEnabled(false);
            view.mapKeysCombo.setEnabled(false);
        }
    }

    public void setMapKey() {
        setMapKey((String) view.mapKeysCombo.getSelectedItem());
    }

    @Override
    public Component getView() {
        return view;
    }

    public void setSavedDbRelationships(List<DbRelationship> rels) {
        this.savedDbRelationships = rels;

        String currPath = "";
        for (DbRelationship rel : rels) {
            currPath += "->" + rel.getName();
        }

        if (rels.size() > 0) {
            currPath = currPath.substring(2);
        }

        currentPath = currPath;
        view.currentPathLabel.setText(currPath);
    }

    public void selectPath() {
        setSavedDbRelationships(new ArrayList<DbRelationship>(dbRelationships));
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
        view.collectionTypeCombo.setEnabled(collectionTypeEnabled);
        view.collectionTypeLabel.setEnabled(collectionTypeEnabled);
        if (collectionTypeEnabled) {
            view.collectionTypeCombo.setSelectedItem(targetCollection);
        }

        boolean mapKeysEnabled = collectionTypeEnabled
                && ObjRelationshipInfo.COLLECTION_TYPE_MAP.equals(view.collectionTypeCombo.getSelectedItem());
        view.mapKeysCombo.setEnabled(mapKeysEnabled);
        view.mapKeysLabel.setEnabled(mapKeysEnabled);
        if (mapKeysEnabled) {
            view.mapKeysCombo.setSelectedItem(mapKey);
        }
    }

    /**
     * Clears paths and selections in browser
     */
    protected void clearPath() {
        getPathBrowser().clearSelection();
        setDbRelationships(new ArrayList<DbRelationship>());
    }

    protected void saveMapping() {
        if (!getDbRelationships().equals(getSavedDbRelationships())) {
            if (JOptionPane.showConfirmDialog((Component) getView(),
                    "You have changed Db Relationship path. Do you want it to be saved?", "Save ObjRelationship",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                selectPath();
            }
        }

        if (savePath()) {
            mediator.fireObjRelationshipEvent(new RelationshipEvent(Application.getFrame(), getRelationship(),
                    getRelationship().getSourceEntity()));
        }
        view.sourceEntityLabel.setText(relationship.getSourceEntity().getName());
        view.dispose();
    }

    /**
     * @return relationship path browser
     */
    public MultiColumnBrowser getPathBrowser() {
        return view.pathBrowser;
    }

    /**
     * Creates a new relationship connecting currently selected source entity
     * with ObjRelationship target entity. User is allowed to edit the
     * relationship, change its name, and create joins.
     */
    protected void createRelationship() {

        DbRelationship dbRel = getLastRelationship();
        DbEntity source = dbRel != null ? dbRel.getTargetEntity() : null;

        DbRelationshipTarget targetModel = new DbRelationshipTarget(mediator, getStartEntity(), source);
        targetModel.startupAction();

        if (!targetModel.isSavePressed()) {
            return;
        }

        DbRelationship dbRelationship = new DbRelationship();
        dbRelationship.setName(createNamingStrategy(NameGeneratorPreferences
                .getInstance()
                .getLastUsedStrategies()
                .get(0)).createDbRelationshipName(
                new ExportedKey(targetModel.getSource().getName(), null, null,
                                targetModel.getTarget().getName(), null, null, (short) 1),
                targetModel.isToMany()));

        // note: NamedObjectFactory doesn't set source or target, just the name
        dbRelationship.setSourceEntity(targetModel.getSource());
        dbRelationship.setTargetEntity(targetModel.getTarget());
        dbRelationship.setToMany(targetModel.isToMany());
        targetModel.getSource().addRelationship(dbRelationship);

        // TODO: creating relationship outside of ResolveDbRelationshipDialog
        // confuses it to send incorrect event - CHANGE instead of ADD
        ResolveDbRelationshipDialog dialog = new ResolveDbRelationshipDialog(dbRelationship);

        dialog.setVisible(true);
        if (dialog.isCancelPressed()) {
            targetModel.getSource().removeRelationship(dbRelationship.getName());
        } else {
            MultiColumnBrowser pathBrowser = getPathBrowser();
            Object[] oldPath = targetModel.isSource1Selected() ? new Object[] { getStartEntity() } : pathBrowser
                    .getSelectionPath().getPath();

            /**
             * Update the view
             */
            EntityTreeModel treeModel = (EntityTreeModel) pathBrowser.getModel();
            treeModel.invalidate();

            pathBrowser.setSelectionPath(new TreePath(new Object[] { getStartEntity() }));
            pathBrowser.repaint();

            Object[] path = new Object[oldPath.length + 1];
            System.arraycopy(oldPath, 0, path, 0, path.length - 1);

            path[path.length - 1] = dbRelationship;
            pathBrowser.setSelectionPath(new TreePath(path));
        }

        dialog.dispose();
    }

    public ObjectNameGenerator createNamingStrategy(String strategyClass) {
        try {
            ClassLoadingService classLoader = application.getClassLoadingService();

            return classLoader.loadClass(ObjectNameGenerator.class, strategyClass).newInstance();
        }
        catch (Throwable th) {
            JOptionPane.showMessageDialog(
                    view,
                    "Naming Strategy Initialization Error: " + th.getMessage(),
                    "Naming Strategy Initialization Error",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
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

        Relationship rel = (Relationship) selectedPath.getLastPathComponent();
        DbEntity target = (DbEntity) rel.getTargetEntity();
        /**
         * Initialize root with one of mapped ObjEntities.
         */
        Collection<ObjEntity> objEntities = target.getDataMap().getMappedEntities(target);

        List<DbRelationship> relPath = new Vector<DbRelationship>(selectedPath.getPathCount() - 1);
        for (int i = 1; i < selectedPath.getPathCount(); i++) {
            relPath.add((DbRelationship) selectedPath.getPathComponent(i));
        }
        setDbRelationships(relPath);
        setObjectTarget(objEntities.size() == 0 ? null : objEntities.iterator().next());

        updateCollectionChoosers();
    }

    public void setObjectTarget(ObjEntity objectTarget) {
        if (this.objectTarget != objectTarget) {
            this.objectTarget = objectTarget;
            view.targetCombo.setSelectedItem(objectTarget);

            // init available map keys
            initMapKeys();
        }
    }

    private void initMapKeys() {
        this.mapKeys.clear();

        mapKeys.add(DEFAULT_MAP_KEY);

        /**
         * Object target can be null when selected target DbEntity has no
         * ObjEntities
         */
        if (objectTarget == null) {
            return;
        }

        for (ObjAttribute attribute : this.objectTarget.getAttributes()) {
            mapKeys.add(attribute.getName());
        }
        view.mapKeysCombo.removeAllItems();
        for (String s : mapKeys)
            view.mapKeysCombo.addItem(s);

        if (mapKey != null && !mapKeys.contains(mapKey)) {
            mapKey = DEFAULT_MAP_KEY;
            view.mapKeysCombo.setSelectedItem(mapKey);
        }
    }

    /**
     * Places in objectTargets list all ObjEntities for specified DbEntity
     */
    @SuppressWarnings("unchecked")
    protected void updateTargetCombo(DbEntity dbTarget) {
        // copy those that have DbEntities mapped to dbTarget, and then sort

        this.objectTargets = new ArrayList<ObjEntity>();

        if (dbTarget != null) {
            objectTargets.addAll(dbTarget.getDataMap().getMappedEntities(dbTarget));
            Collections.sort(objectTargets, Comparators.getNamedObjectComparator());
        }
        view.targetCombo.removeAllItems();
        for (ObjEntity s : objectTargets) {
            view.targetCombo.addItem(s.getName());
        }
    }

    public ObjRelationship getRelationship() {
        return relationship;
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

        updateTargetCombo(rels.size() > 0 ? (DbEntity) rels.get(rels.size() - 1).getTargetEntity() : null);

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
        return relationshipName;
    }

    public void setRelationshipName(String relationshipName) {
        view.relationshipName.setText(relationshipName);
        this.relationshipName = relationshipName;
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
    public boolean savePath() {
        boolean hasChanges = false;

        boolean oldToMany = relationship.isToMany();

        if (!Util.nullSafeEquals(relationship.getName(), relationshipName)) {
            hasChanges = true;
            relationship.setName(relationshipName);
        }

        if (savedDbRelationships.size() > 0) {
            DbEntity lastEntity = (DbEntity) savedDbRelationships.get(savedDbRelationships.size() - 1)
                    .getTargetEntity();

            if (objectTarget == null || objectTarget.getDbEntity() != lastEntity) {
                /**
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
            relationship.setTargetEntity(objectTarget);
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

        /**
         * As of CAY-436 here we check if to-many property has changed during
         * the editing, and if so, delete rule must be reset to default value
         */
        if (hasChanges && relationship.isToMany() != oldToMany) {
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

                Relationship anyConnector = source != null ? source.getAnyRelationship(target) : null;

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
            throw new CayenneRuntimeException("Can't map relationship without source DbEntity.");
        }
    }

    public DbEntity getStartEntity() {
        return ((ObjEntity) relationship.getSourceEntity()).getDbEntity();
    }

    public DbEntity getEndEntity() {
        /**
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

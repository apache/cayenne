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
package org.apache.cayenne.modeler.dialog.autorelationship;

import org.apache.cayenne.dbsync.naming.ObjectNameGenerator;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ClassLoadingService;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.mvc.ChildController;
import org.apache.cayenne.modeler.mvc.RootController;
import org.apache.cayenne.modeler.undo.CreateRelationshipUndoableEdit;
import org.apache.cayenne.modeler.undo.InferRelationshipsUndoableEdit;
import org.apache.cayenne.modeler.util.NameGeneratorPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class InferRelationshipsController extends ChildController<RootController> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InferRelationshipsController.class);
    public static final String SELECTED_PROPERTY = "selected";

    protected DataMap dataMap;
    protected java.util.List<InferredRelationship> inferredRelationships;
    protected java.util.List<DbEntity> entities;
    protected Set<InferredRelationship> selectedEntities;
    protected int index;
    protected ObjectNameGenerator strategy;
    private final InferRelationshipsTabController entitySelector;
    private InferRelationshipsDialog view;

    public InferRelationshipsController(RootController parent, DataMap dataMap) {
        super(parent);
        this.dataMap = dataMap;
        this.entities = new ArrayList<>(dataMap.getDbEntities());
        this.selectedEntities = new HashSet<>();
        this.strategy = createNamingStrategy(NameGeneratorPreferences
                .getInstance()
                .getLastUsedStrategies()
                .get(0));
        setNamingStrategy(strategy);
        setRelationships();
        this.entitySelector = new InferRelationshipsTabController(this);
    }


    public void setRelationships() {
        inferredRelationships = new ArrayList<>();

        for (DbEntity entity : entities) {
            createRelationships(entity);
        }

        createJoins();
        createNames();
    }

    protected void createRelationships(DbEntity entity) {

        for (DbAttribute attribute : entity.getAttributes()) {

            String name = attribute.getName();
            if (name.length() < 4) {
                continue;
            }

            if (!name.substring(name.length() - 3).equalsIgnoreCase("_ID")) {
                continue;
            }

            String baseName = name.substring(0, name.length() - 3);
            for (DbEntity targetEntity : entities) {
                // TODO: should we handle relationships to self??
                if (targetEntity == entity) {
                    continue;
                }

                if (baseName.equalsIgnoreCase(targetEntity.getName())
                        && !attribute.isPrimaryKey()
                        && !targetEntity.getAttributes().isEmpty()) {

                    if (!attribute.isForeignKey()) {
                        InferredRelationship myir = new InferredRelationship();
                        myir.setSource(entity);
                        myir.setTarget(targetEntity);
                        inferredRelationships.add(myir);
                    }
                    createReversRelationship(targetEntity, entity);
                }
            }
        }
    }

    public void createReversRelationship(DbEntity eSourse, DbEntity eTarget) {
        InferredRelationship myir = new InferredRelationship();
        for (DbRelationship relationship : eSourse.getRelationships()) {
            for (DbJoin join : relationship.getJoins()) {
                if (join.getSource().getEntity().equals(eSourse) && join.getTarget().getEntity().equals(eTarget)) {
                    return;
                }
            }
        }
        myir.setSource(eSourse);
        myir.setTarget(eTarget);
        inferredRelationships.add(myir);
    }

    protected DbAttribute getJoinAttribute(DbEntity sEntity, DbEntity tEntity) {
        if (sEntity.getAttributes().size() == 1) {
            return sEntity.getAttributes().iterator().next();
        } else {
            for (DbAttribute attr : sEntity.getAttributes()) {
                if (attr.getName().equalsIgnoreCase(tEntity.getName() + "_ID")) {
                    return attr;
                }
            }

            for (DbAttribute attr : sEntity.getAttributes()) {
                if ((attr.getName().equalsIgnoreCase(sEntity.getName() + "_ID"))
                        && (!attr.isPrimaryKey())) {
                    return attr;
                }
            }

            for (DbAttribute attr : sEntity.getAttributes()) {
                if (attr.isPrimaryKey()) {
                    return attr;
                }
            }
        }
        return null;
    }

    protected void createJoins() {
        Iterator<InferredRelationship> it = inferredRelationships.iterator();
        while (it.hasNext()) {
            InferredRelationship inferred = it.next();

            DbAttribute src = getJoinAttribute(inferred.getSource(), inferred.getTarget());
            if (src == null) {
                // TODO: andrus 03/28/2010 this is pretty inefficient I guess... We should
                // check for this condition earlier. See CAY-1405 for the map that caused
                // this issue
                it.remove();
                continue;
            }

            DbAttribute target = getJoinAttribute(inferred.getTarget(), inferred
                    .getSource());
            if (target == null) {
                // TODO: andrus 03/28/2010 this is pretty inefficient I guess... We should
                // check for this condition earlier. See CAY-1405 for the map that caused
                // this issue
                it.remove();
                continue;
            }

            inferred.setJoinSource(src);
            if (src.isPrimaryKey()) {
                inferred.setToMany(true);
            }

            inferred.setJoinTarget(target);
        }
    }

    protected void createNames() {


        for (InferredRelationship myir : inferredRelationships) {

            DbRelationship localRelationship = new DbRelationship();
            localRelationship.setToMany(myir.isToMany());

            if (myir.getJoinSource().isPrimaryKey()) {

                localRelationship.addJoin(
                        new DbJoin(localRelationship, myir.getJoinSource().getName(), myir.getJoinTarget().getName())
                );
                localRelationship.setSourceEntity(myir.getSource());
                localRelationship.setTargetEntityName(myir.getTarget().getName());
            } else {
                localRelationship.addJoin(
                        new DbJoin(localRelationship, myir.getJoinTarget().getName(), myir.getJoinSource().getName())
                );
                localRelationship.setSourceEntity(myir.getTarget());
                localRelationship.setTargetEntityName(myir.getSource().getName());
            }

            myir.setName(strategy.relationshipName(localRelationship));
        }
    }

    public boolean isSelected(InferredRelationship entity) {
        return selectedEntities.contains(entity);
    }

    public void setSelected(InferredRelationship entity, boolean selectedFlag) {
        if (selectedFlag) {
            if (selectedEntities.add(entity)) {
                firePropertyChange(SELECTED_PROPERTY, null, null);
            }
        } else {
            if (selectedEntities.remove(entity)) {
                firePropertyChange(SELECTED_PROPERTY, null, null);
            }
        }
    }

    public int getSelectedEntitiesSize() {
        return selectedEntities.size();
    }

    public List<InferredRelationship> getEntities() {
        return inferredRelationships;
    }

    public DataMap getDataMap() {
        return dataMap;
    }

    public void setNamingStrategy(ObjectNameGenerator namestr) {
        strategy = namestr;
    }


    public ObjectNameGenerator createNamingStrategy(String strategyClass) {
        try {
            ClassLoadingService classLoader = application.getClassLoadingService();

            return classLoader.loadClass(ObjectNameGenerator.class, strategyClass).getDeclaredConstructor().newInstance();
        } catch (Throwable th) {
            LOGGER.error("Error in " + getClass().getName(), th);

            JOptionPane.showMessageDialog(
                    view,
                    "Naming Strategy Initialization Error: " + th.getMessage(),
                    "Naming Strategy Initialization Error",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    @Override
    public Component getView() {
        return view;
    }

    public void startup() {
        // show dialog even on empty DataMap, as custom generation may still take
        // advantage of it

        view = new InferRelationshipsDialog(entitySelector.getView());
        initBindings();

        view.pack();
        view.setModal(true);
        centerView();
        makeCloseableOnEscape();
        view.setVisible(true);

    }

    protected void initBindings() {
        view.getCancelButton().addActionListener(e -> cancelAction());
        view.getGenerateButton().addActionListener(e -> generateAction());
        addPropertyChangeListener(SELECTED_PROPERTY, evt -> entitySelectedAction());
        view.getStrategyCombo().addActionListener(e -> strategyComboAction());
    }

    public void entitySelectedAction() {
        int size = getSelectedEntitiesSize();
        String label;

        if (size == 0) {
            label = "No DbRelationships selected";
        } else if (size == 1) {
            label = "One DbRelationships selected";
        } else {
            label = size + " DbRelationships selected";
        }

        view.getEntityCount().setText(label);
    }

    public void strategyComboAction() {
        try {

            String strategyClass = (String) view.getStrategyCombo().getSelectedItem();

            this.strategy = createNamingStrategy(strategyClass);

            // Be user-friendly and update preferences with specified strategy
            if (strategy == null) {
                return;
            }
            NameGeneratorPreferences.getInstance().addToLastUsedStrategies(strategyClass);
            view.getStrategyCombo().setModel(
                    new DefaultComboBoxModel<>(NameGeneratorPreferences.getInstance().getLastUsedStrategies()));
        } catch (Throwable th) {
            LOGGER.error("Error in " + getClass().getName(), th);
            return;
        }

        setNamingStrategy(strategy);
        createNames();
        entitySelector.initBindings();
        view.setChoice(InferRelationshipsDialog.SELECT);
    }

    public void cancelAction() {
        view.dispose();
    }

    public void generateAction() {

        ProjectController mediator = application.getFrameController().getProjectController();
        InferRelationshipsUndoableEdit undoableEdit = new InferRelationshipsUndoableEdit();

        for (InferredRelationship temp : selectedEntities) {
            DbRelationship rel = new DbRelationship(uniqueRelName(temp.getSource(), temp
                    .getName()));

            RelationshipEvent e = new RelationshipEvent(Application.getFrame(), rel, temp
                    .getSource(), MapEvent.ADD);
            mediator.fireDbRelationshipEvent(e);

            rel.setSourceEntity(temp.getSource());
            rel.setTargetEntityName(temp.getTarget());
            DbJoin join = new DbJoin(rel, temp.getJoinSource().getName(), temp
                    .getJoinTarget()
                    .getName());
            rel.addJoin(join);
            rel.setToMany(temp.isToMany());
            temp.getSource().addRelationship(rel);

            undoableEdit.addEdit(new CreateRelationshipUndoableEdit(temp.getSource(), new DbRelationship[]{rel}));
        }
        JOptionPane.showMessageDialog(view, getSelectedEntitiesSize() + " relationships generated");
        view.dispose();
    }

    private String uniqueRelName(DbEntity entity, String preferredName) {
        int currentSuffix = 1;
        String relName = preferredName;

        while (entity.getRelationship(relName) != null
                || entity.getAttribute(relName) != null) {
            relName = preferredName + currentSuffix;
            currentSuffix++;
        }
        return relName;
    }

    public String getJoin(InferredRelationship irItem) {
        return irItem.getJoinSource().getName()
                + " : "
                + irItem.getJoinTarget().getName();
    }

    public String getToMany(InferredRelationship irItem) {
        if (irItem.isToMany()) {
            return "to many";
        } else {
            return "to one";
        }
    }

    public boolean updateSelection(Predicate<InferredRelationship> predicate) {
        boolean modified = false;

        for (InferredRelationship entity : inferredRelationships) {
            boolean select = predicate.test(entity);

            if (select) {
                if (selectedEntities.add(entity)) {
                    modified = true;
                }
            } else {
                if (selectedEntities.remove(entity)) {
                    modified = true;
                }
            }
        }

        if (modified) {
            firePropertyChange(SELECTED_PROPERTY, null, null);
        }

        return modified;
    }
}
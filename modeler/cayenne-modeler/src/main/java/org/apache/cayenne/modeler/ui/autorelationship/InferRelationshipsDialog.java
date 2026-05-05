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
package org.apache.cayenne.modeler.ui.autorelationship;

import org.apache.cayenne.dbsync.naming.ObjectNameGenerator;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.modeler.event.model.DbRelationshipEvent;
import org.apache.cayenne.modeler.service.classloader.ModelerClassLoader;
import org.apache.cayenne.modeler.toolkit.ProjectDialog;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.undo.CreateRelationshipUndoableEdit;
import org.apache.cayenne.modeler.undo.InferRelationshipsUndoableEdit;
import org.apache.cayenne.modeler.util.NameGeneratorPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Window;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Modal dialog that scans a {@link DataMap}'s DbEntities for likely missing
 * DbRelationships (FK columns named "{TARGET}_ID") and offers the user a list to confirm.
 */
public class InferRelationshipsDialog extends ProjectDialog {

    private static final Logger LOGGER = LoggerFactory.getLogger(InferRelationshipsDialog.class);
    public static final String SELECTED_PROPERTY = "selected";
    public static final int SELECT = 1;
    public static final int CANCEL = 0;

    private final DataMap dataMap;
    private final List<InferredRelationship> inferredRelationships = new ArrayList<>();
    private final List<DbEntity> entities;
    private final Set<InferredRelationship> selectedEntities = new HashSet<>();
    private final InferRelationshipsTabController entitySelector;

    private final JButton generateButton;
    private final JButton cancelButton;
    private final JLabel entityCount;
    private final JComboBox<String> strategyCombo;

    private ObjectNameGenerator strategy;
    private PropertyChangeSupport propertyChangeSupport;

    @SuppressWarnings("unused")
    private int choice = CANCEL;

    public InferRelationshipsDialog(ProjectSession session, Window owner, DataMap dataMap) {
        super(session, owner, "Infer Relationships", ModalityType.APPLICATION_MODAL);
        this.dataMap = dataMap;
        this.entities = new ArrayList<>(dataMap.getDbEntities());
        this.strategy = createNamingStrategy(NameGeneratorPreferences
                .getInstance()
                .getLastUsedStrategies(app)
                .get(0));
        setRelationships();

        this.generateButton = new JButton("Create DbRelationships");
        this.cancelButton = new JButton("Cancel");
        this.entityCount = new JLabel("No DbRelationships selected");
        this.entityCount.setFont(entityCount.getFont().deriveFont(10f));
        this.strategyCombo = new JComboBox<>();
        this.strategyCombo.setEditable(true);

        this.entitySelector = new InferRelationshipsTabController(this);

        initLayout();
        initBindings();
    }

    private void initLayout() {
        getRootPane().setDefaultButton(generateButton);

        JPanel strategyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        strategyPanel.add(new JLabel("Naming Strategy:  "));
        strategyPanel.add(strategyCombo);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(entityCount);
        buttons.add(Box.createHorizontalStrut(50));
        buttons.add(cancelButton);
        buttons.add(generateButton);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(strategyPanel, BorderLayout.NORTH);
        contentPane.add(entitySelector.getView(), BorderLayout.CENTER);
        contentPane.add(buttons, BorderLayout.SOUTH);

        strategyCombo.setModel(new DefaultComboBoxModel<>(
                NameGeneratorPreferences.getInstance().getLastUsedStrategies(app)));
    }

    private void initBindings() {
        cancelButton.addActionListener(e -> dispose());
        generateButton.addActionListener(e -> generateAction());
        addSelectionChangeListener(evt -> entitySelectedAction());
        strategyCombo.addActionListener(e -> strategyComboAction());
    }

    // ---------- model methods (formerly on the controller) ----------

    public List<InferredRelationship> getEntities() {
        return inferredRelationships;
    }

    public DataMap getDataMap() {
        return dataMap;
    }

    public boolean isSelected(InferredRelationship rel) {
        return selectedEntities.contains(rel);
    }

    public void setSelected(InferredRelationship rel, boolean selectedFlag) {
        if (selectedFlag) {
            if (selectedEntities.add(rel)) firePropertyChange();
        } else {
            if (selectedEntities.remove(rel)) firePropertyChange();
        }
    }

    public int getSelectedEntitiesSize() {
        return selectedEntities.size();
    }

    public boolean updateSelection(Predicate<InferredRelationship> predicate) {
        boolean modified = false;
        for (InferredRelationship rel : inferredRelationships) {
            if (predicate.test(rel)) {
                if (selectedEntities.add(rel)) modified = true;
            } else {
                if (selectedEntities.remove(rel)) modified = true;
            }
        }
        if (modified) firePropertyChange();
        return modified;
    }

    public String getJoin(InferredRelationship rel) {
        return rel.getJoinSource().getName() + " : " + rel.getJoinTarget().getName();
    }

    public String getToMany(InferredRelationship rel) {
        return rel.isToMany() ? "to many" : "to one";
    }

    private void firePropertyChange() {
        if (propertyChangeSupport != null) {
            propertyChangeSupport.firePropertyChange(SELECTED_PROPERTY, null, null);
        }
    }

    private void addSelectionChangeListener(PropertyChangeListener listener) {
        if (propertyChangeSupport == null) {
            propertyChangeSupport = new PropertyChangeSupport(this);
        }
        propertyChangeSupport.addPropertyChangeListener(SELECTED_PROPERTY, listener);
    }

    private void setRelationships() {
        inferredRelationships.clear();
        for (DbEntity entity : entities) {
            createRelationships(entity);
        }
        createJoins();
        createNames();
    }

    private void createRelationships(DbEntity entity) {
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
                    createReverseRelationship(targetEntity, entity);
                }
            }
        }
    }

    private void createReverseRelationship(DbEntity eSource, DbEntity eTarget) {
        InferredRelationship myir = new InferredRelationship();
        for (DbRelationship relationship : eSource.getRelationships()) {
            for (DbJoin join : relationship.getJoins()) {
                if (join.getSource().getEntity().equals(eSource) && join.getTarget().getEntity().equals(eTarget)) {
                    return;
                }
            }
        }
        myir.setSource(eSource);
        myir.setTarget(eTarget);
        inferredRelationships.add(myir);
    }

    private DbAttribute getJoinAttribute(DbEntity sEntity, DbEntity tEntity) {
        if (sEntity.getAttributes().size() == 1) {
            return sEntity.getAttributes().iterator().next();
        }
        for (DbAttribute attr : sEntity.getAttributes()) {
            if (attr.getName().equalsIgnoreCase(tEntity.getName() + "_ID")) {
                return attr;
            }
        }
        for (DbAttribute attr : sEntity.getAttributes()) {
            if (attr.getName().equalsIgnoreCase(sEntity.getName() + "_ID") && !attr.isPrimaryKey()) {
                return attr;
            }
        }
        for (DbAttribute attr : sEntity.getAttributes()) {
            if (attr.isPrimaryKey()) {
                return attr;
            }
        }
        return null;
    }

    private void createJoins() {
        Iterator<InferredRelationship> it = inferredRelationships.iterator();
        while (it.hasNext()) {
            InferredRelationship inferred = it.next();

            DbAttribute src = getJoinAttribute(inferred.getSource(), inferred.getTarget());
            if (src == null) {
                // see CAY-1405 for the map that caused this issue
                it.remove();
                continue;
            }

            DbAttribute target = getJoinAttribute(inferred.getTarget(), inferred.getSource());
            if (target == null) {
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

    private void createNames() {
        for (InferredRelationship myir : inferredRelationships) {
            DbRelationship localRelationship = new DbRelationship();
            localRelationship.setToMany(myir.isToMany());

            if (myir.getJoinSource().isPrimaryKey()) {
                localRelationship.addJoin(new DbJoin(localRelationship,
                        myir.getJoinSource().getName(), myir.getJoinTarget().getName()));
                localRelationship.setSourceEntity(myir.getSource());
                localRelationship.setTargetEntityName(myir.getTarget().getName());
            } else {
                localRelationship.addJoin(new DbJoin(localRelationship,
                        myir.getJoinTarget().getName(), myir.getJoinSource().getName()));
                localRelationship.setSourceEntity(myir.getTarget());
                localRelationship.setTargetEntityName(myir.getSource().getName());
            }

            myir.setName(strategy.relationshipName(localRelationship));
        }
    }

    private ObjectNameGenerator createNamingStrategy(String strategyClass) {
        try {
            ModelerClassLoader classLoader = app.getClassLoader();
            return classLoader.loadClass(ObjectNameGenerator.class, strategyClass)
                    .getDeclaredConstructor().newInstance();
        } catch (Throwable th) {
            LOGGER.error("Error in " + getClass().getName(), th);
            JOptionPane.showMessageDialog(
                    this,
                    "Naming Strategy Initialization Error: " + th.getMessage(),
                    "Naming Strategy Initialization Error",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void entitySelectedAction() {
        int size = getSelectedEntitiesSize();
        String label;
        if (size == 0) {
            label = "No DbRelationships selected";
        } else if (size == 1) {
            label = "One DbRelationships selected";
        } else {
            label = size + " DbRelationships selected";
        }
        entityCount.setText(label);
    }

    private void strategyComboAction() {
        try {
            String strategyClass = (String) strategyCombo.getSelectedItem();
            this.strategy = createNamingStrategy(strategyClass);

            if (strategy == null) {
                return;
            }
            // be user-friendly: update preferences with the chosen strategy
            NameGeneratorPreferences.getInstance().addToLastUsedStrategies(app, strategyClass);
            strategyCombo.setModel(new DefaultComboBoxModel<>(
                    NameGeneratorPreferences.getInstance().getLastUsedStrategies(app)));
        } catch (Throwable th) {
            LOGGER.error("Error in " + getClass().getName(), th);
            return;
        }

        createNames();
        entitySelector.initBindings();
        choice = SELECT;
    }

    private void generateAction() {
        InferRelationshipsUndoableEdit undoableEdit = new InferRelationshipsUndoableEdit();

        for (InferredRelationship temp : selectedEntities) {
            DbRelationship rel = new DbRelationship(uniqueRelName(temp.getSource(), temp.getName()));

            DbRelationshipEvent e = DbRelationshipEvent.ofAdd(
                    app.getFrame(), rel, temp.getSource());
            session.fireDbRelationshipEvent(e);

            rel.setSourceEntity(temp.getSource());
            rel.setTargetEntityName(temp.getTarget());
            DbJoin join = new DbJoin(rel,
                    temp.getJoinSource().getName(),
                    temp.getJoinTarget().getName());
            rel.addJoin(join);
            rel.setToMany(temp.isToMany());
            temp.getSource().addRelationship(rel);

            undoableEdit.addEdit(new CreateRelationshipUndoableEdit(
                    session, temp.getSource(), new DbRelationship[]{rel}));
        }
        JOptionPane.showMessageDialog(this, getSelectedEntitiesSize() + " relationships generated");
        dispose();
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
}

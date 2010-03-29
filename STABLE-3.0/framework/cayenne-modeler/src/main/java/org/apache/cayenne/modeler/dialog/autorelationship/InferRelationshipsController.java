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
package org.apache.cayenne.modeler.dialog.autorelationship;

import java.awt.Component;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.map.naming.NamingStrategy;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ClassLoadingService;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.ErrorDebugDialog;
import org.apache.cayenne.modeler.undo.CreateRelationshipUndoableEdit;
import org.apache.cayenne.modeler.undo.InferRelationshipsUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.modeler.util.NamingStrategyPreferences;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class InferRelationshipsController extends InferRelationshipsControllerBase {

    public static final int SELECT = 1;
    public static final int CANCEL = 0;

    private static Log logObj = LogFactory.getLog(ErrorDebugDialog.class);

    protected InferRelationshipsDialog view;

    protected InferRelationshipsTabController entitySelector;

    protected NamingStrategy strategy;

    public InferRelationshipsController(CayenneController parent, DataMap dataMap) {
        super(parent, dataMap);
        strategy = createNamingStrategy(NamingStrategyPreferences
                .getInstance()
                .getLastUsedStrategies()
                .get(0));
        setNamingStrategy(strategy);
        setRelationships();
        this.entitySelector = new InferRelationshipsTabController(this);
    }

    public NamingStrategy createNamingStrategy(String strategyClass) {
        try {
            ClassLoadingService classLoader = Application
                    .getInstance()
                    .getClassLoadingService();

            return (NamingStrategy) classLoader.loadClass(strategyClass).newInstance();
        }
        catch (Throwable th) {
            logObj.error("Error in " + getClass().getName(), th);

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
        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);

        builder.bindToAction(view.getCancelButton(), "cancelAction()");
        builder.bindToAction(view.getGenerateButton(), "generateAction()");
        builder.bindToAction(this, "entitySelectedAction()", SELECTED_PROPERTY);
        builder.bindToAction(view.getStrategyCombo(), "strategyComboAction()");
    }

    public void entitySelectedAction() {
        int size = getSelectedEntitiesSize();
        String label;

        if (size == 0) {
            label = "No DbRelationships selected";
        }
        else if (size == 1) {
            label = "One DbRelationships selected";
        }
        else {
            label = size + " DbRelationships selected";
        }

        view.getEntityCount().setText(label);
    }

    public void strategyComboAction() {
        try {

            String strategyClass = (String) view.getStrategyCombo().getSelectedItem();

            this.strategy = createNamingStrategy(strategyClass);

            /**
             * Be user-friendly and update preferences with specified strategy
             */
            if (strategy == null) {
                return;
            }
            NamingStrategyPreferences
                    .getInstance()
                    .addToLastUsedStrategies(strategyClass);
            view.getStrategyCombo().setModel(
                    new DefaultComboBoxModel(NamingStrategyPreferences
                            .getInstance()
                            .getLastUsedStrategies()));

        }
        catch (Throwable th) {
            logObj.error("Error in " + getClass().getName(), th);
            return;
        }

        setNamingStrategy(strategy);
        createName();
        entitySelector.initBindings();
        view.setChoice(SELECT);

    }

    public NamingStrategy getNamingStrategy() {
        return strategy;
    }

    public void cancelAction() {
        view.dispose();
    }

    public void generateAction() {
        
        ProjectController mediator = application
                .getFrameController()
                .getProjectController();
        
        InferRelationshipsUndoableEdit undoableEdit = new InferRelationshipsUndoableEdit();
        
        for (InferRelationships temp : selectedEntities) {
            DbRelationship rel = new DbRelationship(uniqueRelName(temp.getSource(), temp
                    .getName()));

            RelationshipEvent e = new RelationshipEvent(Application.getFrame(), rel, temp
                    .getSource(), MapEvent.ADD);
            mediator.fireDbRelationshipEvent(e);

            rel.setSourceEntity(temp.getSource());
            rel.setTargetEntity(temp.getTarget());
            DbJoin join = new DbJoin(rel, temp.getJoinSource().getName(), temp
                    .getJoinTarget()
                    .getName());
            rel.addJoin(join);
            rel.setToMany(temp.isToMany());
            temp.getSource().addRelationship(rel);
            
            undoableEdit.addEdit(new CreateRelationshipUndoableEdit(temp.getSource(), new DbRelationship[] { rel }));
        }
        JOptionPane.showMessageDialog(this.getView(), getSelectedEntitiesSize()
                + " relationships generated");
        view.dispose();
    }

    private String uniqueRelName(Entity entity, String preferredName) {
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
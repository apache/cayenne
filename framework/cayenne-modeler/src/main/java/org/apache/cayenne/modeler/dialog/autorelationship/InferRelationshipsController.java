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
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;

import org.apache.cayenne.access.DbLoader;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.map.naming.NamingStrategy;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.CayenneModelerController;
import org.apache.cayenne.modeler.ClassLoadingService;
import org.apache.cayenne.modeler.ModelerPreferences;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.CreateRelationshipAction;
import org.apache.cayenne.modeler.dialog.ErrorDebugDialog;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.project.NamedObjectFactory;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class InferRelationshipsController extends InferRelationshipsControllerBase {

    private static final String STRATEGIES_PREFERENCE = "recent.preferences";

    private static final Vector<String> PREDEFINED_STRATEGIES = new Vector<String>();
    static {
        PREDEFINED_STRATEGIES.add("org.apache.cayenne.map.naming.BasicNamingStrategy");
        PREDEFINED_STRATEGIES.add("org.apache.cayenne.map.naming.SmartNamingStrategy");
    };

    public static final int SELECT = 1;
    public static final int CANCEL = 0;

    private static Log logObj = LogFactory.getLog(ErrorDebugDialog.class);

    protected InferRelationshipsDialog view;

    protected InferRelationshipsTabController entitySelector;

    protected NamingStrategy strategy;

    public InferRelationshipsController(CayenneController parent, DataMap dataMap) {
        super(parent, dataMap);

        this.entitySelector = new InferRelationshipsTabController(this);
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
            ClassLoadingService classLoader = Application
                    .getInstance()
                    .getClassLoadingService();
            String strategyClass = (String) view.getStrategyCombo().getSelectedItem();

            this.strategy = (NamingStrategy) classLoader
                    .loadClass(strategyClass)
                    .newInstance();
            
            /**
             * Be user-friendly and update preferences with specified strategy
             */
            ModelerPreferences pref = ModelerPreferences.getPreferences();
            Vector arr = pref.getVector(STRATEGIES_PREFERENCE, PREDEFINED_STRATEGIES);

            // move to top
            arr.remove(strategyClass);
            arr.add(0, strategyClass);

            pref.setProperty(STRATEGIES_PREFERENCE, arr);
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
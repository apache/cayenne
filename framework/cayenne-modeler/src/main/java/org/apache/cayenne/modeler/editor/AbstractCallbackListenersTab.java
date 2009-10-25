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
package org.apache.cayenne.modeler.editor;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.apache.cayenne.map.EntityListener;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.EntityListenerEvent;
import org.apache.cayenne.modeler.event.EntityListenerListener;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.modeler.util.CayenneWidgetFactory;

import com.jgoodies.forms.builder.DefaultFormBuilder;


/**
 * Base abstract class for editing callback mapping on listener class
 * Adds entity listener class processing logic
 *
 * @version 1.0 Oct 29, 2007
 */
public abstract class AbstractCallbackListenersTab extends AbstractCallbackMethodsTab {

    /**
     * listener class seiection combo
     */
    protected JComboBox listenerClassCombo;

    /**
     * Constructor
     * @param mediator mediator instance
     */
    protected AbstractCallbackListenersTab(ProjectController mediator) {
        super(mediator);
    }

    /**
     * @return returns entity listeners list
     */
    protected abstract List<EntityListener> getEntityListeners();

    /**
     * @return action for removing entity listeners
     */
    protected abstract CayenneAction getRemoveEntityListenerAction();

    /**
     * @return action for creating entity listeners
     */
    public abstract CayenneAction getCreateEntityListenerAction();

    /**
     * performs GUI components initialization
     */
    protected void init() {
        super.init();

        toolBar.addSeparator();
        toolBar.add(getCreateEntityListenerAction().buildButton());
        toolBar.add(getRemoveEntityListenerAction().buildButton());
    }

    protected abstract EntityListener getEntityListener(String listenerClass);

    private void processEditedListenerClassValue(String newValue) {
        String prevName = mediator.getCurrentListenerClass();
        if (getEntityListener(newValue) == null) {
            EntityListener listener = getEntityListener(prevName);
            if (listener != null) {
                listener.setClassName(newValue);
                mediator.fireEntityListenerEvent(new EntityListenerEvent(
                        this,
                        prevName,
                        newValue,
                        MapEvent.CHANGE));
            }
        }
    }


    /**
     * init listeners
     */
    protected void initController() {
        super.initController();
        addComponentListener(
                new ComponentAdapter() {
                    public void componentShown(ComponentEvent e) {
                        rebuildListenerClassCombo(null);
                        mediator.setCurrentCallbackType((CallbackType)callbackTypeCombo.getSelectedItem());
                        updateCallbackTypeCounters();
                        rebuildTable();
                    }
                }
        );


        listenerClassCombo.addItemListener(
                new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        if (e.getStateChange() == ItemEvent.SELECTED && isVisible()) {
                            //detect editing
                            if (listenerClassCombo.getSelectedIndex() == -1 &&
                                listenerClassCombo.getSelectedItem() != null) {
                                processEditedListenerClassValue((String)listenerClassCombo.getSelectedItem());
                            }
                            else {
                                //just celeection changed
                                mediator.setCurrentListenerClass((String)listenerClassCombo.getSelectedItem());
                                updateCallbackTypeCounters();
                                rebuildTable();
                            }
                        }
                    }
                }
        );

        mediator.addEntityListenerListener(
                new EntityListenerListener() {
                    public void entityListenerAdded(EntityListenerEvent e) {
                        if (isVisible() && getCreateEntityListenerAction() == e.getSource()) {
                            rebuildListenerClassCombo(e.getNewName());
                            rebuildTable();
                        }
                    }

                    public void entityListenerChanged(EntityListenerEvent e) {
                        if (isVisible() && e.getSource() == AbstractCallbackListenersTab.this) {
                            rebuildListenerClassCombo(e.getNewName());
                            rebuildTable();
                        }
                    }

                    public void entityListenerRemoved(EntityListenerEvent e) {
                        if (isVisible() && getRemoveEntityListenerAction() == e.getSource()) {
                            rebuildListenerClassCombo(null);
                            rebuildTable();
                        }
                    }
                }
        );
    }

    /**
     * rebuils listener class selection dropdown content and fires selection event
     *
     * @param selectedListener listener to be selected after rebuild
     */
    protected void rebuildListenerClassCombo(String selectedListener) {
        List entityListeners = getEntityListeners();
        List listenerClasses = new ArrayList();
        if (entityListeners !=  null) {
            for (EntityListener entityListener : getEntityListeners()) {
                listenerClasses.add(entityListener.getClassName());
            }
        }

        listenerClassCombo.setModel(
                new DefaultComboBoxModel(listenerClasses.toArray())
        );

        getCreateCallbackMethodAction().setEnabled(listenerClasses.size() > 0);

        if (selectedListener == null) {
            if (listenerClasses.size() > 0) {
                listenerClassCombo.setSelectedIndex(0);
            }
        }
        else {
            listenerClassCombo.setSelectedItem(selectedListener);
        }

        mediator.setCurrentListenerClass((String)listenerClassCombo.getSelectedItem());

        getRemoveEntityListenerAction().setEnabled(listenerClasses.size() > 0);
        listenerClassCombo.setEnabled(listenerClasses.size() > 0);
    }


    /**
     * adds listener class dropdown to filter bar
     * @param builder filter forms builder
     */
    protected void buildFilter(DefaultFormBuilder builder) {
        listenerClassCombo = CayenneWidgetFactory.createComboBox();
        listenerClassCombo.setEditable(true);
        builder.append(new JLabel("Listener class:"), listenerClassCombo);
        builder.nextLine();
        super.buildFilter(builder);
    }

}

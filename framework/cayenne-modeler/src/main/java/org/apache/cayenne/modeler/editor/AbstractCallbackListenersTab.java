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

import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CayenneWidgetFactory;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.modeler.event.ListenerClassSelectionEvent;
import org.apache.cayenne.modeler.event.EntityListenerEvent;
import org.apache.cayenne.modeler.event.ListenerClassSelectionListener;
import org.apache.cayenne.modeler.event.EntityListenerListener;
import org.apache.cayenne.map.EntityListener;

import javax.swing.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

import com.jgoodies.forms.builder.DefaultFormBuilder;


/**
 * Base abstract class for editing callback mapping on listener class
 * Adds entity listener class processing logic
 *
 * @author Vasil Tarasevich
 * @version 1.0 Oct 29, 2007
 */
public abstract class AbstractCallbackListenersTab extends AbstractCallbackMethodsTab
        implements ListenerClassSelectionListener, EntityListenerListener {

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
    protected abstract List getEntityListeners();

    /**
     * @return action for removing entity listeners
     */
    protected abstract CayenneAction getRemoveEntityListenerAction();

    /**
     * @return action for creating entity listeners
     */
    public abstract CayenneAction getCreateEntityListenerAction();

    /**
     * @return action for changing entity listeners
     */
    public abstract CayenneAction getChangeEntityListenerAction();

    /**
     * performs GUI components initialization
     */
    protected void init() {
        super.init();

        toolBar.addSeparator();
        toolBar.add(getCreateEntityListenerAction().buildButton());
        toolBar.add(getChangeEntityListenerAction().buildButton());
        toolBar.add(getRemoveEntityListenerAction().buildButton());

        listenerClassCombo.addItemListener(
                new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        if (e.getStateChange() == ItemEvent.SELECTED) {
                            mediator.fireListenerClassSelectionEvent(
                                    new ListenerClassSelectionEvent(
                                            AbstractCallbackListenersTab.this,
                                            (String)listenerClassCombo.getSelectedItem()
                                    )
                            );
                        }
                    }
                }
        );
    }

    /**
     * init listeners
     */
    protected void initController() {
        super.initController();
        mediator.addListenerClassSelectionListener(this);
        mediator.addEntityListenerListener(this);
    }

    /**
     * rebuils listener class selection dropdown content and fires selection event
     */
    protected void rebuildListenerClassCombo() {
        List entityListeners = getEntityListeners();
        List listenerClasses = new ArrayList();
        if (entityListeners !=  null) {
            for (Iterator i = getEntityListeners().iterator(); i.hasNext();) {
                EntityListener entityListener = (EntityListener)i.next();
                listenerClasses.add(entityListener.getClassName());
            }
        }

        listenerClassCombo.setModel(
                new DefaultComboBoxModel(listenerClasses.toArray())
        );

        mediator.fireListenerClassSelectionEvent(
                new ListenerClassSelectionEvent(
                        this,
                        listenerClassCombo.getItemCount() > 0 ?
                        (String)listenerClassCombo.getItemAt(0) : null)
        );
    }


    /**
     * adds listener class dropdown to filter bar
     * @param builder filter forms builder
     */
    protected void buildFilter(DefaultFormBuilder builder) {
        listenerClassCombo = CayenneWidgetFactory.createComboBox();
        builder.append(new JLabel("Listener class:"), listenerClassCombo);
        builder.nextLine();
        super.buildFilter(builder);
    }

    /**
     * processes adding of new entity listener
     * @param e event
     */
    public void entityListenerAdded(EntityListenerEvent e) {
        rebuildListenerClassCombo();
        listenerClassCombo.setSelectedItem(e.getNewName());
        mediator.fireListenerClassSelectionEvent(
                new ListenerClassSelectionEvent(
                        this,
                        e.getNewName()
                )
        );
    }

    /**
     * processes renaming of an entity listener
     * @param e event
     */
    public void entityListenerChanged(EntityListenerEvent e) {
        rebuildListenerClassCombo();
    }

    /**
     * processes removing of an entity listener
     * @param e event
     */
    public void entityListenerRemoved(EntityListenerEvent e) {
        rebuildListenerClassCombo();
    }

    /**
     * processes listener class selection
     * @param e event
     */
    public void listenerClassSelected(ListenerClassSelectionEvent e) {
        if (e.getSource() == this) {
            rebuildTable();
            getChangeEntityListenerAction().setEnabled(e.getListenerClass() != null);
        }
    }
}


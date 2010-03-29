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

import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.RemoveAttributeAction;
import org.apache.cayenne.modeler.action.RemoveCallbackMethodAction;
import org.apache.cayenne.modeler.event.EmbeddableAttributeDisplayEvent;
import org.apache.cayenne.modeler.event.EmbeddableAttributeDisplayListener;
import org.apache.cayenne.modeler.event.EmbeddableDisplayEvent;
import org.apache.cayenne.modeler.event.EmbeddableDisplayListener;

public class EmbeddableTabbedView extends JTabbedPane implements
        EmbeddableAttributeDisplayListener, EmbeddableDisplayListener {

    protected ProjectController mediator;

    protected JScrollPane embeddablePanel;
    protected EmbeddableAttributeTab attributesPanel;

    protected JPanel listenersPanel;

    public EmbeddableTabbedView(ProjectController mediator) {
        this.mediator = mediator;

        initView();
        initController();
    }

    private void initView() {
        setTabPlacement(JTabbedPane.TOP);

        embeddablePanel = new JScrollPane(new EmbeddableTab(mediator));
        addTab("Embeddable", embeddablePanel);

        attributesPanel = new EmbeddableAttributeTab(mediator);
        addTab("Attributes", attributesPanel);
    }

    private void initController() {

        mediator.addEmbeddableAttributeDisplayListener(this);
        mediator.addEmbeddableDisplayListener(this);

        addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                resetRemoveButtons();
                Component selected = getSelectedComponent();
                while (selected instanceof JScrollPane) {
                    selected = ((JScrollPane) selected).getViewport().getView();
                }

                if (selected instanceof ExistingSelectionProcessor) {
                    ((ExistingSelectionProcessor) selected).processExistingSelection(e);
                }
            }
        });
    }

    /** Reset the remove buttons */
    private void resetRemoveButtons() {
        Application app = Application.getInstance();
        app.getAction(RemoveAttributeAction.getActionName()).setEnabled(false);
        app.getAction(RemoveCallbackMethodAction.getActionName()).setEnabled(false);
    }

    public void currentEmbeddableChanged(EmbeddableDisplayEvent e) {
        Embeddable emb = e.getEmbeddable();
        if (e.isMainTabFocus() && emb instanceof Embeddable) {
            
            if (getSelectedComponent() != embeddablePanel) {
                setSelectedComponent(embeddablePanel);
                embeddablePanel.setVisible(true);
            }
        }

        resetRemoveButtons();
        setVisible(e.getEmbeddable() != null);
    }

    public void currentEmbeddableAttributeChanged(EmbeddableAttributeDisplayEvent e) {
        if (e.getEmbeddable() == null)
            return;

        EmbeddableAttribute[] attrs = e.getEmbeddableAttributes();
        EmbeddableAttribute[] embAttrs = new EmbeddableAttribute[attrs.length];

        System.arraycopy(attrs, 0, embAttrs, 0, attrs.length);

        if (getSelectedComponent() != attributesPanel && embAttrs.length > 0) {
            setSelectedComponent(attributesPanel);
            attributesPanel.setVisible(true);
        }

        attributesPanel.selectAttributes(embAttrs);
    }
}

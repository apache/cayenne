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
package org.apache.cayenne.modeler.ui.project.editor.embeddable;

import org.apache.cayenne.modeler.ui.project.editor.embeddable.attributes.EmbeddableAttributesView;
import org.apache.cayenne.modeler.ui.project.editor.embeddable.main.EmbeddableMainView;
import org.apache.cayenne.modeler.ui.project.editor.query.ExistingSelectionProcessor;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.RemoveAttributeAction;
import org.apache.cayenne.modeler.action.RemoveCallbackMethodAction;
import org.apache.cayenne.modeler.event.display.EmbeddableAttributeDisplayEvent;
import org.apache.cayenne.modeler.event.display.EmbeddableDisplayEvent;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;

public class EmbeddableView extends JTabbedPane {

    private final JScrollPane embeddablePanel;
    private final EmbeddableAttributesView attributesPanel;

    public EmbeddableView(ProjectController controller) {

        setTabPlacement(JTabbedPane.TOP);

        embeddablePanel = new JScrollPane(new EmbeddableMainView(controller));
        addTab("Embeddable", embeddablePanel);

        attributesPanel = new EmbeddableAttributesView(controller);
        addTab("Attributes", attributesPanel);

        controller.addEmbeddableAttributeDisplayListener(this::currentEmbeddableAttributeChanged);
        controller.addEmbeddableDisplayListener(this::currentEmbeddableChanged);
        addChangeListener(this::stateChanged);
    }

    private void stateChanged(ChangeEvent e) {
        resetRemoveButtons();
        Component selected = getSelectedComponent();
        while (selected instanceof JScrollPane) {
            selected = ((JScrollPane) selected).getViewport().getView();
        }

        if (selected instanceof ExistingSelectionProcessor) {
            ((ExistingSelectionProcessor) selected).processExistingSelection(e);
        }
    }

    private void resetRemoveButtons() {
        ActionManager actionManager = Application.getInstance().getActionManager();

        actionManager.getAction(RemoveAttributeAction.class).setEnabled(false);
        actionManager.getAction(RemoveCallbackMethodAction.class).setEnabled(false);
    }

    private void currentEmbeddableChanged(EmbeddableDisplayEvent e) {
        Embeddable emb = e.getEmbeddable();
        if (e.isMainTabFocus() && emb != null) {

            if (getSelectedComponent() != embeddablePanel) {
                setSelectedComponent(embeddablePanel);
                embeddablePanel.setVisible(true);
            }
        }

        resetRemoveButtons();
        setVisible(e.getEmbeddable() != null);
    }

    private void currentEmbeddableAttributeChanged(EmbeddableAttributeDisplayEvent e) {
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

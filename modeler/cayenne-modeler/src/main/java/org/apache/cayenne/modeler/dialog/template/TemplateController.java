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
package org.apache.cayenne.modeler.dialog.template;

import org.apache.cayenne.map.template.ClassGenerationDescriptor;
import org.apache.cayenne.map.template.ClassTemplate;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.naming.DefaultUniqueNameGenerator;
import org.apache.cayenne.map.naming.NameCheckers;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.CreateTemplateAction;
import org.apache.cayenne.modeler.action.RemoveAction;
import org.apache.cayenne.modeler.undo.CreateTemplateUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.swing.BindingBuilder;

import javax.swing.*;
import java.awt.*;

/**
 * @since 4.0
 */
public class TemplateController extends CayenneController {
    protected ProjectController projectController;
    protected TemplateView view;
    protected boolean canceled;

    public TemplateController(ProjectController controller,
                              TemplateView view) {
        super(controller);
        this.projectController = controller;
        this.view = view;

        initBindings();
    }

    protected void initBindings() {
        BindingBuilder builder = new BindingBuilder(getApplication().getBindingFactory(), this);

        builder.bindToAction(view.getAddTemplateButton(), "addTemplateAction()");
        builder.bindToAction(view.getDeleteTemplateButton(), "deleteTemplateAction()");
    }

    public void addTemplateAction() {
        ActionManager actionManager = Application.getInstance().getActionManager();
        CreateTemplateAction action = actionManager
                .getAction(CreateTemplateAction.class);

        DataChannelDescriptor dataChannelDescriptor = projectController.getCurrentDataChanel();
        String templateName = DefaultUniqueNameGenerator.generate(NameCheckers.template, dataChannelDescriptor);

        ClassTemplate template = new ClassTemplate(templateName);
        DataMap dataMap = projectController.getCurrentDataMap();
        template.setDataMap(dataMap);

        action.createTemplate(dataMap, template);
        application.getUndoManager().addEdit(new CreateTemplateUndoableEdit(dataMap, template));
    }

    public void deleteTemplateAction() {
        DataMap dataMap = projectController.getCurrentDataMap();
        JTable templateList =  view.getTemplateListEditor().getView().getTemplateList();
        String templateName = (String) templateList.
                getValueAt(templateList.getSelectedRow(), 0);
        if (templateName != null) {
            DataMap currentDataMap = projectController.getCurrentDataMap();
            if (currentDataMap != null) {
                ClassGenerationDescriptor classGenerationDescriptor = currentDataMap.getClassGenerationDescriptor();
                    if (classGenerationDescriptor.getTemplates() != null) {
                        ClassTemplate template = classGenerationDescriptor.getTemplates().get(templateName);

                        ActionManager actionManager = Application.getInstance().getActionManager();
                        RemoveAction action = actionManager.getAction(RemoveAction.class);
                        action.removeTemplate(currentDataMap, template);
                    }
            }
        }
    }

    public Component getView() {
        return view;
    }

}

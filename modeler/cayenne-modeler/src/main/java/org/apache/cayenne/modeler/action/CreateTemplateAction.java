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
package org.apache.cayenne.modeler.action;

import org.apache.cayenne.map.template.ClassGenerationDescriptor;
import org.apache.cayenne.map.template.ClassTemplate;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.event.TemplateEvent;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.map.naming.DefaultUniqueNameGenerator;
import org.apache.cayenne.map.naming.NameCheckers;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CayenneAction;

import java.awt.event.ActionEvent;

/**
 * @since 4.0
 */
public class CreateTemplateAction extends CayenneAction{
    public static String getActionName() {
        return "Create Template";
    }

    /**
     * Constructor for CreateTemplateAction
     */
    public CreateTemplateAction(Application application) {
        super(getActionName(), application);
    }

    @Override
    public void performAction(ActionEvent e) {
        ProjectController mediator = getProjectController();

        DataChannelDescriptor dataChannelDescriptor = mediator.getCurrentDataChanel();
        String templateName = DefaultUniqueNameGenerator.generate(NameCheckers.template, dataChannelDescriptor);

        ClassTemplate template = new ClassTemplate(templateName);
        DataMap dataMap = mediator.getCurrentDataMap();

        createTemplate(dataMap, template);
    }

    public void createTemplate(DataMap dataMap, ClassTemplate template) {
        ProjectController mediator = getProjectController();
        if (dataMap != null) {
            ClassGenerationDescriptor classGenerationDescriptor = dataMap.getClassGenerationDescriptor();
            if (classGenerationDescriptor.getTemplates() != null) {
                template.setDataMap(dataMap);
                classGenerationDescriptor.getTemplates().put(template.getName(), template);
                fireTemplateEvent(this, mediator, dataMap, template);
            }
        }
    }

    /**
     * Fires events when a template was added
     */
    static void fireTemplateEvent(
            Object src,
            ProjectController mediator,
            DataMap dataMap,
            ClassTemplate template) {
            mediator.fireTemplateEvent(new TemplateEvent(src, template, MapEvent.ADD));
    }
}

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
package org.apache.cayenne.modeler.action;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.project.validation.Inspection;
import org.apache.cayenne.project.validation.ValidationConfig;

import java.awt.event.ActionEvent;
import java.util.EnumSet;

/**
 * @since 5.0
 */
public class DisableValidationInspectionAction extends UpdateValidationConfigAction {

    public static final String ACTION_NAME = "Disable inspection";

    private static final String INSPECTION_PARAM = "inspection";

    private final ValidateAction validationAction;

    public DisableValidationInspectionAction(Application application) {
        super(ACTION_NAME, application);
        validationAction = new ValidateAction(application);

        setUndoable(false);
    }

    @Override
    public void performAction(ActionEvent e) {
        Inspection inspection = (Inspection) getValue(INSPECTION_PARAM);
        DataChannelDescriptor dataChannel = (DataChannelDescriptor) application.getProject().getRootNode();
        ValidationConfig config = ValidationConfig.fromMetadata(application.getMetaData(), dataChannel);

        EnumSet<Inspection> enabledInspections = EnumSet.copyOf(config.getEnabledInspections());
        enabledInspections.remove(inspection);
        putConfig(new ValidationConfig(enabledInspections));
        super.performAction(e);

        validationAction.performAction(e);
    }

    public DisableValidationInspectionAction putInspection(Inspection inspection) {
        putValue(INSPECTION_PARAM, inspection);
        return this;
    }

    @Override
    public String getIconName() {
        return "icon-disable.png";
    }
}

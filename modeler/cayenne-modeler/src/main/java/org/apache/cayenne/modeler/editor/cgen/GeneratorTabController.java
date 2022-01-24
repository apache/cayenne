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

package org.apache.cayenne.modeler.editor.cgen;

import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.pref.PreferenceDetail;

/**
 * @since 4.1
 */
public class GeneratorTabController extends CayenneController {

    public static final String GENERATOR_PROPERTY = "generator";

    protected GeneratorTabPanel view;
    protected PreferenceDetail preferences;
    private final StandardModeController standardModeController;
    private final ClientModeController clientModeController;
    private final CustomModeController customModeController;

    public GeneratorTabController(CodeGeneratorController parent) {
        super(parent);
        this.standardModeController = new StandardModeController(parent);
        this.clientModeController = new ClientModeController(parent);
        this.customModeController = new CustomModeController(parent);
        this.view = new GeneratorTabPanel(standardModeController.view, clientModeController.view, customModeController.view);
        initBindings();
    }

    public GeneratorTabPanel getView() {
        return view;
    }

    protected CodeGeneratorController getParentController() {
        return (CodeGeneratorController) getParent();
    }

    public PreferenceDetail getPreferences() {
        return preferences;
    }

    protected void initBindings() {
        view.getStandardModeRButton().addActionListener(e -> configureController(standardModeController));
        view.getClientModeRButton().addActionListener(e -> configureController(clientModeController));
        view.getCustomTemplateModeRButton().addActionListener(e -> configureController(customModeController));
    }

    void configureController(GeneratorController<? extends StandardModePanel> controller) {
        CgenConfiguration cgenConfiguration = getParentController().getCgenConfiguration();
        controller.updateConfiguration(cgenConfiguration);
        controller.initForm(cgenConfiguration);
        getParentController().getPrevGeneratorController().put(cgenConfiguration.getDataMap(), controller);
    }


    GeneratorController<StandardModePanel> getStandardController() {
        return standardModeController;
    }

    GeneratorController<StandardModePanel> getClientModeController() {
        return clientModeController;
    }

    GeneratorController<CustomModePanel> getCustomModeController() {
        return customModeController;
    }


}
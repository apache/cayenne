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

import java.awt.Component;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 4.1
 */
public class GeneratorTabController extends CayenneController {

    private static final String STANDARD_OBJECTS_MODE = "Standard Persistent Objects";
    private static final String CLIENT_OBJECTS_MODE = "Client Persistent Objects";
    private static final String ADVANCED_MODE = "Advanced";

    public static final String GENERATOR_PROPERTY = "generator";

    private static final String[] GENERATION_MODES = new String[] {
            STANDARD_OBJECTS_MODE, CLIENT_OBJECTS_MODE, ADVANCED_MODE
    };

    protected GeneratorTabPanel view;
    protected Map<String, GeneratorController> controllers;
    protected PreferenceDetail preferences;

    public GeneratorTabController(CodeGeneratorController parent) {
        super(parent);
        this.controllers = new HashMap<>(3);
        controllers.put(STANDARD_OBJECTS_MODE, new StandardModeController(parent));
        controllers.put(CLIENT_OBJECTS_MODE, new ClientModeController(parent));
        controllers.put(ADVANCED_MODE, new CustomModeController(parent));
        Component[] modePanels = new Component[GENERATION_MODES.length];
        for (int i = 0; i < GENERATION_MODES.length; i++) {
            modePanels[i] = controllers.get(GENERATION_MODES[i]).getView();
        }
        this.view = new GeneratorTabPanel(GENERATION_MODES, modePanels);
        initBindings();
        view.setPreferredSize(new Dimension(550, 480));
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
        view.getGenerationMode().addActionListener(action -> {
            String name = (String)view.getGenerationMode().getSelectedItem();
            GeneratorController modeController = getGeneratorController();
            CgenConfiguration cgenConfiguration = getParentController().getCgenConfiguration();
            modeController.updateConfiguration(cgenConfiguration);
            controllers.get(name).initForm(cgenConfiguration);
            getParentController().getPrevGeneratorController().put(cgenConfiguration.getDataMap(), modeController);
        });
    }

    public void setSelectedController(GeneratorController generatorController) {
        for(String key : controllers.keySet()) {
            if(generatorController.equals(controllers.get(key))) {
                getView().getGenerationMode().setSelectedItem(key);
            }
        }
    }

    GeneratorController getGeneratorController() {
        String name = (String)view.getGenerationMode().getSelectedItem();
        return controllers.get(name);
    }

    GeneratorController getStandartController() {
        return controllers.get(STANDARD_OBJECTS_MODE);
    }

    GeneratorController getCustomModeController() {
        return controllers.get(ADVANCED_MODE);
    }

    GeneratorController getClientGeneratorController() {
        return controllers.get(CLIENT_OBJECTS_MODE);
    }
}
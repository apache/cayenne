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

package org.apache.cayenne.modeler.dialog.codegen;

import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.pref.DataMapDefaults;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;

public class StandardModeController extends GeneratorController {

    protected StandardModePanel view;
    protected DataMapDefaults preferences;

    public StandardModeController(CodeGeneratorControllerBase parent) {
        super(parent);
    }

    protected void createDefaults() {
        TreeMap<DataMap, DataMapDefaults> treeMap = new TreeMap<DataMap, DataMapDefaults>();
        ArrayList<DataMap> dataMaps = (ArrayList<DataMap>) getParentController().getDataMaps();

        for (DataMap dataMap : dataMaps) {
            DataMapDefaults preferences = getApplication()
                    .getFrameController()
                    .getProjectController()
                    .getDataMapPreferences(dataMap);

            preferences.setSuperclassPackage("");
            preferences.updateSuperclassPackage(dataMap, false);

            treeMap.put(dataMap, preferences);
            if (getOutputPath() == null) {
                setOutputPath(preferences.getOutputPath());
            }
        }

        setMapPreferences(treeMap);
    }

    protected GeneratorControllerPanel createView() {
        this.view = new StandardModePanel();
        return view;
    }

    public Component getView() {
        return view;
    }

    @Override
    protected ClassGenerationAction newGenerator() {
        ClassGenerationAction action = new ClassGenerationAction();
        getApplication().getInjector().injectMembers(action);
        return action;
    }

    @Override
    public Collection<ClassGenerationAction> createGenerator() {
        return super.createGenerator();
    }
}

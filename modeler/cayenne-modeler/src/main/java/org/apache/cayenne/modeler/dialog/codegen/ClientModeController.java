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
import org.apache.cayenne.gen.ClientClassGenerationAction;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.pref.DataMapDefaults;
import org.apache.cayenne.validation.BeanValidationFailure;
import org.apache.cayenne.validation.ValidationResult;

import java.util.ArrayList;
import java.util.TreeMap;

public class ClientModeController extends StandardModeController {

    public ClientModeController(CodeGeneratorControllerBase parent) {
        super(parent);
    }

    public void validateEntity(ValidationResult validationBuffer, ObjEntity entity) {
        if (!entity.isClientAllowed()) {
            validationBuffer.addFailure(new BeanValidationFailure(
                    entity.getName(),
                    "clientAllowed",
                    "Not a client entity"));
        } else {
            super.validateEntity(validationBuffer, entity, true);
        }
    }

    protected void createDefaults() {
        TreeMap<DataMap, DataMapDefaults> map = new TreeMap<DataMap, DataMapDefaults>();
        ArrayList<DataMap> dataMaps = (ArrayList<DataMap>) getParentController().getDataMaps();
        for (DataMap dataMap : dataMaps) {
            DataMapDefaults preferences = getApplication()
                    .getFrameController()
                    .getProjectController()
                    .getDataMapPreferences(this.getClass().getName().replace(".", "/"), dataMap);

            preferences.setSuperclassPackage("");
            preferences.updateSuperclassPackage(dataMap, true);

            map.put(dataMap, preferences);

            if (getOutputPath() == null) {
                setOutputPath(preferences.getOutputPath());
            }
        }

        setMapPreferences(map);
    }

    protected GeneratorControllerPanel createView() {
        this.view = new StandardModePanel();
        return view;
    }

    @Override
    protected ClassGenerationAction newGenerator() {
        return new ClientClassGenerationAction();
    }
}

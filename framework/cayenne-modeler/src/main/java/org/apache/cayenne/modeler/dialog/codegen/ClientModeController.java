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
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.pref.DataMapDefaults;
import org.apache.cayenne.validation.BeanValidationFailure;
import org.apache.cayenne.validation.ValidationResult;
import org.apache.commons.collections.Predicate;

public class ClientModeController extends StandardModeController {

    protected Predicate checkPredicate;

    public ClientModeController(CodeGeneratorControllerBase parent) {
        super(parent);

        this.checkPredicate = new Predicate() {

            public boolean evaluate(Object object) {
                if (object instanceof ObjEntity) {
                    ObjEntity entity = (ObjEntity) object;
                    return entity.isClientAllowed()
                            && getParentController().getProblem(entity.getName()) == null;
                }

                return false;
            }
        };
    }

    public void validateEntity(ValidationResult validationBuffer, ObjEntity entity) {
        if (!entity.isClientAllowed()) {
            validationBuffer.addFailure(new BeanValidationFailure(
                    entity.getName(),
                    "clientAllowed",
                    "Not a client entity"));
        }
        else {
            super.validateEntity(validationBuffer, entity, true);
        }
    }

    protected DataMapDefaults createDefaults() {
        DataMapDefaults prefs = getApplication()
                .getFrameController()
                .getProjectController()
                .getDataMapPreferences("__client");

        prefs.updateSuperclassPackage(getParentController().getDataMap(), true);
        this.preferences = prefs;
        return prefs;
    }

    @Override
    protected ClassGenerationAction newGenerator() {
        return new ClientClassGenerationAction();
    }

    public Predicate getDefaultEntityFilter() {
        return checkPredicate;
    }
}
